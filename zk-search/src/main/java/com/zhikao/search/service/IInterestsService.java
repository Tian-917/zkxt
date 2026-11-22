package com.zhikao.search.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.search.domain.po.Interests;
import com.zhikao.api.dto.course.CategoryBasicDTO;

import java.util.List;

/**
 * <p>
 * 用户兴趣表，保存感兴趣的二级分类id 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IInterestsService extends IService<Interests> {

    void saveInterests(List<Long> interestedIds);

    List<CategoryBasicDTO> queryMyInterests();

    List<Long> queryMyInterestsIds();
}
