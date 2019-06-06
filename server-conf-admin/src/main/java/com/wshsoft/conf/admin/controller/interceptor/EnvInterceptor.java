package com.wshsoft.conf.admin.controller.interceptor;

import com.wshsoft.conf.admin.core.model.ServerConfEnv;
import com.wshsoft.conf.admin.core.util.CookieUtil;
import com.wshsoft.conf.admin.dao.ServerConfEnvDao;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * push cookies to model as cookieMap
 * @author Carry_xie 2015-12-12 18:09:04
 */
@Component
public class EnvInterceptor extends HandlerInterceptorAdapter {

	public static final String CURRENT_ENV = "SERVER_CONF_CURRENT_ENV";

	@Resource
	private ServerConfEnvDao ServerConfEnvDao;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

		// env list
		List<ServerConfEnv> envList = ServerConfEnvDao.findAll();
		if (envList==null || envList.size()==0) {
			throw new RuntimeException("系统异常，获取Env数据失败");
		}

		// current env
		String currentEnv = envList.get(0).getEnv();
		String currentEnvCookie = CookieUtil.getValue(request, CURRENT_ENV);
		if (currentEnvCookie!=null && currentEnvCookie.trim().length()>0) {
			for (ServerConfEnv envItem: envList) {
				if (currentEnvCookie.equals(envItem.getEnv())) {
					currentEnv = envItem.getEnv();
				}
			}
		}

		request.setAttribute("envList", envList);
		request.setAttribute(CURRENT_ENV, currentEnv);

		return super.preHandle(request, response, handler);
	}
	
}
