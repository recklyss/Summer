package com.zhang.summer.service;

import com.zhang.summer.MyAnnotation.MyService;

/**
 * @Author zhangjiaheng
 * @Description 用户业务层
 **/
@MyService("userService")
public class UserService {

    public String getUserName(){
        return "我叫詹姆斯";
    }
}
