package com.zhikao.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhikao.api.client.course.CourseClient;
import com.zhikao.api.client.learning.LearningClient;
import com.zhikao.api.client.user.UserClient;
import com.zhikao.api.dto.course.MediaQuoteDTO;
import com.zhikao.api.dto.course.SectionInfoDTO;
import com.zhikao.api.dto.user.UserDTO;
import com.zhikao.common.domain.dto.PageDTO;
import com.zhikao.common.exceptions.ForbiddenException;
import com.zhikao.common.utils.*;
import com.zhikao.media.constants.FileErrorInfo;
import com.zhikao.media.domain.dto.MediaDTO;
import com.zhikao.media.domain.dto.MediaUploadResultDTO;
import com.zhikao.media.domain.po.Media;
import com.zhikao.media.domain.query.MediaQuery;
import com.zhikao.media.domain.vo.MediaVO;
import com.zhikao.media.domain.vo.VideoPlayVO;
import com.zhikao.media.enums.FileStatus;
import com.zhikao.media.mapper.MediaMapper;
import com.zhikao.media.service.IMediaService;
import com.zhikao.media.storage.IMediaStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.zhikao.media.constants.FileErrorInfo.MEDIA_NOT_EXISTS;

/**
 * <p>
 * 媒资表，主要是视频文件 服务实现类
 * </p>
 *
 * @author Tian
 * @since 2026-11-19
 */
@Service
@RequiredArgsConstructor
public class MediaServiceImpl extends ServiceImpl<MediaMapper, Media> implements IMediaService {

    private final IMediaStorage mediaStorage;

    private final CourseClient courseClient;

    private final LearningClient learningClient;

    private final UserClient userClient;

    @Override
    public String getUploadSignature() {
        try {
            return mediaStorage.getUploadSignature();
        } catch (Exception e) {
            // 未配置云点播凭证时不要抛 500，返回空签名让前端提示「暂不支持上传」
            return "";
        }
    }

    @Override
    public VideoPlayVO getPlaySignatureBySectionId(Long sectionId) {
        // 1.根据sectionId查询媒课程信息
        SectionInfoDTO sectionInfo = courseClient.sectionInfo(sectionId);
        Long courseId = sectionInfo.getCourseId();
        // 2.查询用户课程表，是否是购买过的课程
        Long lessonId = learningClient.isLessonValid(courseId);

        if(lessonId != null){
            // 2.1.是，查询媒资信息，直接获取签名
            Media media = getById(sectionInfo.getMediaId());
            AssertUtils.isNotNull(media, MEDIA_NOT_EXISTS);
            // 1）获取签名
            String signature =  mediaStorage.getPlaySignature(media.getFileId(), UserContext.getUser(), null);
            // 2）返回
            VideoPlayVO vo = new VideoPlayVO();
            vo.setSignature(signature);
            vo.setFileId(media.getFileId());
            return vo;
        }
        // 2.2.否，判断课程章节是否免费
        Boolean trailer = sectionInfo.getTrailer();
        if(BooleanUtils.isFalse(trailer)) {
            // 2.3.不免费，抛出异常
            throw new ForbiddenException(FileErrorInfo.MEDIA_NOT_FREE);
        }

        // 3.免费，获取课程信息
        Media media = getById(sectionInfo.getMediaId());
        AssertUtils.isNotNull(media, MEDIA_NOT_EXISTS);
        // 4.获取签名
        String signature =  mediaStorage.getPlaySignature(
                media.getFileId(), UserContext.getUser(), sectionInfo.getFreeDuration());
        // 5.返回
        VideoPlayVO vo = new VideoPlayVO();
        vo.setSignature(signature);
        vo.setFileId(media.getFileId());
        return vo;
    }


    @Override
    public VideoPlayVO getPlaySignatureByMediaId(Long mediaId) {
        // 1.根据id查询媒资信息
        Media media = getById(mediaId);
        // 2.获取签名
        String signature =  mediaStorage.getPlaySignature(media.getFileId(), UserContext.getUser(), null);
        // 3.返回
        VideoPlayVO vo = new VideoPlayVO();
        vo.setSignature(signature);
        vo.setFileId(media.getFileId());
        return vo;
    }

