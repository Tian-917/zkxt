package com.zhikao.pay.third.wx.config;

import cn.hutool.crypto.PemUtil;
import com.wechat.pay.contrib.apache.httpclient.WechatPayHttpClientBuilder;
import com.wechat.pay.contrib.apache.httpclient.auth.PrivateKeySigner;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Credentials;
import com.wechat.pay.contrib.apache.httpclient.auth.WechatPay2Validator;
import com.wechat.pay.contrib.apache.httpclient.cert.CertificatesManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;

/**
 * 微信支付客户端配置。
 * <p>
 * 注意：{@link CertificatesManager#putMerchant} 会**立即调用微信支付接口下载平台证书**，
 * 商户凭证失效/过期时这里会抛 401，导致整个 pay-service 起不来。
 * <p>
 * 因此这里改成"失败降级"：拿不到证书只告警、不阻断启动；真正发起支付时才会报错，
 * 这样没有真实商户号的本地/演示环境也能把服务跑起来。
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(WxPayProperties.class)
public class WxConfiguration {

    /**
     * 微信支付证书管理器（初始化失败不阻断启动）
     */
    @Bean(destroyMethod = "stop")
    public CertificatesManager certificatesManager(WxPayProperties properties) {
        CertificatesManager certificatesManager = CertificatesManager.getInstance();
        try {
            PrivateKey privateKey = PemUtil.readPemPrivateKey(
                    new ByteArrayInputStream(properties.getPrivateKey().getBytes(StandardCharsets.UTF_8)));
            PrivateKeySigner privateKeySigner = new PrivateKeySigner(properties.getMchSerialNo(), privateKey);
            WechatPay2Credentials credentials = new WechatPay2Credentials(properties.getMchId(), privateKeySigner);
            certificatesManager.putMerchant(
                    properties.getMchId(), credentials, properties.getApiV3Key().getBytes(StandardCharsets.UTF_8));
            log.info("微信支付平台证书初始化成功，mchId={}", properties.getMchId());
        } catch (Exception e) {
            // 凭证缺失/失效（常见于演示环境）时降级：服务照常启动，支付调用会失败
            log.warn("微信支付平台证书初始化失败，支付功能不可用：{}", e.getMessage());
        }
        return certificatesManager;
    }

    @Bean
    public CloseableHttpClient closeableHttpClient(WxPayProperties properties, CertificatesManager certificatesManager) {
        WechatPayHttpClientBuilder builder = WechatPayHttpClientBuilder.create();
        try {
            PrivateKey privateKey = PemUtil.readPemPrivateKey(
                    new ByteArrayInputStream(properties.getPrivateKey().getBytes(StandardCharsets.UTF_8)));
            builder.withMerchant(properties.getMchId(), properties.getMchSerialNo(), privateKey);
            builder.withValidator(new WechatPay2Validator(certificatesManager.getVerifier(properties.getMchId())));
            return builder.build();
        } catch (Exception e) {
            // 没有商户凭证时构建器会直接抛"缺少身份认证信息"，这里退化成普通 HttpClient，保证服务能启动
            log.warn("微信支付 HTTP 客户端降级为普通客户端，支付功能不可用：{}", e.getMessage());
            return HttpClients.createDefault();
        }
    }
}
