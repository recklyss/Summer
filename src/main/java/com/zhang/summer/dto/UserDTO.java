package com.zhang.summer.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author zhangjiaheng
 * @Description
 **/
@Getter
@Setter
public class UserDTO {
    private String username;
    private Integer userage;

    public UserDTO(String username, Integer userage) {
        this.username = username;
        this.userage = userage;
    }
}
