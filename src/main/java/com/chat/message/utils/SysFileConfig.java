package com.chat.message.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @program: chatgpt_boot
 * @ClassName SysFileConfig
 * @description:
 * @author: MT
 * @create: 2023-05-06 08:25
 **/
@Data
@Configuration
@ConfigurationProperties(prefix = "file.upload")
public class SysFileConfig {

    String containerpath;
    String systempath;
    String hostname;
    String username;
    String password;
    String remoteDir;

}