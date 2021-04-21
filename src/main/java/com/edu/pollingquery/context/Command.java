package com.edu.pollingquery.context;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 请求方法管理者
 *
 * @author jcb
 * @since 2021/4/16
 */
@Slf4j
public class Command<T> {

    Function<Object, T> func;

    Object params;

    Supplier<T> supplier;

    public Command(Function<Object, T> func, Object params, Supplier<T> supplier) {
        this.func = func;
        this.params = params;
        this.supplier = supplier;
    }

    public Function<Object, T> getFunc() {
        return func;
    }

    public Object getParams() {
        return params;
    }

    public Supplier<T> getSupplier() {
        return supplier;
    }

    /**
     * 执行请求处理动作
     * 如果包含supplier和func 优先使用supplier提供的逻辑
     * @return 请求处理结果
     */
    public T execute(){
        if(supplier != null){
            return supplier.get();
        }else if(func != null){
            return func.apply(params);
        }else{
            log.warn("未指定执行方法，请检查处理逻辑！");
            return null;
        }
    }

    @Override
    public String toString() {
        return "Command{" +
                "func=" + func +
                ", params=" + params +
                ", supplier=" + supplier +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Command<?> command = (Command<?>) o;
        return Objects.equals(func, command.func) &&
                Objects.equals(params, command.params) &&
                Objects.equals(supplier, command.supplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(func, params, supplier);
    }
}
