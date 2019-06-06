package com.wshsoft.conf.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * @author Carry_xie 2018-02-24 22:00:52
 */
@SpringBootApplication
@Configuration
@ImportResource(locations= {"classpath:xml-test.xml"})
public class ServerConfSampleApplication {

	public static void main(String[] args) {
        SpringApplication.run(ServerConfSampleApplication.class, args);
	}

}