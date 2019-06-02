package com.zhang.summer.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)// 只能使用在Java类成员上面
@Retention(RetentionPolicy.RUNTIME)// 运行时
@Documented
public @interface MyAutowrite {
    String value();
}
