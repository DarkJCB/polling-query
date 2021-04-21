package com.edu.pollingquery.dispatcher;

import com.edu.pollingquery.model.ResultWrapper;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象结果派发器
 *
 * @author jcb
 * @since 2021/1/22
 */
@Slf4j
public abstract class AbstractResultDispatcher<T> {

    /**
     * 接收处理结果执行派发处理
     * @param resultWrapper 结果
     */
    public abstract void acceptResult(ResultWrapper<T> resultWrapper);

    /**
     * 通知返回查询绑定结果
     * @param resultWrapper 查询结果
     */
    public abstract int processAndDispatchReturnResult(ResultWrapper<T> resultWrapper);
}
