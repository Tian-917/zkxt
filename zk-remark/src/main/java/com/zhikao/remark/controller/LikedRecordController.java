package com.zhikao.remark.controller;


import com.zhikao.remark.domain.dto.LikeRecordFormDTO;
import com.zhikao.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 前端控制器
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
public class LikedRecordController {

    private final ILikedRecordService iLikedRecordService;

    /**
     * 点赞或取消点赞
     * @param likeRecordFormDTO
     */
    @PostMapping
    public void addLikeRecord(@Valid @RequestBody LikeRecordFormDTO likeRecordFormDTO) {
        iLikedRecordService.addLikeRecord(likeRecordFormDTO);
    }

    /**
     * 查询指定业务id的点赞状态
     * @param bizIds
     * @return
     */
    @GetMapping("/list")
    public Set<Long> isBizLiked(@RequestParam("bizIds") List<Long> bizIds) {
        return iLikedRecordService.isBizLiked(bizIds);
    }
}
