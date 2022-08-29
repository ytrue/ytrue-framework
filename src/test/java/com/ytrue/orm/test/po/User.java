package com.ytrue.orm.test.po;

import lombok.Data;

import java.util.Date;

@Data
public class User {

    private Long id;
    // 用户ID
    private String userId;
    // 用户名称
    private String userName;
    // 头像
    private String userHead;
    // 创建时间
    private Date createTime;
    // 更新时间
    private Date updateTime;


    public User() {
    }

    public User(Long id) {
        this.id = id;
    }

    public User(Long id, String userId) {
        this.id = id;
        this.userId = userId;
    }

    public User(Long id, String userId, String userName) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
    }
}
