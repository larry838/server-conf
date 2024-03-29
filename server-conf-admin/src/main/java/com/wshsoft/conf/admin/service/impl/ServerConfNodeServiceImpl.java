package com.wshsoft.conf.admin.service.impl;

import com.wshsoft.conf.admin.core.model.*;
import com.wshsoft.conf.admin.core.util.RegexUtil;
import com.wshsoft.conf.admin.core.util.ReturnT;
import com.wshsoft.conf.admin.dao.*;
import com.wshsoft.conf.admin.service.IServerConfNodeService;
import com.wshsoft.conf.core.util.PropUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 配置
 * @author Carry_xie 2016-08-15 22:53
 */
@Service
public class ServerConfNodeServiceImpl implements IServerConfNodeService, InitializingBean, DisposableBean {
	private static Logger logger = LoggerFactory.getLogger(ServerConfNodeServiceImpl.class);


	@Resource
	private ServerConfNodeDao ServerConfNodeDao;
	@Resource
	private ServerConfProjectDao ServerConfProjectDao;
	/*@Resource
	private ServerConfZKManager ServerConfZKManager;*/
	@Resource
	private ServerConfNodeLogDao ServerConfNodeLogDao;
	@Resource
	private ServerConfEnvDao ServerConfEnvDao;
	@Resource
	private ServerConfNodeMsgDao ServerConfNodeMsgDao;


	@Value("${server.conf.confdata.filepath}")
	private String confDataFilePath;
	@Value("${server.conf.access.token}")
	private String accessToken;

	private int confBeatTime = 30;


	@Override
	public boolean ifHasProjectPermission(ServerConfUser loginUser, String loginEnv, String appname){
		if (loginUser.getPermission() == 1) {
			return true;
		}
		if (ArrayUtils.contains(StringUtils.split(loginUser.getPermissionData(), ","), (appname.concat("#").concat(loginEnv)) )) {
			return true;
		}
		return false;
	}

	@Override
	public Map<String,Object> pageList(int offset,
									   int pagesize,
									   String appname,
									   String key,
									   ServerConfUser loginUser,
									   String loginEnv) {

		// project permission
		if (StringUtils.isBlank(loginEnv) || StringUtils.isBlank(appname) || !ifHasProjectPermission(loginUser, loginEnv, appname)) {
			//return new ReturnT<String>(500, "您没有该项目的配置权限,请联系管理员开通");
			Map<String, Object> emptyMap = new HashMap<String, Object>();
			emptyMap.put("data", new ArrayList<>());
			emptyMap.put("recordsTotal", 0);
			emptyMap.put("recordsFiltered", 0);
			return emptyMap;
		}

		// ServerConfNode in mysql
		List<ServerConfNode> data = ServerConfNodeDao.pageList(offset, pagesize, loginEnv, appname, key);
		int list_count = ServerConfNodeDao.pageListCount(offset, pagesize, loginEnv, appname, key);

		// fill value in zk
		/*if (CollectionUtils.isNotEmpty(data)) {
			for (ServerConfNode node: data) {
				String realNodeValue = ServerConfZKManager.get(node.getEnv(), node.getKey());
				node.setZkValue(realNodeValue);
			}
		}*/

		// package result
		Map<String, Object> maps = new HashMap<String, Object>();
		maps.put("data", data);
		maps.put("recordsTotal", list_count);		// 总记录数
		maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
		return maps;

	}

	@Override
	public ReturnT<String> delete(String key, ServerConfUser loginUser, String loginEnv) {
		if (StringUtils.isBlank(key)) {
			return new ReturnT<String>(500, "参数缺失");
		}
		ServerConfNode existNode = ServerConfNodeDao.load(loginEnv, key);
		if (existNode == null) {
			return new ReturnT<String>(500, "参数非法");
		}

		// project permission
		if (!ifHasProjectPermission(loginUser, loginEnv, existNode.getAppname())) {
			return new ReturnT<String>(500, "您没有该项目的配置权限,请联系管理员开通");
		}

		//ServerConfZKManager.delete(loginEnv, key);
		ServerConfNodeDao.delete(loginEnv, key);
		ServerConfNodeLogDao.deleteTimeout(loginEnv, key, 0);

		// conf msg
		sendConfMsg(loginEnv, key, null);

		return ReturnT.SUCCESS;
	}

