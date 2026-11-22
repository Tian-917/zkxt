package com.zhikao.promotion.mapper;

import com.zhikao.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 * 优惠券的规则信息 Mapper 接口
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface CouponMapper extends BaseMapper<Coupon> {

    @Update("UPDATE coupon SET issue_num = issue_num + 1 WHERE id = #{couponId} AND issue_num < total_num")
    int incrIssueNum(@Param("couponId") Long couponId);

    /**
     * 批量增减优惠券已使用数量
     *
     * @param couponIds 优惠券模板id集合
     * @param amount    增减量，核销传1，退还传-1
     */
    @Update("<script>" +
            "UPDATE coupon SET used_num = used_num + #{amount} WHERE id IN " +
            "<foreach collection='couponIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int incrUsedNum(@Param("couponIds") List<Long> couponIds, @Param("amount") int amount);
}
