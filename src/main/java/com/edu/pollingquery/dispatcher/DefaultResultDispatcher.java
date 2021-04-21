package com.edu.pollingquery.dispatcher;

import com.edu.pollingquery.context.BoundRelationContextHolder;
import com.edu.pollingquery.context.BoundResultRequestContextHolder;
import com.edu.pollingquery.model.ResultWrapper;
import com.edu.pollingquery.processor.chain.AbstractProcessorChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 结果派发者默认实现
 *
 * @author jcb
 * @since 2021/1/27
 */
@Slf4j
@Component
public class DefaultResultDispatcher<T> extends AbstractResultDispatcher<T>{

    /**
     * Lock used for processResult operations
     */
    private final ReentrantLock lock;

    private BoundRelationContextHolder<T> boundRelationContext;

    /**
     * 请求编号与请求结果处理线程池映射 一个ACTION对应多个VIEW
     */
    private static final Map<String, AsyncTaskExecutor> ACTION_RESULT_HANDLER_MAP = new ConcurrentHashMap<>();

    /**
     * 视图结果派发处理与线程池映射
     */
    private static final Map<String, AsyncTaskExecutor> VIEW_SEND_RESULT_MAP = new ConcurrentHashMap<>();

    private AbstractProcessorChain<T> processorChain;

    public DefaultResultDispatcher(AbstractProcessorChain<T> processorChain, BoundRelationContextHolder<T> boundRelationContext) {
        this.processorChain = processorChain;
        this.boundRelationContext = boundRelationContext;
        lock = new ReentrantLock();
    }

    /**
     * 接收处理结果执行派发处理
     * @param resultWrapper 结果
     */
    @Override
    public void acceptResult(ResultWrapper<T> resultWrapper){
        if(resultWrapper.getActionCode() == null){
            return;
        }
        AsyncTaskExecutor executor = doGetBindResultHandlerThreadPool(resultWrapper.getActionCode());

        executor.execute(() -> {
            if(log.isTraceEnabled()){
                log.trace("分配处理结果开始: actionCode={}, startTime={}"
                        , resultWrapper.getActionCode(), resultWrapper.getStartTime());
            }
            int num = processAndDispatchReturnResult(resultWrapper);
            if(log.isTraceEnabled()) {
                log.trace("分配处理结果结束: actionCode={}, startTime={}, 处理条数：{}"
                        , resultWrapper.getActionCode(), resultWrapper.getStartTime(), num);
            }
        });
    }

    /**
     * 获取执行动作结果派发处理线程池
     * @param actionCode 动作编号
     * @return 线程池
     */
    public AsyncTaskExecutor doGetBindResultHandlerThreadPool(String actionCode){
        AsyncTaskExecutor executor = ACTION_RESULT_HANDLER_MAP.get(actionCode);
        if(executor != null){
            return executor;
        }else {
            ACTION_RESULT_HANDLER_MAP.putIfAbsent(actionCode, createResultHandlerThreadPool(actionCode));
            return ACTION_RESULT_HANDLER_MAP.get(actionCode);
        }
    }

