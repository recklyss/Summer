package com.zhang.summer.MyAnnotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})// 只能使用在Java类或者方法上面
@Retention(RetentionPolicy.RUNTIME)// 运行时
@Documented
public @interface MyRequestMapping {
    String value();
}
