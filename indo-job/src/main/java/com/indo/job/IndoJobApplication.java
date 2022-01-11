package com.indo.job;

import com.indo.job.common.util.SpringUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
public class IndoJobApplication {

    public static void main(String[] args) {
        ApplicationContext applicationContext = SpringApplication.run(IndoJobApplication.class, args);
        SpringUtil.setApplicationContext(applicationContext);
    }


}