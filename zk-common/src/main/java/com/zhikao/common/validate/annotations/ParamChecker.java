package com.zhikao.common.validate.annotations;

/**
 * 接口方法参数校验器
 * @ClassName ParamChecker
 * @author Tian
 * @since 2026-11-19
 * @version 1.0.0
 **/
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ParamChecker {
}
