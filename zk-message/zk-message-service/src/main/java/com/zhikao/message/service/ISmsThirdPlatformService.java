package com.zhikao.message.service;

import com.zhikao.message.domain.dto.SmsThirdPlatformDTO;
import com.zhikao.message.domain.dto.SmsThirdPlatformFormDTO;
import com.zhikao.message.domain.query.SmsThirdPlatformPageQuery;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.message.domain.po.SmsThirdPlatform;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 第三方云通讯平台 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface ISmsThirdPlatformService extends IService<SmsThirdPlatform> {

    List<SmsThirdPlatform> queryAllPlatform();

    Long saveSmsThirdPlatform(SmsThirdPlatformFormDTO thirdPlatformDTO);

    void updateSmsThirdPlatform(SmsThirdPlatformFormDTO thirdPlatformDTO);

    PageDTO<SmsThirdPlatformDTO> querySmsThirdPlatforms(SmsThirdPlatformPageQuery query);

    SmsThirdPlatformDTO querySmsThirdPlatform(Long id);
}
