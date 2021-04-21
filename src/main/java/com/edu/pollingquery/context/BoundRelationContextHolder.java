package com.edu.pollingquery.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 储存绑定关系上下文
 *
 * @author jcb
 * @since 2021/1/27
 */
@Slf4j
@Component
public class BoundRelationContextHolder<T> {

    /**
     * 查询行为编号与对应视图绑定关系模板 一次查询动作结果可以对应多个视图，每个视图绑定多个查询请求结果  依赖外部配置注入
     */
    protected Map<String, Set<String>> QUERY_ACTION_BOUND_TEMPLATE_MAP = new HashMap<>();

    /**
     * 查询行为编号与对应视图绑定关系 一次查询动作结果可以对应多个视图，每个视图绑定多个查询请求结果 动作和视图允许带有请求参数
     */
    protected Map<String, Set<String>> QUERY_ACTION_BOUND_MAP = new ConcurrentHashMap<>();

    /**
     * 视图和绑定结果关系
     */
    protected final Map<String, Queue<BoundResultRequestContextHolder<T>>> VIEW_RESULT_BOUND_MAP = new ConcurrentHashMap<>();

    /**
     * 初始化查询事件和视图关系
     */
    public void initActionBound(Map<String, Set<String>> boundMap){
        if(QUERY_ACTION_BOUND_TEMPLATE_MAP.isEmpty()){
            QUERY_ACTION_BOUND_TEMPLATE_MAP = checkNotEmpty(boundMap, "bound action to view map is not allow to null!");
        }
    }

    public Set<Map.Entry<String, Set<String>>> getActionBoundTemplateEntries(){
        return QUERY_ACTION_BOUND_TEMPLATE_MAP.entrySet();
    }

    public Set<Map.Entry<String, Set<String>>> getActionBoundEntries(){
        return QUERY_ACTION_BOUND_MAP.entrySet();
    }

    public boolean bindActionAndViews(String actionCode, Set<String> viewCodes){
        if(actionCode == null || viewCodes == null || viewCodes.isEmpty()){
            log.error("绑定关系元素为空！actionCode:" + actionCode);
            return false;
        }
        QUERY_ACTION_BOUND_MAP.merge(actionCode, viewCodes, (viewCodes1, viewCodes2) -> {
            if(CollectionUtils.isEmpty(viewCodes1)){
                return viewCodes2;
            }
            viewCodes1.addAll(viewCodes2);
            return viewCodes1;
        });
        return true;
    }

    public Set<String> getViewsByActionCode(String actionCode){
        return QUERY_ACTION_BOUND_MAP.get(actionCode);
    }

    public Queue<BoundResultRequestContextHolder<T>> initViewBoundResultQueueIfAbsent(String viewCode, Queue<BoundResultRequestContextHolder<T>> rstQueue){
        if(!VIEW_RESULT_BOUND_MAP.containsKey(viewCode)) {
            VIEW_RESULT_BOUND_MAP.putIfAbsent(viewCode, rstQueue);
        }
        return VIEW_RESULT_BOUND_MAP.get(viewCode);
    }


    public Queue<BoundResultRequestContextHolder<T>> getViewBoundResultQueueByViewCode(String viewCode){
        return VIEW_RESULT_BOUND_MAP.get(viewCode);
    }

    public static <K, V> Map<K, V> checkNotEmpty(Map<K, V> reference, @Nullable Object errorMessage) {
        if (reference == null || reference.isEmpty()) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        } else {
            return reference;
        }
    }
}
