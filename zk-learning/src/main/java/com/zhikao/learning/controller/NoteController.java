package com.zhikao.learning.controller;

import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.learning.domain.dto.NoteFormDTO;
import com.zhikao.learning.domain.query.NotePageQuery;
import com.zhikao.learning.domain.vo.NoteVO;
import com.zhikao.learning.service.INoteService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 *  控制器
 * </p>
 *
 * @author Tian
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/notes")
public class NoteController {

    private final INoteService noteService;

    @ApiOperation("新增笔记")
    @PostMapping
    public void saveNote(@Validated(NoteFormDTO.Save.class) @RequestBody NoteFormDTO noteDTO) {
        noteService.saveNote(noteDTO);
    }

    @ApiOperation("采集笔记")
    @PostMapping("/gathers/{id}")
    public void gatherNote(@PathVariable("id") Long id) {
        noteService.gatherNote(id);
    }

    @ApiOperation("取消采集笔记")
    @DeleteMapping("/gathers/{id}")
    public void removeGatherNote(@PathVariable("id") Long id) {
        noteService.removeGatherNote(id);
    }

    @ApiOperation("更新笔记")
    @PutMapping("/{id}")
    public void updateNote(@PathVariable("id") Long id, @Validated(NoteFormDTO.Update.class) @RequestBody NoteFormDTO noteDTO) {
        noteDTO.setId(id);
        noteService.updateNote(noteDTO);
    }

    @ApiOperation("删除我的笔记")
    @DeleteMapping("/{id}")
    public void removeMyNote(@PathVariable("id") Long id) {
        noteService.removeMyNote(id);
    }

    @ApiOperation("用户端分页查询笔记")
    @GetMapping("/page")
    public PageDTO<NoteVO> queryNotePage(@Valid NotePageQuery query) {
        return noteService.queryNotePage(query);
    }
}
