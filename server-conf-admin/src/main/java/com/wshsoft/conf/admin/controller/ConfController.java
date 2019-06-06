package com.wshsoft.conf.admin.controller;

import com.wshsoft.conf.admin.controller.annotation.PermessionLimit;
import com.wshsoft.conf.admin.core.model.ServerConfNode;
import com.wshsoft.conf.admin.core.model.ServerConfProject;
import com.wshsoft.conf.admin.core.model.ServerConfUser;
import com.wshsoft.conf.admin.core.util.JacksonUtil;
import com.wshsoft.conf.admin.core.util.ReturnT;
import com.wshsoft.conf.admin.dao.ServerConfProjectDao;
import com.wshsoft.conf.admin.service.IServerConfNodeService;
import com.wshsoft.conf.admin.service.impl.LoginService;
import com.wshsoft.conf.core.model.ServerConfParamVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static com.wshsoft.conf.admin.controller.interceptor.EnvInterceptor.CURRENT_ENV;

/**
 * 配置管理
 *
 * @author Carry_xie
 */
@Controller
@RequestMapping("/conf")
public class ConfController {

	@Resource
	private ServerConfProjectDao ServerConfProjectDao;
	@Resource
	private IServerConfNodeService ServerConfNodeService;

	@RequestMapping("")
	public String index(HttpServletRequest request, Model model, String appname){

		List<ServerConfProject> list = ServerConfProjectDao.findAll();
		if (list==null || list.size()==0) {
			throw new RuntimeException("系统异常，无可用项目");
		}

		ServerConfProject project = list.get(0);
		for (ServerConfProject item: list) {
			if (item.getAppname().equals(appname)) {
				project = item;
			}
		}

		boolean ifHasProjectPermission = ServerConfNodeService.ifHasProjectPermission(
				(ServerConfUser) request.getAttribute(LoginService.LOGIN_IDENTITY),
				(String) request.getAttribute(CURRENT_ENV),
				project.getAppname());

		model.addAttribute("ProjectList", list);
		model.addAttribute("project", project);
		model.addAttribute("ifHasProjectPermission", ifHasProjectPermission);

		return "conf/conf.index";
	}

	@RequestMapping("/pageList")
	@ResponseBody
	public Map<String, Object> pageList(HttpServletRequest request,
										@RequestParam(required = false, defaultValue = "0") int start,
										@RequestParam(required = false, defaultValue = "10") int length,
										String appname,
										String key) {

		ServerConfUser ServerConfUser = (ServerConfUser) request.getAttribute(LoginService.LOGIN_IDENTITY);
		String loginEnv = (String) request.getAttribute(CURRENT_ENV);

		return ServerConfNodeService.pageList(start, length, appname, key, ServerConfUser, loginEnv);
	}

	/**
	 * get
	 * @return
	 */
	@RequestMapping("/delete")
	@ResponseBody
	public ReturnT<String> delete(HttpServletRequest request, String key){

		ServerConfUser ServerConfUser = (ServerConfUser) request.getAttribute(LoginService.LOGIN_IDENTITY);
		String loginEnv = (String) request.getAttribute(CURRENT_ENV);

		return ServerConfNodeService.delete(key, ServerConfUser, loginEnv);
	}

	/**
	 * create/update
	 * @return
	 */
	@RequestMapping("/add")
	@ResponseBody
	public ReturnT<String> add(HttpServletRequest request, ServerConfNode ServerConfNode){

		ServerConfUser ServerConfUser = (ServerConfUser) request.getAttribute(LoginService.LOGIN_IDENTITY);
		String loginEnv = (String) request.getAttribute(CURRENT_ENV);

		// fill env
		ServerConfNode.setEnv(loginEnv);

		return ServerConfNodeService.add(ServerConfNode, ServerConfUser, loginEnv);
	}
	
