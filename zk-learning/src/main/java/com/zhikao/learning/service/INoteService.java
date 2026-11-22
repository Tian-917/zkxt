package com.zhikao.learning.service;

import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.learning.domain.dto.NoteFormDTO;
import com.zhikao.learning.domain.po.Note;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.learning.domain.query.NoteAdminPageQuery;
import com.zhikao.learning.domain.query.NotePageQuery;
import com.zhikao.learning.domain.vo.NoteAdminDetailVO;
import com.zhikao.learning.domain.vo.NoteAdminVO;
import com.zhikao.learning.domain.vo.NoteVO;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Tian
 */
public interface INoteService extends IService<Note> {

    void saveNote(NoteFormDTO noteDTO);

    void gatherNote(Long id);

    void removeGatherNote(Long id);

    void updateNote(NoteFormDTO noteDTO);

    PageDTO<NoteVO> queryNotePage(NotePageQuery query);

    PageDTO<NoteAdminVO> queryNotePageForAdmin(NoteAdminPageQuery query);

    NoteAdminDetailVO queryNoteDetailForAdmin(Long id);

    void hiddenNote(Long id, boolean hidden);

    void removeMyNote(Long id);
}
