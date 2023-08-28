package com.ytrue.job.core.glue;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ytrue
 * @date 2023-08-28 11:37
 * @description 规则类型枚举
 */
@Getter
@AllArgsConstructor
public enum GlueTypeEnum {
    //通常都是bean模式
    BEAN("BEAN", false, null, null),
    GLUE_GROOVY("GLUE(Java)", false, null, null),
    GLUE_SHELL("GLUE(Shell)", true, "bash", ".sh"),
    GLUE_PYTHON("GLUE(Python)", true, "python", ".py"),
    GLUE_PHP("GLUE(PHP)", true, "php", ".php"),
    GLUE_NODEJS("GLUE(Nodejs)", true, "node", ".js"),
    GLUE_POWERSHELL("GLUE(PowerShell)", true, "powershell", ".ps1");

    /**
     * 描述
     */
    private final String desc;

    /**
     * 是否脚本
     */
    private final boolean isScript;

    /**
     * xxx-cli php xxx.php or py xxx.py  or node xxx.js
     */
    private final String cmd;

    /**
     * 脚本后缀
     */
    private final String suffix;



    public static GlueTypeEnum match(String name){
        for (GlueTypeEnum item: GlueTypeEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return null;
    }
}