	/**
	 * create/update
	 * @return
	 */
	@RequestMapping("/update")
	@ResponseBody
	public ReturnT<String> update(HttpServletRequest request, ServerConfNode ServerConfNode){

		ServerConfUser ServerConfUser = (ServerConfUser) request.getAttribute(LoginService.LOGIN_IDENTITY);
		String loginEnv = (String) request.getAttribute(CURRENT_ENV);

		// fill env
		ServerConfNode.setEnv(loginEnv);

		return ServerConfNodeService.update(ServerConfNode, ServerConfUser, loginEnv);
	}

	/*@RequestMapping("/syncConf")
	@ResponseBody
	public ReturnT<String> syncConf(HttpServletRequest request,
										String appname) {

		ServerConfUser ServerConfUser = (ServerConfUser) request.getAttribute(LoginService.LOGIN_IDENTITY);
		String loginEnv = (String) request.getAttribute(CURRENT_ENV);

		return ServerConfNodeService.syncConf(appname, ServerConfUser, loginEnv);
	}*/


	// ---------------------- rest api ----------------------

    @Value("${server.conf.access.token}")
    private String accessToken;


	/**
	 * 配置查询 API
	 *
	 * 说明：查询配置数据；
	 *
	 * ------
	 * 地址格式：{配置中心跟地址}/find
	 *
	 * 请求参数说明：
	 *  1、accessToken：请求令牌；
	 *  2、env：环境标识
	 *  3、keys：配置Key列表
	 *
	 * 请求数据格式如下，放置在 RequestBody 中，JSON格式：
	 *
	 *     {
	 *         "accessToken" : "xx",
	 *         "env" : "xx",
	 *         "keys" : [
	 *             "key01",
	 *             "key02"
	 *         ]
	 *     }
	 *
	 * @param data
	 * @return
	 */
	@RequestMapping("/find")
	@ResponseBody
	@PermessionLimit(limit = false)
	public ReturnT<Map<String, String>> find(@RequestBody(required = false) String data){

		// parse data
		ServerConfParamVO confParamVO = null;
		try {
			confParamVO = (ServerConfParamVO) JacksonUtil.readValue(data, ServerConfParamVO.class);
		} catch (Exception e) { }

		// parse param
		String accessToken = null;
		String env = null;
		List<String> keys = null;
		if (confParamVO != null) {
			accessToken = confParamVO.getAccessToken();
			env = confParamVO.getEnv();
			keys = confParamVO.getKeys();
		}

		return ServerConfNodeService.find(accessToken, env, keys);
	}

	/**
	 * 配置监控 API
	 *
	 * 说明：long-polling 接口，主动阻塞一段时间（默认30s）；直至阻塞超时或配置信息变动时响应；
	 *
	 * ------
	 * 地址格式：{配置中心跟地址}/monitor
	 *
	 * 请求参数说明：
	 *  1、accessToken：请求令牌；
	 *  2、env：环境标识
	 *  3、keys：配置Key列表
	 *
	 * 请求数据格式如下，放置在 RequestBody 中，JSON格式：
	 *
	 *     {
	 *         "accessToken" : "xx",
	 *         "env" : "xx",
	 *         "keys" : [
	 *             "key01",
	 *             "key02"
	 *         ]
	 *     }
	 *
	 * @param data
	 * @return
	 */
	@RequestMapping("/monitor")
	@ResponseBody
	@PermessionLimit(limit = false)
	public DeferredResult<ReturnT<String>> monitor(@RequestBody(required = false) String data){

		// parse data
		ServerConfParamVO confParamVO = null;
		try {
			confParamVO = (ServerConfParamVO) JacksonUtil.readValue(data, ServerConfParamVO.class);
		} catch (Exception e) { }

		// parse param
		String accessToken = null;
		String env = null;
		List<String> keys = null;
		if (confParamVO != null) {
			accessToken = confParamVO.getAccessToken();
			env = confParamVO.getEnv();
			keys = confParamVO.getKeys();
		}

		return ServerConfNodeService.monitor(accessToken, env, keys);
	}


}
