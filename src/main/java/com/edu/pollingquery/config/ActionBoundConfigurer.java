package com.edu.pollingquery.config;

import java.util.Map;
import java.util.Set;

/**
 * 事件视图绑定配置
 *
 * @author jcb
 * @since 2021/1/28
 */
public interface ActionBoundConfigurer {
    Map<String, Set<String>> actionBoundSupplier();
}
