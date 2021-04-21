package com.edu.pollingquery.server;

import com.edu.pollingquery.context.BoundResultRequestContextHolder;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * 抽象查询接收器
 *
 * @author jcb
 * @since 2021/1/22
 */
public abstract class AbstractQueryServer<T> {

    /**
     * 登记查询请求绑定结果
     * @param additionalDeferredResult 请求绑定的结果
     */
    public abstract DeferredResult<T> registerQueryRequest(BoundResultRequestContextHolder<T> additionalDeferredResult);

}
