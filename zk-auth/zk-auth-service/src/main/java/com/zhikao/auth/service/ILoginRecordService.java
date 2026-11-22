package com.zhikao.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.auth.domain.po.LoginRecord;

/**
 * <p>
 * 登录信息记录表 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface ILoginRecordService extends IService<LoginRecord> {

    void saveAsync(LoginRecord record);

    void loginSuccess(String cellphone, Long userId);
}
