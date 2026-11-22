package com.zhikao.data.service;


import com.zhikao.data.model.dto.TodayDataDTO;
import com.zhikao.data.model.vo.TodayDataVO;

/**
 * @author Tian
 * @since 2026-11-19
 **/
public interface TodayDataService {

    /**
     * 获取今日数据
     * @return
     */
    TodayDataVO get();

    /**
     * 设置今日数据
     * @param todayDataDTO
     */
    void set(TodayDataDTO todayDataDTO);
}