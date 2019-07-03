package com.wshsoft.conf.core.listener.impl;

import com.wshsoft.conf.core.listener.ServerConfListener;
import com.wshsoft.conf.core.spring.ServerConfFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * server conf annotaltion refresh
 *
 * @author Carry_xie 2018-02-204 01:46:20
 */
public class BeanRefreshServerConfListener implements ServerConfListener {


    // ---------------------- listener ----------------------

    // object + field
    public static class BeanField{
        private String beanName;
        private String property;

        public BeanField() {
        }

        public BeanField(String beanName, String property) {
            this.beanName = beanName;
            this.property = property;
        }

        public String getBeanName() {
            return beanName;
        }

        public void setBeanName(String beanName) {
            this.beanName = beanName;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }
    }

    // key : object-field[]
    private static Map<String, List<BeanField>> key2BeanField = new ConcurrentHashMap<String, List<BeanField>>();
    public static void addBeanField(String key, BeanField beanField){
        List<BeanField> beanFieldList = key2BeanField.get(key);
        if (beanFieldList == null) {
            beanFieldList = new ArrayList<>();
            key2BeanField.put(key, beanFieldList);
        }
        for (BeanField item: beanFieldList) {
            if (item.getBeanName().equals(beanField.getBeanName()) && item.getProperty().equals(beanField.getProperty())) {
                return; // avoid repeat refresh
            }
        }
        beanFieldList.add(beanField);
    }

    // ---------------------- onChange ----------------------

    @Override
    public void onChange(String key, String value) throws Exception {
        List<BeanField> beanFieldList = key2BeanField.get(key);
        if (beanFieldList!=null && beanFieldList.size()>0) {
            for (BeanField beanField: beanFieldList) {
                ServerConfFactory.refreshBeanField(beanField, value, null);
            }
        }
    }
}