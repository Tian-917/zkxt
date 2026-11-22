package com.zhikao.learning.mq.message;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class SignInMessage {
    public Long userId;
    public Integer point;
}
