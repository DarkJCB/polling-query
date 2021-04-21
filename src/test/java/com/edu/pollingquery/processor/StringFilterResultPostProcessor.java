package com.edu.pollingquery.processor;

import com.edu.pollingquery.context.BoundResultRequestContextHolder;
import org.springframework.stereotype.Component;

/**
 * Description goes here
 *
 * @author jcb
 * @since 2021/1/28
 */
@Component
public class StringFilterResultPostProcessor implements ResultPostProcessor<String> {

    @Override
    public ResultProcessorType getType() {
        return ResultProcessorType.FILTER;
    }

    @Override
    public String doProcess(BoundResultRequestContextHolder<String> requestHolder, String handlerResult) {
        System.out.println("filter string");
        return handlerResult;
    }

    @Override
    public ThreadLocal<Object> initProcessorTypeContext() {
        return null;
    }
}
