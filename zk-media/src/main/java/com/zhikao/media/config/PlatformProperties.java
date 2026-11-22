package com.zhikao.media.config;

import com.zhikao.media.enums.Platform;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "zk.platform")
public class PlatformProperties {
    private Platform file;
    private Platform media;
}
