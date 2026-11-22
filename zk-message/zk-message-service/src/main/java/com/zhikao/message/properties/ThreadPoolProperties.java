package com.zhikao.message.properties;

import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 线程池配置
 * @ClassName ThreadPoolProperties
 * @author Tian
 * @since 2026-11-19
 * @version 1.0.0
 **/
@Configuration
@Data
public class ThreadPoolProperties {

    @NestedConfigurationProperty
    private Map<String, Object> threadPools;


}
