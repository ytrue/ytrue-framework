package com.ytrue.job.admin.service;

import com.ytrue.job.admin.core.model.XxlJobUser;
import com.ytrue.job.admin.core.util.CookieUtil;
import com.ytrue.job.admin.core.util.I18nUtil;
import com.ytrue.job.admin.core.util.JacksonUtil;
import com.ytrue.job.admin.dao.XxlJobUserDao;
import com.ytrue.job.core.biz.model.ReturnT;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;

/**
 * @author ytrue
 * @date 2023-08-30 9:28
 * @description 用户登陆的核心业务类
 */
@Configuration
public class LoginService {

    /**
     * 用户的登陆身份
     */
    public static final String LOGIN_IDENTITY_KEY = "XXL_JOB_LOGIN_IDENTITY";

    @Resource
    private XxlJobUserDao xxlJobUserDao;


    /**
     * 制作用户token
     *
     * @param xxlJobUser
     * @return
     */
    private String makeToken(XxlJobUser xxlJobUser) {
        //把用户个人的信息转化为字符串
        String tokenJson = JacksonUtil.writeValueAsString(xxlJobUser);
        //把当前字符串转换为16进制的字符串
        assert tokenJson != null;
        return new BigInteger(tokenJson.getBytes()).toString(16);
    }


    /**
     * 解析token
     *
     * @param tokenHex
     * @return
     */
    private XxlJobUser parseToken(String tokenHex) {
        XxlJobUser xxlJobUser = null;
        if (tokenHex != null) {
            String tokenJson = new String(new BigInteger(tokenHex, 16).toByteArray());
            xxlJobUser = JacksonUtil.readValue(tokenJson, XxlJobUser.class);
        }
        return xxlJobUser;
    }


    /**
     * 登陆功能
     *
     * @param request
     * @param response
     * @param username
     * @param password
     * @param ifRemember
     * @return
     */
    public ReturnT<String> login(HttpServletRequest request, HttpServletResponse response, String username, String password, boolean ifRemember) {
        if (username == null || username.trim().length() == 0 || password == null || password.trim().length() == 0) {
            // 账号或密码为空
            return new ReturnT<>(500, I18nUtil.getString("login_param_empty"));
        }
        XxlJobUser xxlJobUser = xxlJobUserDao.loadByUserName(username);
        if (xxlJobUser == null) {
            // 账号或密码错误
            return new ReturnT<>(500, I18nUtil.getString("login_param_unvalid"));
        }
        String passwordMd5 = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!passwordMd5.equals(xxlJobUser.getPassword())) {
            // 账号或密码错误
            return new ReturnT<>(500, I18nUtil.getString("login_param_unvalid"));
        }
        String loginToken = makeToken(xxlJobUser);
        CookieUtil.set(response, LOGIN_IDENTITY_KEY, loginToken, ifRemember);
        return ReturnT.SUCCESS;
    }


    /**
     * 登出
     *
     * @param request
     * @param response
     * @return
     */
    public ReturnT<String> logout(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.remove(request, response, LOGIN_IDENTITY_KEY);
        return ReturnT.SUCCESS;
    }


    /**
     * 是否登陆
     *
     * @param request
     * @param response
     * @return
     */
    public XxlJobUser ifLogin(HttpServletRequest request, HttpServletResponse response) {
        String cookieToken = CookieUtil.getValue(request, LOGIN_IDENTITY_KEY);
        if (cookieToken != null) {
            XxlJobUser cookieUser = null;
            try {
                cookieUser = parseToken(cookieToken);
            } catch (Exception e) {
                logout(request, response);
            }
            if (cookieUser != null) {
                XxlJobUser dbUser = xxlJobUserDao.loadByUserName(cookieUser.getUsername());
                if (dbUser != null) {
                    if (cookieUser.getPassword().equals(dbUser.getPassword())) {
                        return dbUser;
                    }
                }
            }
        }
        return null;
    }
}
