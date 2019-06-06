package com.wshsoft.conf.admin.service.impl;
//package com.wshsoft.conf.admin.service.impl;
//
//import com.wshsoft.conf.core.core.ServerConfZkManageConf;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.DisposableBean;
//import org.springframework.beans.factory.InitializingBean;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//
///**
// * ZooKeeper cfg client (Watcher + some utils)
// *
// * @author Carry_xie 2015年8月26日21:36:43
// */
//@Component
//public class ServerConfZKManager implements InitializingBean, DisposableBean {
//	private static Logger logger = LoggerFactory.getLogger(ServerConfZKManager.class);
//
//	@Value("${server.conf.zkaddress:''}")
//	private String zkaddress;
//
//	@Value("${server.conf.zkdigest:''}")
//	private String zkdigest;
//
//	private static boolean open = false;
//	public static boolean isOpen() {
//		return open;
//	}
//
//	// ------------------------------ zookeeper client ------------------------------
//
//	@Override
//	public void afterPropertiesSet() throws Exception {
//		if (zkaddress==null || zkaddress.trim().length()==0) {
//			open = false;
//			return;
//		}
//		ServerConfZkManageConf.init(zkaddress, zkdigest);
//	}
//
//	@Override
//	public void destroy() throws Exception {
//		if (!open) {
//			return;
//		}
//
//		ServerConfZkManageConf.destroy();
//	}
//
//
//	// ------------------------------ conf opt ------------------------------
//
//	/**
//	 * set zk conf
//	 *
//	 * @param key
//	 * @param data
//	 * @return
//	 */
//	public void set(String env, String key, String data) {
//		if (!open) {
//			return;
//		}
//
//		ServerConfZkManageConf.set(env, key, data);
//	}
//
//	/**
//	 * delete zk conf
//	 *
//	 * @param env
//	 * @param key
//	 */
//	public void delete(String env, String key){
//		if (!open) {
//			return;
//		}
//
//		ServerConfZkManageConf.delete(env, key);
//	}
//
//	/**
//	 * get zk conf
//	 *
//	 * @param key
//	 * @return
//	 */
//	public String get(String env, String key){
//		if (!open) {
//			return null;
//		}
//
//		return ServerConfZkManageConf.get(env, key);
//	}
//
//}