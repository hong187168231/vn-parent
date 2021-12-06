package com.indo.job.executor;

import com.indo.job.executor.util.SpringUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
@EnableDiscoveryClient
public class IndoJobExecutorApplication {

    public static void main(String[] args) {
        ApplicationContext applicationContext = SpringApplication.run(IndoJobExecutorApplication.class, args);
        SpringUtil.setApplicationContext(applicationContext);
    }

}