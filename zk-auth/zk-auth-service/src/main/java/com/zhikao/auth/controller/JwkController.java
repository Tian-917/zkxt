package com.zhikao.auth.controller;

import cn.hutool.core.codec.Base64;
import com.zhikao.auth.common.constants.JwtConstants;
import com.zhikao.common.exceptions.UnauthorizedException;
import com.zhikao.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.security.KeyPair;
import java.util.Objects;

@RestController
@RequestMapping("jwks")
@ApiIgnore
public class JwkController {

    private final KeyPair keyPair;

    @Value("${zk.auth.jwk-client-id:}")
    private String jwkClientId;

    @Value("${zk.auth.jwk-client-secret:}")
    private String jwkClientSecret;

    @Autowired
    public JwkController(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    @GetMapping
    public String getJwk(
            @RequestHeader(value = JwtConstants.JWK_CLIENT_ID_HEADER, required = false) String clientId,
            @RequestHeader(value = JwtConstants.JWK_CLIENT_SECRET_HEADER, required = false) String clientSecret) {
        // 配置了客户端凭证时才进行校验，未配置时保持向下兼容
        if (StringUtils.isNotBlank(jwkClientId) && StringUtils.isNotBlank(jwkClientSecret)) {
            if (!Objects.equals(jwkClientId, clientId) || !Objects.equals(jwkClientSecret, clientSecret)) {
                throw new UnauthorizedException("无效的客户端凭证");
            }
        }
        // 获取公钥并转码
        return Base64.encode(keyPair.getPublic().getEncoded());
    }
}
