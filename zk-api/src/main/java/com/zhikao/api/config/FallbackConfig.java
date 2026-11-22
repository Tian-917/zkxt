package com.zhikao.api.config;

import com.zhikao.api.client.course.fallback.CourseClientFallback;
import com.zhikao.api.client.learning.fallback.LearningClientFallback;
import com.zhikao.api.client.promotion.PromotionClient;
import com.zhikao.api.client.promotion.fallback.PromotionClientFallback;
import com.zhikao.api.client.search.fallback.SearchClientFallback;
import com.zhikao.api.client.remark.fallback.RemarkClientFallback;
import com.zhikao.api.client.trade.fallback.TradeClientFallback;
import com.zhikao.api.client.user.fallback.UserClientFallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FallbackConfig {
    @Bean
    public LearningClientFallback learningClientFallback(){
        return new LearningClientFallback();
    }

    @Bean
    public TradeClientFallback tradeClientFallback(){
        return new TradeClientFallback();
    }

    @Bean
    public UserClientFallback userClientFallback(){
        return new UserClientFallback();
    }

    @Bean
    public RemarkClientFallback remarkClientFallback(){return new RemarkClientFallback();}

    @Bean
    public PromotionClientFallback promotionClientFallback(){return new PromotionClientFallback();}

    @Bean
    public CourseClientFallback courseClientFallback(){return new CourseClientFallback();}

    @Bean
    public SearchClientFallback searchClientFallback(){return new SearchClientFallback();}
}
