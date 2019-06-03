package com.zhang.summer.service;

import com.zhang.summer.annotation.MyService;
import com.zhang.summer.dto.UserDTO;

import java.util.HashMap;

/**
 * @Author zhangjiaheng
 * @Description 用户业务层
 **/
@MyService("userService")
public class UserService {

    private static HashMap<String, UserDTO> map = new HashMap<String, UserDTO>();

    static {
        map.put("jam0", new UserDTO("J1", 12));
        map.put("jam1", new UserDTO("J2", 13));
        map.put("jam2", new UserDTO("J3", 14));
        map.put("jam3", new UserDTO("J4", 15));
        map.put("jam4", new UserDTO("J5", 16));
        map.put("jam5", new UserDTO("J6", 17));
    }

    public UserDTO getUserName(String name) {
        return map.get(name);
    }
}
