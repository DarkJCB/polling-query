package com.edu.pollingquery.server;

import com.edu.pollingquery.context.BoundRelationSupport;
import com.edu.pollingquery.context.BoundResultRequestContextHolder;
import com.edu.pollingquery.context.Command;
import com.edu.pollingquery.handler.AbstractRequestHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Queue;

/**
 * 查询接收器默认实现
 *
 * @author jcb
 * @since 2021/1/22
 */
@Slf4j
@Component
public class DefaultQueryServer<T> extends AbstractQueryServer<T> {

    private final BoundRelationSupport<T> boundRelationSupport;

    private AbstractRequestHandler<T> handler;

    public DefaultQueryServer(AbstractRequestHandler<T> handler, BoundRelationSupport<T> boundRelationSupport) {
        this.handler = handler;
        this.boundRelationSupport = boundRelationSupport;
    }

    /**
     * 验证请求是否可用
     * @param requestHolder 请求信息
     * @throws IllegalArgumentException 请求无效异常
     */
    private void checkRequest(BoundResultRequestContextHolder<T> requestHolder) throws IllegalArgumentException{
        if(requestHolder == null || requestHolder.getReqStartTime() == null){
            throw new IllegalArgumentException("无效的请求！");
        }
        if(requestHolder.getFunction() == null && requestHolder.getSupplier() == null) {
            throw new IllegalArgumentException("未指定请求处理方法！");
        }
        if(requestHolder.getFunction() != null && requestHolder.getSupplier() != null) {
            throw new IllegalArgumentException("无法指定多个处理方法！");
        }
    }

    /**
     * 构造执行命令
     * @param requestHolder 请求信息
     * @return 执行命令
     */
    private Command<T> buildCommand(BoundResultRequestContextHolder<T> requestHolder){
        return new Command<T>(requestHolder.getFunction(), requestHolder.getParam(), requestHolder.getSupplier());
    }

    /**
     * 注册查询请求
     * @param requestHolder 请求持有者
     * @return 异步结果
     */
    @Override
    public DeferredResult<T> registerQueryRequest(BoundResultRequestContextHolder<T> requestHolder) {
        if(log.isTraceEnabled()){
            log.trace("接收到轮询查询请求：{}", requestHolder == null ? "" : requestHolder.toString());
        }
        //valid request is usable
        checkRequest(requestHolder);

        //获取视图编码
        String viewCode = requestHolder.getViewCode();
        if(StringUtils.isEmpty(viewCode)){
            log.error("未指定视图，无法使用轮询查询模式！");
            requestHolder.setResult(requestHolder.getFunction().apply(requestHolder.getParam()));
            return requestHolder;
        }

        //根据配置项获取视图对应的查询动作
        String actionCode = boundRelationSupport.obtainOrCreateActionCode(viewCode, requestHolder.getParam());
        if(StringUtils.isEmpty(actionCode)){
            requestHolder.setResult(requestHolder.getFunction().apply(requestHolder.getParam()));
            return requestHolder;
        }

        //初始化线程安全的优先级队列
        Queue<BoundResultRequestContextHolder<T>> boundResultQueue =
                boundRelationSupport.ObtainViewBoundResultQueue(viewCode, requestHolder.getParam());

        //绑定结果到队列中
        assert boundResultQueue != null;
        if(!boundResultQueue.offer(requestHolder)){
            requestHolder.setErrorResult(new Exception("注册到队列失败！"));
            return requestHolder;
        }else{
            if(log.isTraceEnabled()) {
                log.trace("将轮询查询请求放入队列：startTime={}, viewCode={}",
                        requestHolder.getReqStartTime().getTime(), viewCode);
            }
        }

        //注册待执行的查询动作
        handler.registerAction(actionCode, buildCommand(requestHolder), requestHolder.getParam());
        return requestHolder;
    }

}
