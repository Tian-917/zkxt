package com.zhikao.course.utils;

import com.zhikao.common.utils.TreeDataUtils;
import com.zhikao.course.domain.po.Category;
import com.zhikao.course.domain.vo.CategoryVO;

import java.util.List;

/**
 * @ClassName CategoryDataWrapper2
 * @Author Tian
 * @Date 2026-11-19
 * @Version
 **/
public class CategoryDataWrapper2 implements TreeDataUtils.DataProcessor<CategoryVO, Category> {
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
    public List<CategoryVO> getChild(CategoryVO categoryVO) {
        return categoryVO.getChildren();
    }

    @Override
    public void setChild(CategoryVO parent, List<CategoryVO> child) {
        parent.setChildren(child);
    }
}
