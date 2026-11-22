package com.zhikao.api.client.remark;


import com.zhikao.api.client.remark.fallback.RemarkClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

@FeignClient(value = "remark-service",fallbackFactory = RemarkClientFallback.class)
public interface RemarkClient {

    /**
     * 查询指定业务id的点赞状态
     * @param bizIds
     * @return
     */
    @GetMapping("/likes/list")
    Set<Long> isBizLiked(@RequestParam("bizIds") Iterable<Long> bizIds);
}
