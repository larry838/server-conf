package com.wshsoft.conf.sample.config;

import com.wshsoft.conf.core.spring.ServerConfFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * server-conf config
 *
 * @author Carry_xie 2017-04-28
 */
@Configuration
public class ServerConfConfig {
    private Logger logger = LoggerFactory.getLogger(ServerConfConfig.class);


    @Value("${server.conf.admin.address}")
    private String adminAddress;

    @Value("${server.conf.env}")
    private String env;

    @Value("${server.conf.access.token}")
    private String accessToken;

    @Value("${server.conf.mirrorfile}")
    private String mirrorfile;


    @Bean
    public ServerConfFactory ServerConfFactory() {

        ServerConfFactory ServerConf = new ServerConfFactory();
        ServerConf.setAdminAddress(adminAddress);
        ServerConf.setEnv(env);
        ServerConf.setAccessToken(accessToken);
        ServerConf.setMirrorfile(mirrorfile);

        logger.info(">>>>>>>>>>> server-conf config init.");
        return ServerConf;
    }

}