	// conf broadcast msg
	private void sendConfMsg(String env, String key, String value){

		ServerConfNodeMsg confNodeMsg = new ServerConfNodeMsg();
		confNodeMsg.setEnv(env);
		confNodeMsg.setKey(key);
		confNodeMsg.setValue(value);

		ServerConfNodeMsgDao.add(confNodeMsg);
	}

	@Override
	public ReturnT<String> add(ServerConfNode ServerConfNode, ServerConfUser loginUser, String loginEnv) {

		// valid
		if (StringUtils.isBlank(ServerConfNode.getAppname())) {
			return new ReturnT<String>(500, "AppName不可为空");
		}

		// project permission
		if (!ifHasProjectPermission(loginUser, loginEnv, ServerConfNode.getAppname())) {
			return new ReturnT<String>(500, "您没有该项目的配置权限,请联系管理员开通");
		}

		// valid group
		ServerConfProject group = ServerConfProjectDao.load(ServerConfNode.getAppname());
		if (group==null) {
			return new ReturnT<String>(500, "AppName非法");
		}

		// valid env
		if (StringUtils.isBlank(ServerConfNode.getEnv())) {
			return new ReturnT<String>(500, "配置Env不可为空");
		}
		ServerConfEnv ServerConfEnv = ServerConfEnvDao.load(ServerConfNode.getEnv());
		if (ServerConfEnv == null) {
			return new ReturnT<String>(500, "配置Env非法");
		}

		// valid key
		if (StringUtils.isBlank(ServerConfNode.getKey())) {
			return new ReturnT<String>(500, "配置Key不可为空");
		}
		ServerConfNode.setKey(ServerConfNode.getKey().trim());

		ServerConfNode existNode = ServerConfNodeDao.load(ServerConfNode.getEnv(), ServerConfNode.getKey());
		if (existNode != null) {
			return new ReturnT<String>(500, "配置Key已存在，不可重复添加");
		}
		if (!ServerConfNode.getKey().startsWith(ServerConfNode.getAppname())) {
			return new ReturnT<String>(500, "配置Key格式非法");
		}

		// valid title
		if (StringUtils.isBlank(ServerConfNode.getTitle())) {
			return new ReturnT<String>(500, "配置描述不可为空");
		}

		// value force null to ""
		if (ServerConfNode.getValue() == null) {
			ServerConfNode.setValue("");
		}

		// add node
		//ServerConfZKManager.set(ServerConfNode.getEnv(), ServerConfNode.getKey(), ServerConfNode.getValue());
		ServerConfNodeDao.insert(ServerConfNode);

		// node log
		ServerConfNodeLog nodeLog = new ServerConfNodeLog();
		nodeLog.setEnv(ServerConfNode.getEnv());
		nodeLog.setKey(ServerConfNode.getKey());
		nodeLog.setTitle(ServerConfNode.getTitle() + "(配置新增)" );
		nodeLog.setValue(ServerConfNode.getValue());
		nodeLog.setOptuser(loginUser.getUsername());
		ServerConfNodeLogDao.add(nodeLog);

		// conf msg
		sendConfMsg(ServerConfNode.getEnv(), ServerConfNode.getKey(), ServerConfNode.getValue());

		return ReturnT.SUCCESS;
	}

	@Override
	public ReturnT<String> update(ServerConfNode ServerConfNode, ServerConfUser loginUser, String loginEnv) {

		// valid
		if (StringUtils.isBlank(ServerConfNode.getKey())) {
			return new ReturnT<String>(500, "配置Key不可为空");
		}
		ServerConfNode existNode = ServerConfNodeDao.load(ServerConfNode.getEnv(), ServerConfNode.getKey());
		if (existNode == null) {
			return new ReturnT<String>(500, "配置Key非法");
		}

		// project permission
		if (!ifHasProjectPermission(loginUser, loginEnv, existNode.getAppname())) {
			return new ReturnT<String>(500, "您没有该项目的配置权限,请联系管理员开通");
		}

		if (StringUtils.isBlank(ServerConfNode.getTitle())) {
			return new ReturnT<String>(500, "配置描述不可为空");
		}

		// value force null to ""
		if (ServerConfNode.getValue() == null) {
			ServerConfNode.setValue("");
		}

		// update conf
		//ServerConfZKManager.set(ServerConfNode.getEnv(), ServerConfNode.getKey(), ServerConfNode.getValue());

		existNode.setTitle(ServerConfNode.getTitle());
		existNode.setValue(ServerConfNode.getValue());
		int ret = ServerConfNodeDao.update(existNode);
		if (ret < 1) {
			return ReturnT.FAIL;
		}

		// node log
		ServerConfNodeLog nodeLog = new ServerConfNodeLog();
		nodeLog.setEnv(existNode.getEnv());
		nodeLog.setKey(existNode.getKey());
		nodeLog.setTitle(existNode.getTitle() + "(配置更新)" );
		nodeLog.setValue(existNode.getValue());
		nodeLog.setOptuser(loginUser.getUsername());
		ServerConfNodeLogDao.add(nodeLog);
		ServerConfNodeLogDao.deleteTimeout(existNode.getEnv(), existNode.getKey(), 10);

		// conf msg
		sendConfMsg(ServerConfNode.getEnv(), ServerConfNode.getKey(), ServerConfNode.getValue());

		return ReturnT.SUCCESS;
	}

