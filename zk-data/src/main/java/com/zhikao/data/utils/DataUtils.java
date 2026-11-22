package com.zhikao.data.utils;

import com.zhikao.common.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName DataUtils
 * @Author Tian
 * @Date 2026-11-19
 * @Version
 **/
@Slf4j
public class DataUtils {

    public static int getVersion(int totalVersion) {
        return DateUtils.now().getDayOfMonth() % totalVersion;
    }
}
