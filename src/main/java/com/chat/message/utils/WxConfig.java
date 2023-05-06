package com.chat.message.utils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @program: chatgpt_boot
 * @ClassName WxConfig
 * @description:
 * @author: MT
 * @create: 2023-04-25 10:59
 **/

@Data
@Configuration
@ConfigurationProperties(prefix = "wx.app")
public class WxConfig {

    private String id;
    private String secret;

}