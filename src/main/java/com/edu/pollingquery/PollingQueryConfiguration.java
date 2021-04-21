package com.edu.pollingquery;

import com.edu.pollingquery.config.ActionBoundConfigurer;
import com.edu.pollingquery.context.BoundRelationContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 轮询方式查询数据缓存通用配置
 *
 * @author jcb
 * @since 2021/1/28
 */
@ComponentScan
@Configuration
public class PollingQueryConfiguration {

    @Autowired
    protected void initActionBound(BoundRelationContextHolder boundRelationContext, ActionBoundConfigurer config){
        boundRelationContext.initActionBound(config.actionBoundSupplier());
    }
}
