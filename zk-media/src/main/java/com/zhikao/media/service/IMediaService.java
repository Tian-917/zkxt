package com.zhikao.media.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.media.domain.dto.MediaDTO;
import com.zhikao.media.domain.dto.MediaUploadResultDTO;
import com.zhikao.media.domain.po.Media;
import com.zhikao.media.domain.query.MediaQuery;
import com.zhikao.media.domain.vo.MediaVO;
import com.zhikao.media.domain.vo.VideoPlayVO;

/**
 * <p>
 * 媒资表，主要是视频文件 服务类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
public interface IMediaService extends IService<Media> {

    String getUploadSignature();

    VideoPlayVO getPlaySignatureBySectionId(Long fileId);

    MediaDTO save(MediaUploadResultDTO mediaResult);

    void updateMediaProcedureResult(Media media);

    void deleteMedia(String fileId);

    VideoPlayVO getPlaySignatureByMediaId(Long mediaId);

    PageDTO<MediaVO> queryMediaPage(MediaQuery query);
}
