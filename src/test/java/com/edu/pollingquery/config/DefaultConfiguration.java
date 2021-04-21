package com.edu.pollingquery.config;

import com.google.common.collect.Sets;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Description goes here
 *
 * @author jcb
 * @since 2021/1/28
 */
@Component
public class DefaultConfiguration implements ActionBoundConfigurer{
    @Override
    public Map<String, Set<String>> actionBoundSupplier() {
        Map<String,Set<String>> map = new HashMap<>();
        map.put("QUERY_FLIGHT", Sets.newHashSet("FLIGHT_LIST"));
        map.put("QUERY_GANTT", Sets.newHashSet("GANT_INFO"));
        return map;
    }
}
