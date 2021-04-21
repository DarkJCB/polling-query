package com.edu.pollingquery.processor.chain;

import com.edu.pollingquery.context.BoundResultRequestContextHolder;

/**
 * 抽象的结果加工器链 用于过滤可见数据、增加用户个性化数据等
 *
 * @author jcb
 * @since 2021/1/26
 */
public abstract class AbstractProcessorChain<T> {

    public abstract T doProcess(BoundResultRequestContextHolder<T> boundResultRequestHolder, T handlerResult);

    public abstract void clearContext();
}
