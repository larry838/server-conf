package com.wshsoft.conf.core.core;

import com.wshsoft.conf.core.ServerConfClient;
import com.wshsoft.conf.core.listener.ServerConfListenerFactory;
import com.wshsoft.conf.core.util.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * local cache conf
 *
 * @author Carry_xie 2018-02-01 19:11:25
 */
public class ServerConfLocalCacheConf {
    private static Logger logger = LoggerFactory.getLogger(ServerConfClient.class);


    // ---------------------- init/destroy ----------------------

    private static ConcurrentHashMap<String, CacheNode> localCacheRepository = null;

    private static Thread refreshThread;
    private static boolean refreshThreadStop = false;
    public static void init(){

        localCacheRepository = new ConcurrentHashMap<String, CacheNode>();

        // preload: mirror or remote
        Map<String, String> preConfData = new HashMap<>();

        Map<String, String> mirrorConfData = ServerConfMirrorConf.readConfMirror();

        Map<String, String> remoteConfData = null;
        if (mirrorConfData!=null && mirrorConfData.size()>0) {
            remoteConfData = ServerConfRemoteConf.find(mirrorConfData.keySet());
        }

        if (mirrorConfData!=null && mirrorConfData.size()>0) {
            preConfData.putAll(mirrorConfData);
        }
        if (remoteConfData!=null && remoteConfData.size()>0) {
            preConfData.putAll(remoteConfData);
        }
        if (preConfData!=null && preConfData.size()>0) {
            for (String preKey: preConfData.keySet()) {
                set(preKey, preConfData.get(preKey), SET_TYPE.PRELOAD );
            }
        }

        // refresh thread
        refreshThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!refreshThreadStop) {
                    try {
                        refreshCacheAndMirror();
                    } catch (Exception e) {
                        if (!refreshThreadStop && !(e instanceof InterruptedException)) {
                            logger.error(">>>>>>>>>> server-conf, refresh thread error.");
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
                logger.info(">>>>>>>>>> server-conf, refresh thread stoped.");
            }
        });
        refreshThread.setDaemon(true);
        refreshThread.start();

        logger.info(">>>>>>>>>> server-conf, ServerConfLocalCacheConf init success.");
    }

    public static void destroy(){
        if (refreshThread != null) {
            refreshThreadStop = true;
            refreshThread.interrupt();
        }
    }

    /**
     * local cache node
     */
    public static class CacheNode implements Serializable{
        private static final long serialVersionUID = 42L;

        private String value;

        public CacheNode() {
        }

        public CacheNode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }


    // ---------------------- util ----------------------

    /**
     * refresh Cache And Mirror, with real-time minitor
     */
    private static void refreshCacheAndMirror() throws InterruptedException{

        if (localCacheRepository.size()==0) {
            TimeUnit.SECONDS.sleep(3);
            return;
        }

        // monitor
        boolean monitorRet = ServerConfRemoteConf.monitor(localCacheRepository.keySet());

        // avoid fail-retry request too quick
        if (!monitorRet){
            TimeUnit.SECONDS.sleep(10);
        }

        // refresh cache: remote > cache
        Set<String> keySet = localCacheRepository.keySet();
        if (keySet.size() > 0) {

            Map<String, String> remoteDataMap = ServerConfRemoteConf.find(keySet);
            if (remoteDataMap!=null && remoteDataMap.size()>0) {
                for (String remoteKey:remoteDataMap.keySet()) {
                    String remoteData = remoteDataMap.get(remoteKey);

                    CacheNode existNode = localCacheRepository.get(remoteKey);
                    if (existNode!=null && existNode.getValue()!=null && existNode.getValue().equals(remoteData)) {
                        logger.debug(">>>>>>>>>> server-conf: RELOAD unchange-pass [{}].", remoteKey);
                    } else {
                        set(remoteKey, remoteData, SET_TYPE.RELOAD );
                    }

                }
            }

        }

        // refresh mirror: cache > mirror
        Map<String, String> mirrorConfData = new HashMap<>();
        for (String key: keySet) {
            CacheNode existNode = localCacheRepository.get(key);
            mirrorConfData.put(key, existNode.getValue()!=null?existNode.getValue():"");
        }
        ServerConfMirrorConf.writeConfMirror(mirrorConfData);

        logger.debug(">>>>>>>>>> server-conf, refreshCacheAndMirror success.");
    }


    // ---------------------- inner api ----------------------

    public enum SET_TYPE{
        SET,        // first use
        RELOAD,     // value updated
        PRELOAD     // pre hot
    }

    /**
     * set conf (invoke listener)
     *
     * @param key
     * @param value
     * @return
     */
    private static void set(String key, String value, SET_TYPE optType) {
        localCacheRepository.put(key, new CacheNode(value));
        logger.info(">>>>>>>>>> server-conf: {}: [{}={}]", optType, key, value);

        // value updated, invoke listener
        if (optType == SET_TYPE.RELOAD) {
            ServerConfListenerFactory.onChange(key, value);
        }

        // new conf, new monitor
        if (optType == SET_TYPE.SET) {
            refreshThread.interrupt();
        }
    }

    /**
     * get conf
     *
     * @param key
     * @return
     */
    private static CacheNode get(String key) {
        if (localCacheRepository.containsKey(key)) {
            CacheNode cacheNode = localCacheRepository.get(key);
            return cacheNode;
        }
        return null;
    }

    /**
     * update conf  (only update exists key)  (invoke listener)
     *
     * @param key
     * @param value
     */
    /*private static void update(String key, String value) {
        if (localCacheRepository.containsKey(key)) {
            set(key, value, SET_TYPE.UPDATE );
        }
    }*/

    /**
     * remove conf
     *
     * @param key
     * @return
     */
    /*private static void remove(String key) {
        if (localCacheRepository.containsKey(key)) {
            localCacheRepository.remove(key);
        }
        logger.info(">>>>>>>>>> server-conf: REMOVE: [{}]", key);
    }*/


    // ---------------------- api ----------------------

    /**
     * get conf
     *
     * @param key
     * @param defaultVal
     * @return
     */
    public static String get(String key, String defaultVal) {

        // level 1: local cache
        ServerConfLocalCacheConf.CacheNode cacheNode = ServerConfLocalCacheConf.get(key);
        if (cacheNode != null && StringUtil.isNotEmpty(cacheNode.getValue())) {
            return cacheNode.getValue();
        }

        // level 2	(get-and-watch, add-local-cache)
        String remoteData = null;
        try {
            remoteData = ServerConfRemoteConf.find(key);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        if(StringUtil.isEmpty(remoteData)){
        	remoteData=defaultVal;
        }
        set(key, remoteData, SET_TYPE.SET );		// support cache null value
        return remoteData;
    }

}
