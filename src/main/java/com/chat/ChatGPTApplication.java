package com.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @program: chatgpt_boot
 * @ClassName ChatGPTApplication
 * @description:
 * @author: MT
 * @create: 2023-04-25 09:47
 **/
@SpringBootApplication
@EnableConfigurationProperties
public class ChatGPTApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatGPTApplication.class, args);
    }

}