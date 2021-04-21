package com.edu.pollingquery.processor.chain;

import com.edu.pollingquery.context.BoundResultRequestContextHolder;
import com.edu.pollingquery.processor.ResultPostProcessor;
import com.edu.pollingquery.processor.ResultProcessorType;
import com.edu.pollingquery.reflection.GenericsUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认的结果加工器 用于过滤可见数据、增加用户个性化数据等
 *
 * @author jcb
 * @since 2021/1/27
 */
@Slf4j
@Component
public class DefaultProcessorChain<T> extends AbstractProcessorChain<T>{

    private static final Map<ResultProcessorType, ThreadLocal<?>> PROCESSOR_INIT_MAP = new ConcurrentHashMap<>();

    private final List<ResultPostProcessor<T>> resultPostProcessors;

    public DefaultProcessorChain(@Autowired(required = false) List<ResultPostProcessor<T>> resultPostProcessors) {
        this.resultPostProcessors = resultPostProcessors;
    }

    @Override
    public T doProcess(BoundResultRequestContextHolder<T> boundResultRequestHolder, T handlerResult){
        if(boundResultRequestHolder != null && handlerResult != null
                && !CollectionUtils.isEmpty(resultPostProcessors)){
            for (ResultPostProcessor<T> postProcessor : resultPostProcessors){
                try {
                    if(boundResultRequestHolder.getSupportProcessorTypes() == null ||
                            !boundResultRequestHolder.getSupportProcessorTypes().contains(postProcessor.getType())){
                        continue;
                    }
                    if(GenericsUtils.checkCompatibleResultPostProcessor(postProcessor, handlerResult)){
                        if(PROCESSOR_INIT_MAP.get(postProcessor.getType()) == null){
                            PROCESSOR_INIT_MAP.putIfAbsent(postProcessor.getType(), postProcessor.initProcessorTypeContext());
                        }
                        handlerResult = postProcessor.doProcess(boundResultRequestHolder, handlerResult);
                    }
                }catch (ClassCastException cce){
                    log.error("处理器类型适配错误，请检查配置!" + cce.getMessage());
                }
            }
        }
        return handlerResult;
    }

    @Override
    public void clearContext(){
        if(!PROCESSOR_INIT_MAP.isEmpty()){
            PROCESSOR_INIT_MAP.values().forEach(ThreadLocal::remove);
            PROCESSOR_INIT_MAP.clear();
        }
    }
}
