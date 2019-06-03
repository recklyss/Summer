package com.zhang.summer.controller;

import com.zhang.summer.annotation.*;
import com.zhang.summer.dto.UserDTO;
import com.zhang.summer.service.UserService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author zhangjiaheng
 * @Description 业务方法控制器 -- 用户
 **/
@MyController
@MyRequestMapping("/userController")
public class UserController {

    @MyAutowrite
    private UserService userService;

    @MyRequestMapping("/queryUser")
    @MyResponsebody
    public UserDTO queryUser(HttpServletRequest request,
                             HttpServletResponse response,
                             @MyRequestParam("username") String name){

        return userService.getUserName(name);
    }

    @MyRequestMapping("/request")
    public String AmIRequestedSuccess(){
        return "success";
    }
}
