package com.wshsoft.conf.sample.controller;

import com.wshsoft.conf.core.ServerConfClient;
import com.wshsoft.conf.core.listener.ServerConfListener;
import com.wshsoft.conf.sample.demo.DemoConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Carry_xie 2019-06-04 01:27:30
 */
@Controller
public class IndexController {
    private static Logger logger = LoggerFactory.getLogger(IndexController.class);

    static {
        /**
         * 配置变更监听示例：可监听配置变更事件等高级功能；
         */
        ServerConfClient.addListener("default.key01", new ServerConfListener(){
            @Override
            public void onChange(String key, String value) throws Exception {
                logger.info("配置变更事件通知：{}={}", key, value);
            }
        });
    }

    @Resource
    private DemoConf demoConf;

    @RequestMapping("")
    @ResponseBody
    public List<String> index(){

        List<String> list = new LinkedList<>();

        /**
         * 方式1: API方式
         *
         * 		- 参考 "IndexController" 中 "ServerConfClient.get("key", null)" 即可；
         * 		- 用法：代码中直接调用API即可，示例代码 "ServerConfClient.get("key", null)";
         * 		- 优点：
         * 			- 配置从配置中心自动加载；
         * 			- 存在LocalCache，不用担心性能问题；；
         * 			- 支持动态推送更新；
         * 			- 支持多数据类型；
         */
        String paramByApi = ServerConfClient.get("default.key01", null);
        list.add("1、API方式:default.key01=" + paramByApi);

        /**
         * 方式2: @ServerConf 注解方式
         *
         * 		- 参考 "DemoConf.paramByAnno" 属性配置；示例代码 "@ServerConf("key") public String paramByAnno;"；
         * 		- 用法：对象Field上加注解 ""@ServerConf("default.key02")"，支持设置默认值，支持设置是否开启动态刷新；
         * 		- 优点：
         * 			- 配置从配置中心自动加载；
         * 			- 存在LocalCache，不用担心性能问题；
         * 			- 支持动态推送更新；
         * 			- 支持设置配置默认值；
         */
        list.add("2、@ServerConf(无默认值)注解方式:default.key02=" + demoConf.paramByAnno);
        
        list.add("3、@ServerConf(有默认值)注解方式:default.key03=" + demoConf.paramByDefaultAnno);

        /**
         * 方式3: XML占位符方式
         *
         * 		- 参考 "xml-test.xml" 中 "DemoConf.paramByXml" 属性配置；示例代码 "<property name="paramByXml" value="$ServerConf{key}" />"；
         * 		- 用法：占位符方式 "$ServerConf{key}"；
         * 		- 优点：
         * 			- 配置从配置中心自动加载；
         * 			- 存在LocalCache，不用担心性能问题；
         *
         */
        list.add("4、XML占位符方式: default.key04=" + demoConf.paramByXml);
        
        return list;
    }

}
