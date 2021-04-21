package com.edu.pollingquery.processor;

import com.edu.pollingquery.context.BoundResultRequestContextHolder;

/**
 * 抽象的结果处理器
 *
 * @author jcb
 * @since 2021/1/26
 */
public interface ResultPostProcessor<T> {

    ResultProcessorType getType();

    T doProcess(BoundResultRequestContextHolder<T> requestHolder, T handlerResult);

    ThreadLocal<Object> initProcessorTypeContext();
}
