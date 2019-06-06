package com.wshsoft.conf.core.factory;

import com.wshsoft.conf.core.core.ServerConfLocalCacheConf;
import com.wshsoft.conf.core.core.ServerConfMirrorConf;
import com.wshsoft.conf.core.core.ServerConfRemoteConf;
import com.wshsoft.conf.core.listener.ServerConfListenerFactory;
import com.wshsoft.conf.core.listener.impl.BeanRefreshServerConfListener;

/**
 * ServerConf Base Factory
 *
 * @author Carry_xie 2015-9-12 19:42:49
 */
public class ServerConfBaseFactory {


	/**
	 * init
	 *
	 * @param adminAddress
	 * @param env
	 */
	public static void init(String adminAddress, String env, String accessToken, String mirrorfile) {
		// init
		ServerConfRemoteConf.init(adminAddress, env, accessToken);	// init remote util
		ServerConfMirrorConf.init(mirrorfile);			// init mirror util
		ServerConfLocalCacheConf.init();				// init cache + thread, cycle refresh + monitor

		ServerConfListenerFactory.addListener(null, new BeanRefreshServerConfListener());    // listener all key change

	}

	/**
	 * destory
	 */
	public static void destroy() {
		ServerConfLocalCacheConf.destroy();	// destroy
	}

}
