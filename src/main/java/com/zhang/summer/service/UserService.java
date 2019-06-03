package com.zhang.summer.service;

import com.zhang.summer.annotation.MyService;

import java.util.HashMap;

/**
 * @Author zhangjiaheng
 * @Description 用户业务层
 **/
@MyService("userService")
public class UserService {

    private static HashMap<String, String> map = new HashMap<String, String>();

    static {
        map.put("jam0", "詹姆斯0");
        map.put("jam1", "詹姆斯1");
        map.put("jam2", "詹姆斯2");
        map.put("jam3", "詹姆斯3");
        map.put("jam4", "詹姆斯4");
        map.put("jam5", "詹姆斯5");
    }

    public String getUserName(String name) {
        return map.get(name);
    }
}
