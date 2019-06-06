package com.wshsoft.conf.admin.controller;

import com.wshsoft.conf.admin.controller.annotation.PermessionLimit;
import com.wshsoft.conf.admin.core.model.ServerConfProject;
import com.wshsoft.conf.admin.core.util.ReturnT;
import com.wshsoft.conf.admin.dao.ServerConfNodeDao;
import com.wshsoft.conf.admin.dao.ServerConfProjectDao;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * 项目管理
 *
 * @author Carry_xie 2016-10-02 20:52:56
 */
@Controller
@RequestMapping("/project")
public class ProjectController {
	
	@Resource
	private ServerConfProjectDao ServerConfProjectDao;
	@Resource
	private ServerConfNodeDao ServerConfNodeDao;

	@RequestMapping
	@PermessionLimit(adminuser = true)
	public String index(Model model) {

		List<ServerConfProject> list = ServerConfProjectDao.findAll();
		model.addAttribute("list", list);

		return "project/project.index";
	}

	@RequestMapping("/save")
	@PermessionLimit(adminuser = true)
	@ResponseBody
	public ReturnT<String> save(ServerConfProject ServerConfProject){

		// valid
		if (StringUtils.isBlank(ServerConfProject.getAppname())) {
			return new ReturnT<String>(500, "AppName不可为空");
		}
		if (ServerConfProject.getAppname().length()<3 || ServerConfProject.getAppname().length()>100) {
			return new ReturnT<String>(500, "Appname长度限制为3~100");
		}
		if (StringUtils.isBlank(ServerConfProject.getTitle())) {
			return new ReturnT<String>(500, "请输入项目名称");
		}

		// valid repeat
		ServerConfProject existProject = ServerConfProjectDao.load(ServerConfProject.getAppname());
		if (existProject != null) {
			return new ReturnT<String>(500, "Appname已存在，请勿重复添加");
		}

		int ret = ServerConfProjectDao.save(ServerConfProject);
		return (ret>0)?ReturnT.SUCCESS:ReturnT.FAIL;
	}

	@RequestMapping("/update")
	@PermessionLimit(adminuser = true)
	@ResponseBody
	public ReturnT<String> update(ServerConfProject ServerConfProject){

		// valid
		if (StringUtils.isBlank(ServerConfProject.getAppname())) {
			return new ReturnT<String>(500, "AppName不可为空");
		}
		if (StringUtils.isBlank(ServerConfProject.getTitle())) {
			return new ReturnT<String>(500, "请输入项目名称");
		}

		int ret = ServerConfProjectDao.update(ServerConfProject);
		return (ret>0)?ReturnT.SUCCESS:ReturnT.FAIL;
	}

	@RequestMapping("/remove")
	@PermessionLimit(adminuser = true)
	@ResponseBody
	public ReturnT<String> remove(String appname){

		if (StringUtils.isBlank(appname)) {
			return new ReturnT<String>(500, "参数AppName非法");
		}

		// valid
		int list_count = ServerConfNodeDao.pageListCount(0, 10, null, appname, null);
		if (list_count > 0) {
			return new ReturnT<String>(500, "拒绝删除，该项目下存在配置数据");
		}

		List<ServerConfProject> allList = ServerConfProjectDao.findAll();
		if (allList.size() == 1) {
			return new ReturnT<String>(500, "拒绝删除, 需要至少预留一个项目");
		}

		int ret = ServerConfProjectDao.delete(appname);
		return (ret>0)?ReturnT.SUCCESS:ReturnT.FAIL;
	}

}