	/*@Override
	public ReturnT<String> syncConf(String appname, ServerConfUser loginUser, String loginEnv) {

		// valid
		ServerConfEnv ServerConfEnv = ServerConfEnvDao.load(loginEnv);
		if (ServerConfEnv == null) {
			return new ReturnT<String>(500, "配置Env非法");
		}
		ServerConfProject group = ServerConfProjectDao.load(appname);
		if (group==null) {
			return new ReturnT<String>(500, "AppName非法");
		}

		// project permission
		if (!ifHasProjectPermission(loginUser, loginEnv, appname)) {
			return new ReturnT<String>(500, "您没有该项目的配置权限,请联系管理员开通");
		}

		List<ServerConfNode> confNodeList = ServerConfNodeDao.pageList(0, 10000, loginEnv, appname, null);
		if (CollectionUtils.isEmpty(confNodeList)) {
			return new ReturnT<String>(500, "操作失败，该项目下不存在配置项");
		}

		// un sync node
		List<ServerConfNode> unSyncConfNodeList = new ArrayList<>();
		for (ServerConfNode node: confNodeList) {
			String realNodeValue = ServerConfZKManager.get(node.getEnv(), node.getKey());
			if (!node.getValue().equals(realNodeValue)) {
				unSyncConfNodeList.add(node);
			}
		}

		if (CollectionUtils.isEmpty(unSyncConfNodeList)) {
			return new ReturnT<String>(500, "操作失败，该项目下不存未同步的配置项");
		}

		// do sync
		String logContent = "操作成功，共计同步 " + unSyncConfNodeList.size() + " 条配置：";
		for (ServerConfNode node: unSyncConfNodeList) {

			ServerConfZKManager.set(node.getEnv(), node.getKey(), node.getValue());

			// node log
			ServerConfNodeLog nodeLog = new ServerConfNodeLog();
			nodeLog.setEnv(node.getEnv());
			nodeLog.setKey(node.getKey());
			nodeLog.setTitle(node.getTitle() + "(全量同步)" );
			nodeLog.setValue(node.getValue());
			nodeLog.setOptuser(loginUser.getUsername());
			ServerConfNodeLogDao.add(nodeLog);
			ServerConfNodeLogDao.deleteTimeout(node.getEnv(), node.getKey(), 10);

			logContent += "<br>" + node.getKey();
		}
		logContent.substring(logContent.length() - 1);

		return new ReturnT<String>(ReturnT.SUCCESS.getCode(), logContent);
	}*/


	// ---------------------- rest api ----------------------

