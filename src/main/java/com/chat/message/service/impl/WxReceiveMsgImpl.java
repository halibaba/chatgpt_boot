package com.chat.message.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chat.message.service.WxReceiveMsg;
import com.chat.message.utils.HttpUtil;
import com.chat.message.utils.SysFileConfig;
import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @program: chatgpt_boot
 * @ClassName WxReceiveMsgImpl
 * @description:
 * @author: MT
 * @create: 2023-04-27 16:12
 **/
@Service
@Slf4j
public class WxReceiveMsgImpl implements WxReceiveMsg {

    @Autowired
    private SysFileConfig sysFileConfig;

    @Value("${wx.token}")
    private String TOKEN;

    @Value("${openAI.token}")
    private String openAIToken;

    @Override
    public Boolean checkSignature(String signature, String timestamp, String nonce) {
        String token = TOKEN;
        String[] arr = {token, timestamp, nonce};
        Arrays.sort(arr);
        StringBuilder sb = new StringBuilder();
        for (String a : arr){
            sb.append(a);
        }
        String str = sb.toString();
        // SHA1签名生成
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(str.getBytes());
        byte[] digest = md.digest();

        StringBuffer hexstr = new StringBuffer();
        String shaHex = "";
        for (int i = 0; i < digest.length; i++) {
            shaHex = Integer.toHexString(digest[i] & 0xFF);
            if (shaHex.length() < 2) {
                hexstr.append(0);
            }
            hexstr.append(shaHex);
        }
        log.info("接收的签名:{}", signature);
        log.info("生成的签名:{}", hexstr.toString());
        return hexstr.toString().equals(signature);
    }

