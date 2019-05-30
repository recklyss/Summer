package com.zhang.summer.MyAnnotation;

import java.lang.annotation.*;

/**
 * @Author zhangjiaheng
 * @Description 自定义controller注解
 **/
@Target(ElementType.TYPE)// 只能使用在Java类上面
@Retention(RetentionPolicy.RUNTIME)// 运行时
@Documented
public @interface MyController {
}
