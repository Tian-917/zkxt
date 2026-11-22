package com.zhikao.api.client.search;

import com.zhikao.api.client.search.fallback.SearchClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "search-service", fallbackFactory = SearchClientFallback.class)
public interface SearchClient {

    @GetMapping("/courses/name")
    List<Long> queryCoursesIdByName(
            @RequestParam(value = "keyword", required = false) String keyword);
}
