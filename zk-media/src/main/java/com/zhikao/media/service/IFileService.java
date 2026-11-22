package com.zhikao.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.media.domain.dto.FileDTO;
import com.zhikao.media.domain.po.File;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 文件表，可以是普通文件、图片等 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IFileService extends IService<File> {

    FileDTO uploadFile(MultipartFile file);

    FileDTO getFileInfo(Long id);
}
