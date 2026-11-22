package com.zhikao.learning.service;

import com.zhikao.learning.domain.vo.SignResultVO;

import java.util.List;

public interface ISignRecordService {
    SignResultVO addSignRecords();

    List<Integer> querySignRecords();
}
