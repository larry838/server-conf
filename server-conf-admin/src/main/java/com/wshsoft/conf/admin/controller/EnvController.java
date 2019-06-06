package com.wshsoft.conf.admin.controller;

import com.wshsoft.conf.admin.controller.annotation.PermessionLimit;
import com.wshsoft.conf.admin.core.model.ServerConfEnv;
import com.wshsoft.conf.admin.core.util.ReturnT;
import com.wshsoft.conf.admin.dao.ServerConfEnvDao;
import com.wshsoft.conf.admin.dao.ServerConfNodeDao;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * 环境管理
 *
 * @author Carry_xie 2018-05-30
 */
@Controller
@RequestMapping("/env")
public class EnvController {
	
	@Resource
	private ServerConfEnvDao ServerConfEnvDao;
    @Resource
    private ServerConfNodeDao ServerConfNodeDao;


	@RequestMapping
	@PermessionLimit(adminuser = true)
	public String index(Model model) {

		List<ServerConfEnv> list = ServerConfEnvDao.findAll();
		model.addAttribute("list", list);

		return "env/env.index";
	}

	@RequestMapping("/save")
	@PermessionLimit(adminuser = true)
	@ResponseBody
	public ReturnT<String> save(ServerConfEnv ServerConfEnv){

		// valid
		if (StringUtils.isBlank(ServerConfEnv.getEnv())) {
			return new ReturnT<String>(500, "Env不可为空");
		}
		if (ServerConfEnv.getEnv().length()<3 || ServerConfEnv.getEnv().length()>50) {
			return new ReturnT<String>(500, "Env长度限制为4~50");
		}
		if (StringUtils.isBlank(ServerConfEnv.getTitle())) {
			return new ReturnT<String>(500, "请输入Env名称");
		}

		// valid repeat
		ServerConfEnv existEnv = ServerConfEnvDao.load(ServerConfEnv.getEnv());
		if (existEnv != null) {
			return new ReturnT<String>(500, "Env已存在，请勿重复添加");
		}

		int ret = ServerConfEnvDao.save(ServerConfEnv);
		return (ret>0)?ReturnT.SUCCESS:ReturnT.FAIL;
	}

	@RequestMapping("/update")
	@PermessionLimit(adminuser = true)
	@ResponseBody
	public ReturnT<String> update(ServerConfEnv ServerConfEnv){

		// valid
		if (StringUtils.isBlank(ServerConfEnv.getEnv())) {
			return new ReturnT<String>(500, "Env不可为空");
		}
		if (StringUtils.isBlank(ServerConfEnv.getTitle())) {
			return new ReturnT<String>(500, "请输入Env名称");
		}

		int ret = ServerConfEnvDao.update(ServerConfEnv);
		return (ret>0)?ReturnT.SUCCESS:ReturnT.FAIL;
	}

	@RequestMapping("/remove")
	@PermessionLimit(adminuser = true)
	@ResponseBody
	public ReturnT<String> remove(String env){

		if (StringUtils.isBlank(env)) {
			return new ReturnT<String>(500, "参数Env非法");
		}

        // valid
        int list_count = ServerConfNodeDao.pageListCount(0, 10, env, null, null);
        if (list_count > 0) {
            return new ReturnT<String>(500, "拒绝删除，该Env下存在配置数据");
        }

		// valid can not be empty
		List<ServerConfEnv> allList = ServerConfEnvDao.findAll();
		if (allList.size() == 1) {
			return new ReturnT<String>(500, "拒绝删除, 需要至少预留一个Env");
		}

		int ret = ServerConfEnvDao.delete(env);
		return (ret>0)?ReturnT.SUCCESS:ReturnT.FAIL;
	}

}
