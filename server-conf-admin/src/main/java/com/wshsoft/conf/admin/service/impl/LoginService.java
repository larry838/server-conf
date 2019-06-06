package com.wshsoft.conf.admin.service.impl;

import com.wshsoft.conf.admin.core.model.ServerConfUser;
import com.wshsoft.conf.admin.core.util.CookieUtil;
import com.wshsoft.conf.admin.core.util.JacksonUtil;
import com.wshsoft.conf.admin.core.util.ReturnT;
import com.wshsoft.conf.admin.dao.ServerConfUserDao;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;

/**
 * Login Service
 *
 * @author Carry_xie 2018-02-04 03:25:55
 */
@Configuration
public class LoginService {

    public static final String LOGIN_IDENTITY = "SERVER_CONF_LOGIN_IDENTITY";

    @Resource
    private ServerConfUserDao ServerConfUserDao;

    private String makeToken(ServerConfUser ServerConfUser){
        String tokenJson = JacksonUtil.writeValueAsString(ServerConfUser);
        String tokenHex = new BigInteger(tokenJson.getBytes()).toString(16);
        return tokenHex;
    }
    private ServerConfUser parseToken(String tokenHex){
        ServerConfUser ServerConfUser = null;
        if (tokenHex != null) {
            String tokenJson = new String(new BigInteger(tokenHex, 16).toByteArray());      // username_password(md5)
            ServerConfUser = JacksonUtil.readValue(tokenJson, ServerConfUser.class);
        }
        return ServerConfUser;
    }

    /**
     * login
     *
     * @param response
     * @param usernameParam
     * @param passwordParam
     * @param ifRemember
     * @return
     */
    public ReturnT<String> login(HttpServletResponse response, String usernameParam, String passwordParam, boolean ifRemember){

        ServerConfUser ServerConfUser = ServerConfUserDao.load(usernameParam);
        if (ServerConfUser == null) {
            return new ReturnT<String>(500, "账号或密码错误");
        }

        String passwordParamMd5 = DigestUtils.md5DigestAsHex(passwordParam.getBytes());
        if (!ServerConfUser.getPassword().equals(passwordParamMd5)) {
            return new ReturnT<String>(500, "账号或密码错误");
        }

        String loginToken = makeToken(ServerConfUser);

        // do login
        CookieUtil.set(response, LOGIN_IDENTITY, loginToken, ifRemember);
        return ReturnT.SUCCESS;
    }

    /**
     * logout
     *
     * @param request
     * @param response
     */
    public void logout(HttpServletRequest request, HttpServletResponse response){
        CookieUtil.remove(request, response, LOGIN_IDENTITY);
    }

    /**
     * logout
     *
     * @param request
     * @return
     */
    public ServerConfUser ifLogin(HttpServletRequest request){
        String cookieToken = CookieUtil.getValue(request, LOGIN_IDENTITY);
        if (cookieToken != null) {
            ServerConfUser cookieUser = parseToken(cookieToken);
            if (cookieUser != null) {
                ServerConfUser dbUser = ServerConfUserDao.load(cookieUser.getUsername());
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
