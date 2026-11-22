package com.zhikao.learning.service.impl;

import com.zhikao.learning.domain.po.NoteUser;
import com.zhikao.learning.mapper.NoteUserMapper;
import com.zhikao.learning.service.INoteUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Tian
 */
@Service
public class NoteUserServiceImpl extends ServiceImpl<NoteUserMapper, NoteUser> implements INoteUserService {

}