    @Override
    public Map<String, String> parseXmlData2Map(HttpServletRequest req) {
        HashMap<String, String> msgMap = new HashMap<>();

        try {
            ServletInputStream inputStream = req.getInputStream();

            // dom4j 用于读取 XML 文件输入流的类
            SAXReader saxReader = new SAXReader();
            // 读取 XML 文件输入流, XML 文档对象
            Document document = saxReader.read(inputStream);
            // XML 文件的根节点
            Element root = document.getRootElement();
            // 所有的子节点
            List<Element> childrenElement = root.elements();
            for (Element element : childrenElement) {
                msgMap.put(element.getName(), element.getStringValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return msgMap;
    }

    @Override
    public String receiveMessage(Map<String, String> param) {
        String content = "";
        try {
            //消息类型
            String msgType = param.get("MsgType");
            switch(msgType){
                case "text" :		//普通文本类型，例如用户发送：java
                    content = this.keyWordMsgTip(param);
                    break;
                default:
                    content = "success";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content;
    }

    /**
     * 处理关键字搜索事件
     * 图文消息个数；当用户发送文本、图片、语音、视频、图文、地理位置这六种消息时，开发者只能回复1条图文消息；其余场景最多可回复8条图文消息
     * @param param
     * @return
     */
    private String keyWordMsgTip(Map<String, String> param) throws IOException {
        String fromusername = param.get("FromUserName");
        String tousername = param.get("ToUserName");
        String content = param.get("Content");

        if(content.contains("图片") ||
                content.contains("照片") ||
                content.contains("画一幅") ||
                content.contains("画一副")
        ){
            return getImgMsg(fromusername, tousername, content);
        }else {
            return getTextMsg(fromusername, tousername, content);
        }

    }

    /**
     * 返回文本
     * @return
     */
    public synchronized String getTextMsg(String fromusername, String tousername, String content) {
        String fileName = fromusername + "_" + new Timestamp(System.currentTimeMillis()).getTime() + ".html";

        //单位为秒，不是毫秒
        Long createTime = new Date().getTime() / 1000;
        StringBuffer text = new StringBuffer();

        AtomicReference<String> resMsg = new AtomicReference<>("");
        //异步调用
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                resMsg.set(textMsg(fromusername, content, fileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        String msg = "字节长度太长，回复内容已添加到html文件，点击查看：http://chatbba.top/v1/files/" + fileName;
        long startTime = System.currentTimeMillis(); //fetch starting time
        while((System.currentTimeMillis() - startTime) < 4000)
        {
            if(!StringUtils.isEmpty(resMsg.get())){
                msg = resMsg.get();
                System.out.println("chatgpt请求完毕，公众号返回结果：" + resMsg.get());
                System.out.println("chatgpt请求完毕，公众号返回结果msg：" + msg);
                break;
            }
        }
        System.out.println("-------输出msg:" + msg);
        text.append("<xml>");
        text.append("<ToUserName><![CDATA["+fromusername+"]]></ToUserName>");
        text.append("<FromUserName><![CDATA["+tousername+"]]></FromUserName>");
        text.append("<CreateTime><![CDATA["+createTime+"]]></CreateTime>");
        text.append("<MsgType><![CDATA[text]]></MsgType>");
        text.append("<Content><![CDATA["+ msg +"]]></Content>");
        text.append("</xml>");
        return text.toString();
    }

    public String textMsg(String fromusername, String content, String fileName) throws IOException {
        long startTime = System.currentTimeMillis(); //fetch starting time
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", "gpt-3.5-turbo");
        Map<String, Object> map = new HashMap<>();
        map.put("role", "user");
        map.put("content", content);
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(map);
        jsonObject.put("messages", jsonArray);
        jsonObject.put("temperature", 1.2);
        System.err.println("=========" + fromusername + "开始请求chatgpt...");
        JSONObject resObj = HttpUtil.postHttpJson("https://api.openai.com/v1/chat/completions", JSON.toJSONString(jsonObject), openAIToken);
        System.err.println("=========请求结果：" + resObj);
        Map resMap = JSON.parseObject(JSON.toJSONString(resObj));
        List<Map> choices = JSON.parseArray(JSON.toJSONString(resMap.get("choices")), Map.class);
        Object message = choices.get(0).get("message");
        JSONObject messageMap = JSON.parseObject(JSON.toJSONString(message));
        String msg = messageMap.get("content").toString();
        System.err.println("=========输出消息：" + msg);

        int length = msg.getBytes("UTF-8").length;
        System.err.println("=========字节数为：" + length);

        if(length > 1984 || (System.currentTimeMillis() - startTime) > 3500)
        {
            generateHtml("text", fileName, msg);
            System.out.println("========chatgpt请求完毕，返回结果：" + msg);
        }
        return msg;
    }

    /**
     * 返回图片
     * @return
     */
    public String getImgMsg(String fromusername, String tousername, String content) {
        String fileName = fromusername + "_" + new Timestamp(System.currentTimeMillis()).getTime() + ".html";
        //单位为秒，不是毫秒
        Long createTime = new Date().getTime() / 1000;
        StringBuffer text = new StringBuffer();
        AtomicReference<String> resMsg = new AtomicReference<>("");
        String msg = "图片已生成，点击查看：http://chatbba.top/v1/files/" + fileName;
        //异步调用
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("prompt", content);
                jsonObject.put("n", 1);
                jsonObject.put("size", "1024x1024");
                JSONObject resObj = HttpUtil.postHttpJson("https://api.openai.com/v1/images/generations", JSON.toJSONString(jsonObject), openAIToken);
                System.err.println("=========请求结果：" + resObj);
                Map resMap = JSON.parseObject(JSON.toJSONString(resObj));
                List<Map> data = JSON.parseArray(JSON.toJSONString(resMap.get("data")), Map.class);
                Object url = data.get(0).get("url");
                System.err.println("=========输出消息：" + url.toString());
                resMsg.set(url.toString());
                if((System.currentTimeMillis() - startTime) > 3500)
                {
                    generateHtml("img", fileName, url.toString());
                    System.out.println("========chatgpt请求完毕，返回url：" + url.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        long startTime = System.currentTimeMillis(); //fetch starting time
        while((System.currentTimeMillis() - startTime) < 4000)
        {
            if(!StringUtils.isEmpty(resMsg.get())){
                msg = resMsg.get();
                System.out.println("chatgpt请求完毕，公众号返回结果：" + resMsg.get());
                System.out.println("chatgpt请求完毕，公众号返回结果msg：" + msg);
                break;
            }
        }
        text.append("<xml>");
        text.append("<ToUserName><![CDATA["+fromusername+"]]></ToUserName>");
        text.append("<FromUserName><![CDATA["+tousername+"]]></FromUserName>");
        text.append("<CreateTime><![CDATA["+createTime+"]]></CreateTime>");
        text.append("<MsgType><![CDATA[text]]></MsgType>");
        text.append("<Content><![CDATA["+ msg +"]]></Content>");
        text.append("</xml>");
        return text.toString();
    }

    //生成html文件存储到服务器中
    public void generateHtml(String type, String fileName, String txt) throws IOException {
        String content = "";
        if(type.equals("text")){
            content = "<html><head><meta charset=\"UTF-8\"></head>\n" +
                    "<body>\n" +
                    "<pre class=\"content\">\n" +
                    txt.replaceAll("```.+?\n", "<code>").replaceAll("```", "</code>") +
                    "</pre>\n" +
                    "<style>\n" +
                    "pre { // 兼容多个浏览器\n" +
                    "    white-space: pre-wrap;\n" +
                    "    white-space: -moz-pre-wrap;\n" +
                    "    white-space: -pre-wrap;\n" +
                    "    white-space: -o-pre-wrap;\n" +
                    "    *word-wrap: break-word;\n" +
                    "    *white-space : normal ;\n" +
                    "    white-space: pre-wrap;\n" +
                    "    word-wrap: break-word;" +
                    "}\n" +
                    "*{\n" +
                    "font-size: 60px;\n" +
                    "}\n" +
                    "pre code { background-color: #333333; color: #f8f8f8; font-family: Consolas, 'Courier New', monospace;  padding: 15px; display: block; white-space: pre-wrap;\n" +
                    " }\n" +
                    "</style>\n" +
                    "</body></html>";
        }
        if(type.equals("img")){
            content = "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "  <meta charset=\"UTF-8\">\n" +
                    "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
                    "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "  <title>Document</title>\n" +
                    "</head>\n" +
                    "<style>\n" +
                    "  img {\n" +
                    "      width:100%;\n" +
                    "    }" +
                    "</style>" +
                    "<body>\n" +
                    "    <div id=\"ddd\">\n" +
                    "      <img src=\"" + txt + "\"/>\n" +
                    "    </div>\n" +
                    "</body>\n" +
                    "<script>\n" +
                    "if (document.documentElement && document.documentElement.clientHeight && document.documentElement.clientWidth)\n" +
                    "{\n" +
                    "winHeight = document.documentElement.clientHeight;\n" +
                    "winWidth = document.documentElement.clientWidth;\n" +
                    "}\n" +
                    "ddd.style.width= winWidth+\"px\";\n" +
                    "</script>" +
                    "</html>";
        }


        String hostname = sysFileConfig.getHostname();  // 目标Linux服务器的主机名
        String username = sysFileConfig.getUsername();         // 登录Linux服务器的用户名
        String password = sysFileConfig.getPassword();     // 登录Linux服务器的密码
        String remoteDir = sysFileConfig.getSystempath();  // 目标文件夹的路径

        try {
            // 创建JSch实例并连接到目标Linux服务器
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, hostname, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            // 创建SFTP通道并打开
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // 将HTML字符串写入到本地临时文件中
            File tempFile = File.createTempFile("temp-", ".html");
            tempFile.deleteOnExit();
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(tempFile)))) {
                out.println(content);
            }

            // 将临时文件上传到目标文件夹中
            channelSftp.cd(remoteDir);
            channelSftp.put(new FileInputStream(tempFile), fileName);

            // 关闭SFTP通道和SSH会话
            channelSftp.disconnect();
            session.disconnect();

            System.out.println("HTML文件上传成功！");

        } catch (JSchException | SftpException | IOException e) {
            e.printStackTrace();
        }
    }

    public String escapeXmlEntities(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
                    break;
                case '&':
                    result.append("&amp;");
                    break;
                case '\"':
                    result.append("&quot;");
                    break;
                case '\'':
                    result.append("&apos;");
                    break;
                default:
                    result.append(c);
                    break;
            }
        }
        return result.toString();
    }
}