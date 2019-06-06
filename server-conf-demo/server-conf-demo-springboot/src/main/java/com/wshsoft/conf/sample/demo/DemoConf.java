package com.wshsoft.conf.sample.demo;

import com.wshsoft.conf.core.annotation.ServerConf;
import org.springframework.stereotype.Component;

/**
 *  测试示例（可删除）
 *
 *  @author Carry_xie
 */
@Component
public class DemoConf {

	@ServerConf("default.key02")
	public String paramByAnno;

	@ServerConf(value="default.key03",defaultValue="3")
	public String paramByDefaultAnno;
	
	public String paramByXml = "XML方式配置：参考查看xml-test.xml，但springboot项目不推荐采用该方式";

	public void setParamByXml(String paramByXml) {
		this.paramByXml = paramByXml;
	}
}
