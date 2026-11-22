package com.zhikao.data.service;


import com.zhikao.data.model.dto.BoardDataSetDTO;
import com.zhikao.data.model.vo.EchartsVO;

import java.util.List;

/**
 * @ClassName BoardService
 * @Author Tian
 * @Date 2026-11-19
 * @Version
 **/
public interface BoardService {

    /**
     * 看板数据获取
     *
     * @param types 数据类型
     * @return
     */
    EchartsVO boardData(List<Integer> types);

    /**
     * 设置看板数据
     *
     * @param boardDataSetDTO
     */
    void setBoardData(BoardDataSetDTO boardDataSetDTO);
}