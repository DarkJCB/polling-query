package com.edu.pollingquery.handler;

import com.edu.pollingquery.context.BoundRelationContextHolder;
import com.edu.pollingquery.context.BoundResultRequestContextHolder;
import com.edu.pollingquery.context.Command;
import com.edu.pollingquery.dispatcher.AbstractResultDispatcher;
import com.edu.pollingquery.model.ResultWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * 请求处理器默认实现
 *
 * @author jcb
 * @since 2021/1/25
 */
@Slf4j
@Component
public class DefaultRequestHandler<T> extends AbstractRequestHandler<T>{

    private static final Map<String, AsyncTaskExecutor> ACTION_MAP = new ConcurrentHashMap<>();

    private final BlockingQueue<ResultWrapper<T>> rstQueue = new LinkedBlockingQueue<>();

    private static AtomicBoolean isDispatcherStarted = new AtomicBoolean(false);

    private AbstractResultDispatcher<T> dispatcher;

    private BoundRelationContextHolder<T> boundRelationContext;

    public DefaultRequestHandler(AbstractResultDispatcher<T> dispatcher, BoundRelationContextHolder<T> boundRelationContext) {
        this.dispatcher = dispatcher;
        this.boundRelationContext = boundRelationContext;
    }

    @Override
    public void registerAction(String actionCode, Command<T> command, Object param) {
        doGetBindThreadPool(actionCode).submit(() -> {
            //定义处理结果
            T rst = null;
            //本次处理是否被忽略
            boolean isIgnore = false;
            //增加睡眠延迟，等待更高的并发 后续考虑基于带有策略的计数器动态生成睡眠时间
            this.sleepToWaitMoreReq(10L);

            //开始处理查询请求,调用实际查询方法处理
            Date startTime = new Date();
            try {
                if(hasUnProcessData(actionCode, startTime)){
                    if(log.isTraceEnabled()) {
                        log.trace("开始处理轮询查询请求：task startTime={}", startTime.getTime());
                    }
                    rst = command.execute();
                }else {
                    isIgnore = true;
                    if(log.isTraceEnabled()) {
                        log.trace("忽略轮询查询请求：task startTime={}, actionCode={}", startTime.getTime(), actionCode);
                    }
                }
            }catch (Exception e){
                log.error(e.getMessage(), e);
            }finally {
                if(!isIgnore) {
                    if (log.isTraceEnabled()) {
                        log.trace("将轮询查询请求处理结果放入到队列中：task startTime={}, actionCode={}",
                                startTime.getTime(), actionCode);
                    }
                    rstQueue.offer(new ResultWrapper<T>(startTime, actionCode, rst));
                }
            }
        });
        if(!isDispatcherStarted.get() && isDispatcherStarted.compareAndSet(false,true)){
            startDispatchResultThread();
        }
    }

    class DispatchResultThread implements Runnable {

        @Override
        public void run() {
            while (!(Thread.interrupted())) {
                try {
                    ResultWrapper<T> rw = rstQueue.take();
                    if (log.isTraceEnabled()) {
                        log.trace("接收到处理完成结果 actionCode:{}, requestTime:{}" + rw.getActionCode(), rw.getStartTime());
                    }
                    dispatcher.acceptResult(rw);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 启动派发结果处理的守护线程
     */
    private void startDispatchResultThread() {
        Thread thread = new Thread(new DispatchResultThread());
        thread.setName("pollingCache-dispatchResult-thread");
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((t, e) -> log.error(t + " throws exception: " + e));
        log.info("启动轮询查询派发结果线程!");
        thread.start();
    }

    /**
     * 创建线程池执行器
     * @param actionCode 动作代号
     * @return 执行器
     */
    private AsyncTaskExecutor createThreadPool(String actionCode){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //线程名称前缀
        executor.setThreadNamePrefix(actionCode + "-executor-");
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
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(2);
        executor.setKeepAliveSeconds(0);
        //默认DiscardPolicy
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }

    public AsyncTaskExecutor doGetBindThreadPool(String actionCode){
        AsyncTaskExecutor executor = ACTION_MAP.get(actionCode);
        if(executor != null){
            return executor;
        }else {
            ACTION_MAP.putIfAbsent(actionCode, createThreadPool(actionCode));
            return ACTION_MAP.get(actionCode);
        }
    }

    /**
     * 优化查询，查询开始时如果无需要处理的请求，则忽略本次查询操作
     * @param actionCode 动作编码
     * @param queryTime 查询请求开始时间
     * @return 判断是否包含未处理的数据
     */
    private boolean hasUnProcessData(String actionCode, Date queryTime){
        Set<String> relationCodeSet = boundRelationContext.getViewsByActionCode(actionCode);
        if(CollectionUtils.isEmpty(relationCodeSet)){
            if (log.isTraceEnabled()) {
                log.trace("查询请求抛弃[未找到关系集合], actionCode:{}, queryTime:{}" + actionCode, queryTime.getTime());
            }
            return false;
        }
        for(String viewCode : relationCodeSet) {
            Queue<BoundResultRequestContextHolder<T>> boundResultQueue =
                    boundRelationContext.getViewBoundResultQueueByViewCode(viewCode);
            if(boundResultQueue != null) {
                BoundResultRequestContextHolder<T> boundResultRequestHolder = boundResultQueue.peek();
                if(boundResultRequestHolder != null) {
                    if(!boundResultRequestHolder.getReqStartTime().after(queryTime)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    //等待更多请求一同处理
    private void sleepToWaitMoreReq(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex){
            log.error(ex.getMessage(),ex);
        }
    }
}
