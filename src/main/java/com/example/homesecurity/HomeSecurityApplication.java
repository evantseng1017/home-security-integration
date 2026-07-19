package com.example.homesecurity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.homesecurity.tak.TakProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(TakProperties.class)
public class HomeSecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomeSecurityApplication.class, args);
    }
}
