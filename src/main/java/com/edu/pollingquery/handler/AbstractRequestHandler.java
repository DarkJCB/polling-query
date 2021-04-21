package com.edu.pollingquery.handler;

import com.edu.pollingquery.context.Command;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 抽象的请求处理器
 *
 * @author jcb
 * @since 2021/1/22
 */
public abstract class AbstractRequestHandler<T> {
    /**
     * 注册获取数据的操作
     * @param actionCode 动作编号
     * @param func 动作的方法
     */
    public abstract void registerAction(String actionCode, Command<T> func, Object param);
}