    /**
     * 创建结果加工线程池执行器
     * @param actionCode 动作代号
     * @return 执行器
     */
    private AsyncTaskExecutor createResultHandlerThreadPool(String actionCode){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //线程名称前缀
        executor.setThreadNamePrefix(actionCode + "-dispatch-executor-");
        //线程创建工厂 匿名内部类实现
        executor.setThreadFactory(new CustomizableThreadFactory(executor.getThreadNamePrefix()) {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread t = createThread(runnable);
                //设置子线程异常日志记录
                t.setUncaughtExceptionHandler((thread, e) -> log.error(thread + " throws exception: " + e, e));
                return t;
            }
        });
        //定义核心线程数
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(2);
        executor.setKeepAliveSeconds(0);
        //设置拒绝处理策略为DiscardOldestPolicy
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public int processAndDispatchReturnResult(ResultWrapper<T> resultWrapper) {
        //查询结果编号
        String queryActionCode = resultWrapper.getActionCode();
        //查询结果数据集
        T handlerResult = resultWrapper.getResult();
        if(handlerResult == null){
            log.error("查询结果返回为空!");
        }
        Set<String> relationCodeSet = boundRelationContext.getViewsByActionCode(queryActionCode);
        if(CollectionUtils.isEmpty(relationCodeSet)){
            log.error("未配置对应关系集合，查询结果被忽略!");
            return 0;
        }
        Date startTime = resultWrapper.getStartTime();
        if(startTime == null){
            log.error("请求时间为null，忽略该结果!");
            return 0;
        }
        //声明返回查询条数
        int num;
        //进行结果处理
        try{
            if(log.isTraceEnabled()) {
                log.trace("根据查询事件动作关系分派结果：startTime={}, actionCode={},relationViewCodeSet={}",
                        startTime, queryActionCode, relationCodeSet);
            }
            num = processAndDispatchResult(startTime, relationCodeSet, handlerResult);
        } finally {
            //清理上下文中的线程变量
            processorChain.clearContext();
        }

        return num;
    }

    /**
     * 获取执行动作结果派发处理线程池
     * @param viewCode 视图编号
     * @return 线程池
     */
    public AsyncTaskExecutor doGetBindSendResultThreadPool(String viewCode){
        AsyncTaskExecutor executor = VIEW_SEND_RESULT_MAP.get(viewCode);
        if(executor != null){
            return executor;
        }else {
            VIEW_SEND_RESULT_MAP.putIfAbsent(viewCode, createSendResultThreadPool(viewCode));
            return VIEW_SEND_RESULT_MAP.get(viewCode);
        }
    }

