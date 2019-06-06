package com.wshsoft.conf.admin.service;


import com.wshsoft.conf.admin.core.model.ServerConfNode;
import com.wshsoft.conf.admin.core.model.ServerConfUser;
import com.wshsoft.conf.admin.core.util.ReturnT;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;

/**
 * @author Carry_xie 2015-9-4 18:19:52
 */
public interface IServerConfNodeService {

	public boolean ifHasProjectPermission(ServerConfUser loginUser, String loginEnv, String appname);

	public Map<String,Object> pageList(int offset,
									   int pagesize,
									   String appname,
									   String key,
									   ServerConfUser loginUser,
									   String loginEnv);

	public ReturnT<String> delete(String key, ServerConfUser loginUser, String loginEnv);

	public ReturnT<String> add(ServerConfNode ServerConfNode, ServerConfUser loginUser, String loginEnv);

	public ReturnT<String> update(ServerConfNode ServerConfNode, ServerConfUser loginUser, String loginEnv);

    /*ReturnT<String> syncConf(String appname, ServerConfUser loginUser, String loginEnv);*/


    // ---------------------- rest api ----------------------

    public ReturnT<Map<String, String>> find(String accessToken, String env, List<String> keys);

    public DeferredResult<ReturnT<String>> monitor(String accessToken, String env, List<String> keys);

}
