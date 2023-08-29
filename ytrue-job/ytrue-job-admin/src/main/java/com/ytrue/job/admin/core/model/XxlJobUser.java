package com.ytrue.job.admin.core.model;

import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * @author ytrue
 * @date 2023-08-29 9:44
 * @description 用户信息对应的实体类
 */
@Data
public class XxlJobUser {
    //用户id
    private int id;
    //用户名
    private String username;
    //密码
    private String password;
    //用户角色，0是普通用户，1是管理员
    private int role;
    //对应权限
    private String permission;


    /**
     * 判断当前用户有没有执行器权限的方法
     *
     * @param jobGroup
     * @return
     */
    public boolean validPermission(int jobGroup) {
        if (this.role == 1) {
            return true;
        } else {
            if (StringUtils.hasText(this.permission)) {
                for (String permissionItem : this.permission.split(",")) {
                    if (String.valueOf(jobGroup).equals(permissionItem)) {
                        return true;
                    }
                }
            }
            return false;
        }

    }
}
