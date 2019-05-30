package com.zhang.summer.MyAnnotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})// 只能使用在方法参数上面
@Retention(RetentionPolicy.RUNTIME)// 运行时
@Documented
public @interface MyRequestParam {
    String value();
}
