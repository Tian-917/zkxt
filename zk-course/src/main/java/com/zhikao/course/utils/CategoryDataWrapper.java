package com.zhikao.course.utils;

import com.zhikao.common.utils.TreeDataUtils;
import com.zhikao.course.domain.po.Category;
import com.zhikao.course.domain.vo.SimpleCategoryVO;

import java.util.List;

/**
 * @ClassName CategoryDataWrapper
 * @Author Tian
 * @Date 2026-11-19
 * @Version
 **/
public class CategoryDataWrapper implements TreeDataUtils.DataProcessor<SimpleCategoryVO, Category> {

    @Override
    public Object getParentKey(Category category) {
        return category.getParentId();
    }

    @Override
    public Object getKey(Category category) {
        return category.getId();
    }

    @Override
    public Object getRootKey() {
        return 0L;
    }

    @Override
    public List<SimpleCategoryVO> getChild(SimpleCategoryVO simpleCategoryVO) {
        return simpleCategoryVO.getChildren();
    }

    @Override
    public void setChild(SimpleCategoryVO parent, List<SimpleCategoryVO> child) {
        parent.setChildren(child);
    }
}
