package com.wshsoft.conf.core.listener;

/**
 * server conf listener
 *
 * @author Carry_xie 2018-02-04 01:27:30
 */
public interface ServerConfListener {

    /**
     * invoke when first-use or conf-change
     *
     * @param key
     */
    public void onChange(String key, String value) throws Exception;

}
