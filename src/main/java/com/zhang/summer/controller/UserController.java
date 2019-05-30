package com.zhang.summer.controller;

import com.zhang.summer.MyAnnotation.MyAutowrite;
import com.zhang.summer.MyAnnotation.MyController;
import com.zhang.summer.MyAnnotation.MyRequestMapping;
import com.zhang.summer.MyAnnotation.MyRequestParam;
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
