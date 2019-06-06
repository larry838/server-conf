package com.wshsoft.conf.core.listener;

import com.wshsoft.conf.core.ServerConfClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * server conf listener
 *
 * @author Carry_xie 2018-02-04 01:27:30
 */
public class ServerConfListenerFactory {
    private static Logger logger = LoggerFactory.getLogger(ServerConfListenerFactory.class);

    /**
     * server conf listener repository
     */
    private static ConcurrentHashMap<String, List<ServerConfListener>> keyListenerRepository = new ConcurrentHashMap<>();
    private static List<ServerConfListener> noKeyConfListener = Collections.synchronizedList(new ArrayList<ServerConfListener>());

    /**
     * add listener and first invoke + watch
     *
     * @param key   empty will listener all key
     * @param ServerConfListener
     * @return
     */
    public static boolean addListener(String key, ServerConfListener ServerConfListener){
        if (ServerConfListener == null) {
            return false;
        }
        if (key==null || key.trim().length()==0) {
            // listene all key used
            noKeyConfListener.add(ServerConfListener);
            return true;
        } else {

            // first use, invoke and watch this key
            try {
                String value = ServerConfClient.get(key);
                ServerConfListener.onChange(key, value);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }

            // listene this key
            List<ServerConfListener> listeners = keyListenerRepository.get(key);
            if (listeners == null) {
                listeners = new ArrayList<>();
                keyListenerRepository.put(key, listeners);
            }
            listeners.add(ServerConfListener);
            return true;
        }
    }

    /**
     * invoke listener on server conf change
     *
     * @param key
     */
    public static void onChange(String key, String value){
        if (key==null || key.trim().length()==0) {
            return;
        }
        List<ServerConfListener> keyListeners = keyListenerRepository.get(key);
        if (keyListeners!=null && keyListeners.size()>0) {
            for (ServerConfListener listener : keyListeners) {
                try {
                    listener.onChange(key, value);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        if (noKeyConfListener.size() > 0) {
            for (ServerConfListener confListener: noKeyConfListener) {
                try {
                    confListener.onChange(key, value);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

}