    /**
     * 创建分发结果线程池执行器
     * @param viewCode 视图编号
     * @return 执行器
     */
    private AsyncTaskExecutor createSendResultThreadPool(String viewCode){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //线程名称前缀
        executor.setThreadNamePrefix(viewCode + "-send-result-executor-");
        //线程创建工厂 匿名内部类实现
        executor.setThreadFactory(new CustomizableThreadFactory(executor.getThreadNamePrefix()) {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread t = createThread(runnable);
                //设置子线程异常日志记录
                t.setUncaughtExceptionHandler((thread, e) -> log.error(thread + " throws exception: " + e, e));
                return t;
            }
        });
        //定义核心线程数
        int machineProcessors = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(machineProcessors);
        executor.setQueueCapacity(20);
        executor.setKeepAliveSeconds(30);
        //设置拒绝处理策略为CallerRunsPolicy
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 处理结果并分发
     * @param startTime 请求开始时间
     * @param relationCodeSet 关系集合
     * @param handlerResult 待加工结果
     * @return 处理结果数量
     */
    @SuppressWarnings("rawtypes")
    private int processAndDispatchResult(Date startTime, Set<String> relationCodeSet, T handlerResult){
        int num = 0;
        Map<String, Queue<BoundResultRequestContextHolder<T>>> waitForProcessResultMap = new HashMap<>();
        //----------------------------------------------提取可以返回的结果-START------------------------------------------
        //循环处理结果可以返回的多个视图绑定请求集合
        for(String code : relationCodeSet){
            Queue<BoundResultRequestContextHolder<T>> boundResultQueue = boundRelationContext.getViewBoundResultQueueByViewCode(code);
            if(CollectionUtils.isEmpty(boundResultQueue)){
                continue;
            }
            int size = boundResultQueue.size();
            if(log.isTraceEnabled()) {
                log.trace("轮询查询请求对应viewCode：{} 的绑定结果：{}条待处理。", code, size);
            }
            //处理次数, 可以处理的绑定结果, 处理出错条数
            int processTimes = 0, boundCurrentResultCnt = 0, processErrCnt = 0;
            //存放[取出的可以分配的待处理请求]队列
            Queue<BoundResultRequestContextHolder<T>> waitForProcessResultQueue = new LinkedList<>();
            waitForProcessResultMap.put(code, waitForProcessResultQueue);
            //循环处理取出可以分配的请求绑定
            while(!boundResultQueue.isEmpty() && processTimes <= size) {
                final ReentrantLock lock = this.lock;
                lock.lock();
                try {
                    BoundResultRequestContextHolder boundResultRequestHolder = boundResultQueue.peek();
                    if (boundResultRequestHolder == null) {
                        if(!boundResultQueue.isEmpty()) {
                            //获取的结果如果为null直接退队
                            boundResultQueue.poll();
                        }
                        lock.unlock();
                        log.warn("绑定请求结果为null，请检查处理逻辑!");
                        continue;
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Q轮询查询请求绑定结果处理开始，请求开始时间：{},查询动作开始时间：{}",
                                boundResultRequestHolder.getReqStartTime().getTime(), startTime.getTime());
                    }
                    if (boundResultRequestHolder.getReqStartTime() == null) {
                        BoundResultRequestContextHolder result = boundResultQueue.poll();
                        lock.unlock();
                        if (result != null) {
                            result.setErrorResult(new Exception("无法获取到请求时间！"));
                            //增加错误的处理计数
                            ++processErrCnt;
                            //增加总处理计数,可以处理的绑定结果计数
                            ++num; ++boundCurrentResultCnt;
                        }
                    } else if (!boundResultRequestHolder.getReqStartTime().after(startTime)) {
                        //正常获取到结果返回
                        BoundResultRequestContextHolder<T> result = boundResultQueue.poll();
                        lock.unlock();
                        waitForProcessResultQueue.offer(result);
                        //增加总处理计数,可以处理的绑定结果计数
                        ++num; ++boundCurrentResultCnt;
                    } else if (boundResultRequestHolder.getReqStartTime().after(startTime)) {
                        //获取的请求是当前查询结果开始时间后入队的，不处理，由于是有序队列后面的无需再判断
                        break;
                    } else {
                        log.error("发现无法正确处理的请求！");
                        break;
                    }
                } finally {
                    //增加处理次数
                    ++processTimes;
                    if(lock.isLocked()) {
                        lock.unlock();
                    }
                }
            }
            if(log.isTraceEnabled()) {
                log.trace("发现轮询查询请求对应viewCode：{} 的绑定结果：{}条。", code, boundCurrentResultCnt);
                if(processErrCnt > 0) {
                    log.trace("处理轮询查询无法获取到请求时间对应viewCode：{} 的绑定结果：{}条。", code, processErrCnt);
                }
            }
        }
        //----------------------------------------------提取可以返回的结果-END--------------------------------------------
        //循环加工请求结果并返回
        for(Map.Entry<String, Queue<BoundResultRequestContextHolder<T>>> entry : waitForProcessResultMap.entrySet()) {
            int processCnt = 0;
            for (BoundResultRequestContextHolder<T> boundResultRequestHolder : entry.getValue()) {
                //----------------------加工结果-START-----------------------
                //处理待返回结果
                T handledResult = processorChain.doProcess(boundResultRequestHolder, handlerResult);
                //----------------------加工结果-END-------------------------
                if (boundResultRequestHolder != null && !boundResultRequestHolder.isSetOrExpired()) {
                    //----------------------分发结果-START-----------------------
                    //异步分发结果
                    doGetBindSendResultThreadPool(entry.getKey()).submit(() -> {
                        boundResultRequestHolder.setResult(handledResult);
                    });
                    //----------------------分发结果-END-------------------------
                    ++processCnt;
                    if (log.isTraceEnabled()) {
                        long sl = System.currentTimeMillis();
                        log.trace("FQ轮询查询请求绑定结果处理完成，请求开始时间：{},查询动作开始时间：{},完成时间：{},处理耗时：{}ms",
                                boundResultRequestHolder.getReqStartTime().getTime(), startTime.getTime(), sl,
                                sl - boundResultRequestHolder.getReqStartTime().getTime());
                    }
                }
            }
            if (log.isTraceEnabled()) {
                log.trace("处理轮询查询请求对应viewCode：{} 的绑定结果：{}条。", entry.getKey(), processCnt);
            }
        }
        return num;
    }
}
