package com.zhikao.api.client.search.fallback;

import com.zhikao.api.client.search.SearchClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collections;
import java.util.List;

@Slf4j
public class SearchClientFallback implements FallbackFactory<SearchClient> {
    @Override
    public SearchClient create(Throwable cause) {
        log.error("查询搜索服务出现异常", cause);
        return keyword -> Collections.emptyList();
    }
}
