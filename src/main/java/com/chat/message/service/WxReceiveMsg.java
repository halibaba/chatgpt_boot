package com.chat.message.service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @program: chatgpt_boot
 * @interfaceName WxReceiveMsg
 * @description:
 * @author: MT
 * @create: 2023-04-27 16:11
 **/
public interface WxReceiveMsg {

    /**
     * 验证签名
     * @param signature
     * @param timestamp
     * @param nonce
     * @return
     */
    Boolean checkSignature(String signature, String timestamp, String nonce);

    /**
     * 解析xml格式数据转为map格式
     * @param req
     * @return
     */
    Map<String, String> parseXmlData2Map(HttpServletRequest req);

    /**
     * 接收消息，判断消息类型，并根据关键词进行回复
     * @param param
     * @return
     */
    String receiveMessage(Map<String, String> param);
}