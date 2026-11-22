package com.zhikao.learning.controller;


import com.zhikao.learning.domain.vo.SignResultVO;
import com.zhikao.learning.service.ISignRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("sign-records")
@RequiredArgsConstructor
public class SignRecordController {

    private final ISignRecordService recordService;

    /**
     * 签到功能接口
     * @return
     */
    @PostMapping
    public SignResultVO addSignRecords(){
        return recordService.addSignRecords();
    }

    /**
     * 查询签到记录
     * @return
     */
    @GetMapping
    public List<Integer> querySignRecords(){ return recordService.querySignRecords();}
}