	@Override
	public ReturnT<Map<String, String>> find(String accessToken, String env, List<String> keys) {

		// valid
		if (this.accessToken!=null && this.accessToken.trim().length()>0 && !this.accessToken.equals(accessToken)) {
			return new ReturnT<Map<String, String>>(ReturnT.FAIL.getCode(), "AccessToken Invalid.");
		}
		if (env==null || env.trim().length()==0) {
			return new ReturnT<>(ReturnT.FAIL.getCode(), "env Invalid.");
		}
		if (keys==null || keys.size()==0) {
			return new ReturnT<>(ReturnT.FAIL.getCode(), "keys Invalid.");
		}
		/*for (String key: keys) {
			if (key==null || key.trim().length()<3 || key.trim().length()>100) {
				return new ReturnT<>(ReturnT.FAIL.getCode(), "Key Invalid[3~100]");
			}
			if (!RegexUtil.matches(RegexUtil.abc_number_line_point_pattern, key)) {
				return new ReturnT<>(ReturnT.FAIL.getCode(), "Key format Invalid");
			}
		}*/

		// result
		Map<String, String> result = new HashMap<String, String>();
		for (String key: keys) {

			// get val
			String value = null;
			if (key==null || key.trim().length()<4 || key.trim().length()>100
					|| !RegexUtil.matches(RegexUtil.abc_number_line_point_pattern, key) ) {
				// invalid key, pass
			} else {
				value = getFileConfData(env, key);
			}

			// parse null
			if (value == null) {
				value = "";
			}

			// put
			result.put(key, value);
		}

		return new ReturnT<Map<String, String>>(result);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public DeferredResult<ReturnT<String>> monitor(String accessToken, String env, List<String> keys) {

		// init
		DeferredResult deferredResult = new DeferredResult(confBeatTime * 1000L, new ReturnT<>(ReturnT.SUCCESS_CODE, "Monitor timeout, no key updated."));

		// valid
		if (this.accessToken!=null && this.accessToken.trim().length()>0 && !this.accessToken.equals(accessToken)) {
			deferredResult.setResult(new ReturnT<>(ReturnT.FAIL.getCode(), "AccessToken Invalid."));
			return deferredResult;
		}
		if (env==null || env.trim().length()==0) {
			deferredResult.setResult(new ReturnT<>(ReturnT.FAIL.getCode(), "env Invalid."));
			return deferredResult;
		}
		if (keys==null || keys.size()==0) {
			deferredResult.setResult(new ReturnT<>(ReturnT.FAIL.getCode(), "keys Invalid."));
			return deferredResult;
		}
		/*for (String key: keys) {
			if (key==null || key.trim().length()<3 || key.trim().length()>100) {
				deferredResult.setResult(new ReturnT<>(ReturnT.FAIL.getCode(), "Key Invalid[3~100]"));
				return deferredResult;
			}
			if (!RegexUtil.matches(RegexUtil.abc_number_line_point_pattern, key)) {
				deferredResult.setResult(new ReturnT<>(ReturnT.FAIL.getCode(), "Key format Invalid"));
				return deferredResult;
			}
		}*/

		// monitor by client
		for (String key: keys) {


			// invalid key, pass
			if (key==null || key.trim().length()<3 || key.trim().length()>100
					|| !RegexUtil.matches(RegexUtil.abc_number_line_point_pattern, key) ) {
				continue;
			}

			// monitor each key
			String fileName = parseConfDataFileName(env, key);

			List<DeferredResult> deferredResultList = confDeferredResultMap.get(fileName);
			if (deferredResultList == null) {
				deferredResultList = new ArrayList<>();
				confDeferredResultMap.put(fileName, deferredResultList);
			}

			deferredResultList.add(deferredResult);
		}

		return deferredResult;
	}


	// ---------------------- start stop ----------------------

	@Override
	public void afterPropertiesSet() throws Exception {
		startThead();
	}

	@Override
	public void destroy() throws Exception {
		stopThread();
	}


	// ---------------------- thread ----------------------

	private ExecutorService executorService = Executors.newCachedThreadPool();
	private volatile boolean executorStoped = false;

	private volatile List<Integer> readedMessageIds = Collections.synchronizedList(new ArrayList<Integer>());

	private Map<String, List<DeferredResult>> confDeferredResultMap = new ConcurrentHashMap<>();

	public void startThead() throws Exception {

		/**
		 * brocast conf-data msg, sync to file, for "add、update、delete"
		 */
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				while (!executorStoped) {
					try {
						// new message, filter readed
						List<ServerConfNodeMsg> messageList = ServerConfNodeMsgDao.findMsg(readedMessageIds);
						if (messageList!=null && messageList.size()>0) {
							for (ServerConfNodeMsg message: messageList) {
								readedMessageIds.add(message.getId());


								// sync file
								setFileConfData(message.getEnv(), message.getKey(), message.getValue());
							}
						}

						// clean old message;
						if ( (System.currentTimeMillis()/1000) % confBeatTime ==0) {
							ServerConfNodeMsgDao.cleanMessage(confBeatTime);
							readedMessageIds.clear();
						}
					} catch (Exception e) {
						if (!executorStoped) {
							logger.error(e.getMessage(), e);
						}
					}
					try {
						TimeUnit.SECONDS.sleep(1);
					} catch (Exception e) {
						if (!executorStoped) {
							logger.error(e.getMessage(), e);
						}
					}
				}
			}
		});


		/**
		 *  sync total conf-data, db + file      (1+N/30s)
		 *
		 *  clean deleted conf-data file
		 */
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				while (!executorStoped) {

					// align to beattime
					try {
						long sleepSecond = confBeatTime - (System.currentTimeMillis()/1000)%confBeatTime;
						if (sleepSecond>0 && sleepSecond<confBeatTime) {
							TimeUnit.SECONDS.sleep(sleepSecond);
						}
					} catch (Exception e) {
						if (!executorStoped) {
							logger.error(e.getMessage(), e);
						}
					}

					try {

						// sync registry-data, db + file
						int offset = 0;
						int pagesize = 1000;
						List<String> confDataFileList = new ArrayList<>();

						List<ServerConfNode> confNodeList = ServerConfNodeDao.pageList(offset, pagesize, null, null, null);
						while (confNodeList!=null && confNodeList.size()>0) {

							for (ServerConfNode confNoteItem: confNodeList) {

								// sync file
								String confDataFile = setFileConfData(confNoteItem.getEnv(), confNoteItem.getKey(), confNoteItem.getValue());

								// collect confDataFile
								confDataFileList.add(confDataFile);
							}


							offset += 1000;
							confNodeList = ServerConfNodeDao.pageList(offset, pagesize, null, null, null);
						}

						// clean old registry-data file
						cleanFileConfData(confDataFileList);

                        logger.debug(">>>>>>>>>>> server-conf, sync totel conf data success, sync conf count = {}", confDataFileList.size());
					} catch (Exception e) {
						if (!executorStoped) {
							logger.error(e.getMessage(), e);
						}
					}
					try {
						TimeUnit.SECONDS.sleep(confBeatTime);
					} catch (Exception e) {
						if (!executorStoped) {
							logger.error(e.getMessage(), e);
						}
					}
				}
			}
		});



	}

	private void stopThread(){
		executorStoped = true;
		executorService.shutdownNow();
	}


	// ---------------------- file opt ----------------------

	// get
	public String getFileConfData(String env, String key){

		// fileName
		String confFileName = parseConfDataFileName(env, key);

		// read
		Properties existProp = PropUtil.loadFileProp(confFileName);
		if (existProp!=null && existProp.containsKey("value")) {
			return existProp.getProperty("value");
		}
		return null;
	}

	private String parseConfDataFileName(String env, String key){
		// fileName
		String fileName = confDataFilePath
				.concat(File.separator).concat(env)
				.concat(File.separator).concat(key)
				.concat(".properties");
		return fileName;
	}

	// set
	private String setFileConfData(String env, String key, String value){

		// fileName
		String confFileName = parseConfDataFileName(env, key);

		// valid repeat update
		Properties existProp = PropUtil.loadFileProp(confFileName);
		if (existProp != null
				&& value!=null
				&& value.equals(existProp.getProperty("value"))
				) {
			return new File(confFileName).getPath();
		}

		// write
		Properties prop = new Properties();
		if (value == null) {
			prop.setProperty("value-deleted", "true");
		} else {
			prop.setProperty("value", value);
		}

		PropUtil.writeFileProp(prop, confFileName);
		logger.info(">>>>>>>>>>> server-conf, setFileConfData: confFileName={}, value={}", confFileName, value);

		// brocast monitor client
		List<DeferredResult> deferredResultList = confDeferredResultMap.get(confFileName);
		if (deferredResultList != null) {
			confDeferredResultMap.remove(confFileName);
			for (DeferredResult deferredResult: deferredResultList) {
				deferredResult.setResult(new ReturnT<>(ReturnT.SUCCESS_CODE, "Monitor key update."));
			}
		}

		return new File(confFileName).getPath();
	}

	// clean
	public void cleanFileConfData(List<String> confDataFileList){
		filterChildPath(new File(confDataFilePath), confDataFileList);
	}

	public void filterChildPath(File parentPath, final List<String> confDataFileList){
		if (!parentPath.exists() || parentPath.list()==null || parentPath.list().length==0) {
			return;
		}
		File[] childFileList = parentPath.listFiles();
		for (File childFile: childFileList) {
			if (childFile.isFile() && !confDataFileList.contains(childFile.getPath())) {
				childFile.delete();

				logger.info(">>>>>>>>>>> server-conf, cleanFileConfData, ConfDataFile={}", childFile.getPath());
			}
			if (childFile.isDirectory()) {
				if (parentPath.listFiles()!=null && parentPath.listFiles().length>0) {
					filterChildPath(childFile, confDataFileList);
				} else {
					childFile.delete();
				}

			}
		}

	}

}
