package com.zhikao.authsdk.gateway.config;

import com.zhikao.authsdk.gateway.util.AuthUtil;
import com.zhikao.authsdk.gateway.util.JwtSignerHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class AuthAutoConfiguration {

    @Bean
    @ConditionalOnClass(DiscoveryClient.class)
    public JwtSignerHolder jwtSignerHolder(DiscoveryClient discoveryClient,
                                           @Value("${zk.auth.jwk-client-id:}") String jwkClientId,
                                           @Value("${zk.auth.jwk-client-secret:}") String jwkClientSecret){
        return new JwtSignerHolder(discoveryClient, jwkClientId, jwkClientSecret);
    }

    @Bean
    public AuthUtil authUtil(JwtSignerHolder jwtSignerHolder, StringRedisTemplate stringRedisTemplate){
        return new AuthUtil(jwtSignerHolder, stringRedisTemplate);
    }
}
