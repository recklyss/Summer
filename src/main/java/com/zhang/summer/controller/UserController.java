package com.zhang.summer.controller;

import com.zhang.summer.annotation.MyAutowrite;
import com.zhang.summer.annotation.MyController;
import com.zhang.summer.annotation.MyRequestMapping;
import com.zhang.summer.annotation.MyRequestParam;
import com.zhang.summer.service.UserService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author zhangjiaheng
 * @Description 业务方法控制器 -- 用户
 **/
@MyController
@MyRequestMapping("/query")
public class UserController {

    @MyAutowrite("userService")
    private UserService userService;

    @MyRequestMapping("/user")
    public String queryUser(HttpServletRequest request,
                            HttpServletResponse response,
                            @MyRequestParam("username") String name){

        return userService.getUserName();
    }
}
