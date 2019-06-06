package com.wshsoft.conf.admin.controller;

import com.wshsoft.conf.admin.controller.annotation.PermessionLimit;
import com.wshsoft.conf.admin.core.model.ServerConfEnv;
import com.wshsoft.conf.admin.core.model.ServerConfProject;
import com.wshsoft.conf.admin.core.model.ServerConfUser;
import com.wshsoft.conf.admin.core.util.JacksonUtil;
import com.wshsoft.conf.admin.core.util.ReturnT;
import com.wshsoft.conf.admin.dao.ServerConfEnvDao;
import com.wshsoft.conf.admin.dao.ServerConfProjectDao;
import com.wshsoft.conf.admin.dao.ServerConfUserDao;
import com.wshsoft.conf.admin.service.impl.LoginService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Carry_xie 2018-03-01
 */
@Controller
@RequestMapping("/user")
public class UserController {

    @Resource
    private ServerConfUserDao ServerConfUserDao;
    @Resource
    private ServerConfProjectDao ServerConfProjectDao;
    @Resource
    private ServerConfEnvDao ServerConfEnvDao;

    @RequestMapping("")
    @PermessionLimit(adminuser = true)
    public String index(Model model){

        List<ServerConfProject> projectList = ServerConfProjectDao.findAll();
        model.addAttribute("projectList", projectList);

        List<ServerConfEnv> envList = ServerConfEnvDao.findAll();
        model.addAttribute("envList", envList);

        return "user/user.index";
    }

    @RequestMapping("/pageList")
    @PermessionLimit(adminuser = true)
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        String username,
                                        int permission) {

        // ServerConfNode in mysql
        List<ServerConfUser> data = ServerConfUserDao.pageList(start, length, username, permission);
        int list_count = ServerConfUserDao.pageListCount(start, length, username, permission);

        // package result
        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("data", data);
        maps.put("recordsTotal", list_count);		// 总记录数
        maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
        return maps;
    }

    /**
     * add
     *
     * @return
     */
    @RequestMapping("/add")
    @PermessionLimit(adminuser = true)
    @ResponseBody
    public ReturnT<String> add(ServerConfUser ServerConfUser){

        // valid
        if (StringUtils.isBlank(ServerConfUser.getUsername())){
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "用户名不可为空");
        }
        if (StringUtils.isBlank(ServerConfUser.getPassword())){
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "密码不可为空");
        }
        if (!(ServerConfUser.getPassword().length()>=4 && ServerConfUser.getPassword().length()<=100)) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "密码长度限制为4~50");
        }

        // passowrd md5
        String md5Password = DigestUtils.md5DigestAsHex(ServerConfUser.getPassword().getBytes());
        ServerConfUser.setPassword(md5Password);

        int ret = ServerConfUserDao.add(ServerConfUser);
        return ret>0? ReturnT.SUCCESS: ReturnT.FAIL;
    }

    /**
     * delete
     *
     * @return
     */
    @RequestMapping("/delete")
    @PermessionLimit(adminuser = true)
    @ResponseBody
    public ReturnT<String> delete(HttpServletRequest request, String username){

        ServerConfUser loginUser = (ServerConfUser) request.getAttribute(LoginService.LOGIN_IDENTITY);
        if (loginUser.getUsername().equals(username)) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "禁止操作当前登录账号");
        }

        /*List<ServerConfUser> adminList = ServerConfUserDao.pageList(0, 1 , null, 1);
        if (adminList.size()<2) {

        }*/

        ServerConfUserDao.delete(username);
        return ReturnT.SUCCESS;
    }

    /**
     * update
     *
     * @return
     */
    @RequestMapping("/update")
    @PermessionLimit(adminuser = true)
    @ResponseBody
    public ReturnT<String> update(HttpServletRequest request, ServerConfUser ServerConfUser){

        ServerConfUser loginUser = (ServerConfUser) request.getAttribute(LoginService.LOGIN_IDENTITY);
        if (loginUser.getUsername().equals(ServerConfUser.getUsername())) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "禁止操作当前登录账号");
        }

        // valid
        if (StringUtils.isBlank(ServerConfUser.getUsername())){
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "用户名不可为空");
        }

        ServerConfUser existUser = ServerConfUserDao.load(ServerConfUser.getUsername());
        if (existUser == null) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "用户名非法");
        }

        if (StringUtils.isNotBlank(ServerConfUser.getPassword())) {
            if (!(ServerConfUser.getPassword().length()>=4 && ServerConfUser.getPassword().length()<=50)) {
                return new ReturnT<String>(ReturnT.FAIL.getCode(), "密码长度限制为4~50");
            }
            // passowrd md5
            String md5Password = DigestUtils.md5DigestAsHex(ServerConfUser.getPassword().getBytes());
            existUser.setPassword(md5Password);
        }
        existUser.setPermission(ServerConfUser.getPermission());

        int ret = ServerConfUserDao.update(existUser);
        return ret>0? ReturnT.SUCCESS: ReturnT.FAIL;
    }

    @RequestMapping("/updatePermissionData")
    @PermessionLimit(adminuser = true)
    @ResponseBody
    public ReturnT<String> updatePermissionData(HttpServletRequest request,
                                                    String username,
                                                    @RequestParam(required = false) String[] permissionData){

        ServerConfUser existUser = ServerConfUserDao.load(username);
        if (existUser == null) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "参数非法");
        }

        String permissionDataArrStr = permissionData!=null?StringUtils.join(permissionData, ","):"";
        existUser.setPermissionData(permissionDataArrStr);
        ServerConfUserDao.update(existUser);

        return ReturnT.SUCCESS;
    }

    @RequestMapping("/updatePwd")
    @ResponseBody
    public ReturnT<String> updatePwd(HttpServletRequest request, String oldpassword,String password){
    	if (StringUtils.isBlank(oldpassword)){
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "旧密码不可为空");
        }
      
        ServerConfUser loginUser = (ServerConfUser) request.getAttribute(LoginService.LOGIN_IDENTITY);

        ServerConfUser existUser = ServerConfUserDao.load(loginUser.getUsername());
        if (existUser==null){
        	 return new ReturnT<String>(ReturnT.FAIL.getCode(), "系统异常");
        }
        if(!DigestUtils.md5DigestAsHex(oldpassword.getBytes()).equals(existUser.getPassword())){
        	return new ReturnT<String>(ReturnT.FAIL.getCode(), "旧密码不正确");
        }
        // new password(md5)
        if (StringUtils.isBlank(password)){
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "新密码不可为空");
        }
        if (!(password.length()>=6 && password.length()<=100)) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), "密码长度限制为6~50");
        }
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

        // update pwd
        existUser.setPassword(md5Password);
        ServerConfUserDao.update(existUser);

        return ReturnT.SUCCESS;
    }

}