    @Override
    public PageDTO<MediaVO> queryMediaPage(MediaQuery query) {
        // 1.分页条件
        Page<Media> mediaPage = new Page<>(query.getPageNo(), query.getPageSize());
        if(StringUtils.isNotBlank(query.getSortBy())){
            mediaPage.addOrder(new OrderItem(query.getSortBy(), query.getIsAsc()));
        }
        // 2.分页搜索
        lambdaQuery()
                .like(StringUtils.isNotBlank(query.getName()), Media::getFilename, query.getName())
                .page(mediaPage);
        // 3.解析数据
        List<Media> records = mediaPage.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(mediaPage);
        }
        List<Long> ids = new ArrayList<>(records.size());
        Set<Long> createIds = new HashSet<>();
        for (Media m : records) {
            ids.add(m.getId());
            createIds.add(m.getCreater());
        }
        createIds.remove(0L);
        // 4.查询引用次数
        List<MediaQuoteDTO> mediaQuoteDTOS = courseClient.mediaUserInfo(ids);
        AssertUtils.isNotEmpty(mediaQuoteDTOS, FileErrorInfo.MEDIA_QUOTE_NOT_EXISTS);
        Map<Long, Integer> quoteMap = mediaQuoteDTOS
                .stream()
                .collect(Collectors.toMap(MediaQuoteDTO::getMediaId, MediaQuoteDTO::getQuoteNum));

        // 5.查询创建者信息
        Map<Long, String> userMap = null;
        if(CollUtils.isNotEmpty(createIds)) {
            List<UserDTO> users = userClient.queryUserByIds(createIds);
            AssertUtils.isNotEmpty(users, FileErrorInfo.USER_NOT_EXISTS);
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, UserDTO::getName));
        }
        // 6.数据转换
        List<MediaVO> list = new ArrayList<>(records.size());
        for (Media m : records) {
            MediaVO v = BeanUtils.toBean(m, MediaVO.class);
            v.setUseTimes(quoteMap.get(m.getId()));
            if(userMap != null) {
                v.setCreater(userMap.get(m.getCreater()));
            }
            list.add(v);
        }
        return new PageDTO<>(mediaPage.getTotal(), mediaPage.getPages(), list);
    }

    @Override
    public MediaDTO save(MediaUploadResultDTO result) {
        // 1.查询视频信息；未配置云点播凭证时会抛异常，这里降级为用请求里的地址入库
        java.util.List<Media> list;
        try {
            list = mediaStorage.queryMediaInfos(result.getFileId());
        } catch (Exception e) {
            list = java.util.Collections.emptyList();
        }
        // 2.判断是否已存在，幂等处理
        Media media = lambdaQuery().eq(Media::getFileId, result.getFileId()).one();
        if (media != null) {
            return BeanUtils.toBean(media, MediaDTO.class);
        }
        if (list.isEmpty()) {
            // 2.1 拿不到云点播信息：用请求里的地址落库
            media = BeanUtils.toBean(result, Media.class);
            media.setDuration(0F);
            media.setSize(0L);
            media.setStatus(FileStatus.PROCESSED);
            save(media);
            return BeanUtils.toBean(media, MediaDTO.class);
        }
        // 3.保存到数据库
        save(list.get(0));
        return BeanUtils.toBean(list.get(0), MediaDTO.class);
    }

    @Override
    public void updateMediaProcedureResult(Media media) {
        // 1.查询fileId是否已经存在
        Media old = lambdaQuery().eq(Media::getFileId, media.getFileId()).one();
        if (old == null) {
            // 2.如果不存在，新增
            save(media);
        }else {
            // 3.存在，则更新
            lambdaUpdate()
                    .set(Media::getStatus, FileStatus.PROCESSED.getValue())
                    .set(Media::getCoverUrl, media.getCoverUrl())
                    .eq(Media::getId, old.getId())
                    .update();
        }
    }

    @Override
    @Transactional
    public void deleteMedia(String fileId) {
        // 1.删除云端文件
        mediaStorage.deleteFile(fileId);
        // 2.删除本地信息
        remove(new LambdaQueryWrapper<Media>().eq(Media::getFileId, fileId));
    }
}
