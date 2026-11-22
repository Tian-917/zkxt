package com.zhikao.data.service;


import com.zhikao.data.model.dto.Top10DataSetDTO;
import com.zhikao.data.model.vo.Top10DataVO;

/**
 * @author Tian
 * @since 2026-11-19
 **/
public interface Top10Service {

    /**
     * 获取top数据
     *
     * @return
     */
    Top10DataVO getTop10Data();

    /**
     * top 10数据设置
     * @param top10DataSetDTO
     */
    void setTop10Data(Top10DataSetDTO top10DataSetDTO);
}