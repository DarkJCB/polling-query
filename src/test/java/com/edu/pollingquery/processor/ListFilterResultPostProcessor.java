package com.edu.pollingquery.processor;

import com.edu.pollingquery.context.BoundResultRequestContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模拟过滤器
 *
 * @author jcb
 * @since 2021/1/28
 */
@Order(1)
@Component
public class ListFilterResultPostProcessor implements ResultPostProcessor<List<String>> {

    private ThreadLocal<Object> threadLocal = new ThreadLocal<>();

    @Override
    public ResultProcessorType getType() {
        return ResultProcessorType.FILTER;
    }

    @Override
    public List<String> doProcess(BoundResultRequestContextHolder<List<String>> resultHolder, List<String> handlerResult) {
        System.out.println("filter by list with rule:" + threadLocal.get());
        return handlerResult;
    }

    @Override
    public ThreadLocal<Object> initProcessorTypeContext() {
        threadLocal.set(new FilterInfo("rule:13567"));
        return threadLocal;
    }

    static class FilterInfo{
        private String rule;

        public FilterInfo(String rule) {
            this.rule = rule;
        }

        public String getRule() {
            return rule;
        }

        public void setRule(String rule) {
            this.rule = rule;
        }

        @Override
        public String toString() {
            return "FilterInfo{" +
                    "rule='" + rule + '\'' +
                    '}';
        }
    }

}
