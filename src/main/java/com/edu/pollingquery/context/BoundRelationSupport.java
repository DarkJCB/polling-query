package com.edu.pollingquery.context;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

/**
 * 绑定关系操作支持
 *
 * @author jcb
 * @since 2021/2/20
 */
@Slf4j
@Component
public class BoundRelationSupport<T> {

    /**
     * 编码和参数的分隔符
     */
    private final static String CODE_PARAM_SEPARATOR = "::";

    /**
     * 默认根据viewCode生成的actionCode前缀
     */
    private final static String ACTION_PREFIX = "Q_";

    /**
     * 绑定关系对象Spring单例
     */
    private final BoundRelationContextHolder<T> boundRelationContext;

    public BoundRelationSupport(BoundRelationContextHolder<T> boundRelationContext) {
        this.boundRelationContext = boundRelationContext;
    }

    /**
     * 包装视图名称
     * @param viewCode 视图编码
     * @param requestParam 请求参数对象
     * @return 视图名称
     */
    private String wrapperViewCodeByParam(String viewCode, Object requestParam){
        if(viewCode == null || requestParam == null){
            return viewCode;
        }
        return viewCode.concat(CODE_PARAM_SEPARATOR).concat(requestParam.toString());
    }

    /**
     * 包装动作编码
     * @param actionCode 动作编码
     * @param requestParam 请求参数对象
     * @return 动作编码
     */
    private String wrapperActionCodeByParam(String actionCode, Object requestParam){
        if(actionCode == null || requestParam == null){
            return actionCode;
        }
        return actionCode.concat(CODE_PARAM_SEPARATOR).concat(requestParam.toString());
    }

    /**
     * 无模板直接生成默认绑定关系
     * @param viewCode 视图编码
     * @param requestParam 请求参数对象
     * @return 生成的绑定关系
     */
    private Map.Entry<String, Set<String>> generateBindRelationWithOutTemplate(String viewCode, Object requestParam) {
        String viewCodeWithParam = wrapperViewCodeByParam(viewCode, requestParam);
        return viewCodeWithParam == null ? null :
                new AbstractMap.SimpleEntry<>(ACTION_PREFIX + viewCodeWithParam, Sets.newHashSet(viewCodeWithParam));
    }

    /**
     * 根据绑定关系模板生成绑定关系
     * @param relationTemplate 绑定关系模板
     * @param requestParam 请求参数对象
     * @return 生成的绑定关系
     */
    private Map.Entry<String, Set<String>> generateBindRelationByTemplate(Map.Entry<String, Set<String>> relationTemplate,
                                                                          Object requestParam){
        if(relationTemplate.getKey() == null || relationTemplate.getValue() == null){
            return null;
        }
        String actionCode = wrapperActionCodeByParam(relationTemplate.getKey(),requestParam);
        Set<String> viewCodes = relationTemplate.getValue().stream()
                .map(viewCode -> wrapperViewCodeByParam(viewCode, requestParam))
                .collect(Collectors.toSet());
        return CollectionUtils.isEmpty(viewCodes) ? null : new AbstractMap.SimpleEntry<>(actionCode, viewCodes);
    }

    /**
     * 通过视图编码（无参数部分）获取初始配置的绑定关系模板
     * @param viewCode 视图编码
     * @return 绑定关系
     */
    private Map.Entry<String, Set<String>> obtainRelationTemplateByViewCode(String viewCode){
        if(boundRelationContext.getActionBoundTemplateEntries().isEmpty()) {
            if(log.isTraceEnabled()) {
                log.trace("视图绑定的查询动作关系模板未配置！");
            }
            return null;
        }
        for (Map.Entry<String, Set<String>> entry : boundRelationContext.getActionBoundTemplateEntries()) {
            if (CollectionUtils.isEmpty(entry.getValue())) {
                continue;
            }
            if (entry.getValue().contains(viewCode)) {
                return entry;
            }
        }
        if(log.isTraceEnabled()) {
            log.trace("无法获取到视图绑定的查询动作关系模板配置！viewCode:" + viewCode);
        }
        return null;
    }

    /**
     * 根据视图编码查询动作编码（实际查询结果分发查询时依赖该编码）
     * @param viewCodeWithParam 视图编码（已经根据参数组装）
     * @return 动作编码
     */
    private String obtainActionCodeByViewCode(String viewCodeWithParam){
        for(Map.Entry<String, Set<String>> entry : boundRelationContext.getActionBoundEntries()){
            if(CollectionUtils.isEmpty(entry.getValue())){
                continue;
            }
            if(entry.getValue().contains(viewCodeWithParam)){
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 获取或生成（包含绑定操作）动作编码
     * @param viewCode 视图编码（原始无参数拼接）
     * @param requestParam 请求参数
     * @return 动作编码
     */
    public String obtainOrCreateActionCode(String viewCode, Object requestParam){
        //根据参数和原始视图编码生成要查询的视图编码
        String viewCodeWithParam = wrapperViewCodeByParam(viewCode, requestParam);
        //获取动作编码
        String actionCodeWithParam = obtainActionCodeByViewCode(viewCodeWithParam);
        //如果已经绑定则直接返回，没有绑定则生成绑定关系后执行绑定操作再返回动作编码
        if(StringUtils.isEmpty(actionCodeWithParam)){
            Map.Entry<String, Set<String>> relationTemplate = obtainRelationTemplateByViewCode(viewCode);
            //生成绑定关系
            Map.Entry<String, Set<String>> bindRelation =
                    relationTemplate == null ? generateBindRelationWithOutTemplate(viewCode, requestParam) :
                            generateBindRelationByTemplate(relationTemplate, requestParam);
            if(bindRelation == null){
                return null;
            }
            //绑定查询动作和视图（支持查询参数）
            if(boundRelationContext.bindActionAndViews(bindRelation.getKey(), bindRelation.getValue())){
                actionCodeWithParam = bindRelation.getKey();
            }
        }
        return actionCodeWithParam;
    }

    /**
     * 获取视图绑定的线程安全的优先级队列，如果队列不存在会先创建再绑定到视图
     * @param viewCode 视图编码（原始无参数拼接）
     * @param requestParam requestParam 请求参数
     * @return 视图绑定的队列信息
     */
    public Queue<BoundResultRequestContextHolder<T>> ObtainViewBoundResultQueue(String viewCode, Object requestParam){
        Queue<BoundResultRequestContextHolder<T>> boundResultQueue =
                boundRelationContext.initViewBoundResultQueueIfAbsent(
                        wrapperViewCodeByParam(viewCode, requestParam),
                        new PriorityBlockingQueue<>(11,
                                Comparator.comparing(BoundResultRequestContextHolder::getReqStartTime))
                        /*Queues.synchronizedQueue(
                                new PriorityQueue<BoundResultRequestContextHolder<T>>(
                                        Comparator.comparing(BoundResultRequestContextHolder::getReqStartTime)
                                )
                        )*/

                );
        if(log.isTraceEnabled()){
            log.trace("绑定视图查询队列记录：{}", boundResultQueue.toString());
        }
        return boundResultQueue;
    }
}
