package com.chat.message.controller;

import com.chat.message.service.WxReceiveMsg;
import com.chat.message.utils.SysFileConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * @program: chatgpt_boot
 * @ClassName Message
 * @description:
 * @author: MT
 * @create: 2023-04-25 09:44
 **/
@RestController
@RequestMapping("v1")
@Slf4j
public class MessageController {

    @Autowired
    private WxReceiveMsg wxReceiveMsg;

    @Autowired
    private SysFileConfig sysFileConfig;

    /**
     * 自动回复URL校验
     * @param signature
     * @param timestamp
     * @param nonce
     * @param echostr
     * @param req
     * @return
     * @throws Exception
     */
    @RequestMapping("check_token")
    public String checkToken(String signature,
                             String timestamp,
                             String nonce,
                             String echostr,
                             HttpServletRequest req) {
        String requestMethod = req.getMethod();
        if (requestMethod.equals("POST")) { // 处理 POST 请求
            Map<String, String> msgMap = wxReceiveMsg.parseXmlData2Map(req);
            //接入ChatGPT
            String s = wxReceiveMsg.receiveMessage(msgMap);
            return s;
        } else if (requestMethod.equals("GET")) { // 处理 GET 请求
            log.info("接收到微信服务器的验证消息,[{},{},{}, {}]", signature, timestamp, nonce, echostr);
            if(StringUtils.isEmpty(signature) || StringUtils.isEmpty(timestamp) || StringUtils.isEmpty(nonce) || StringUtils.isEmpty(echostr)){
                throw new IllegalArgumentException("请求参数非法，请核实！");
            }
            Boolean aBoolean = wxReceiveMsg.checkSignature(signature, timestamp, nonce);
            if(!aBoolean){
                return "签名不合法！";
            }
            return echostr;
        } else {
            return "不是 GET 和 POST";
        }
    }

    /**
     * 存储文件
     * @param file
     * @return
     */
    @PostMapping("/files")
    public String handleFileUpload(@RequestParam("file") MultipartFile file) {
        String filename = file.getOriginalFilename();
        Path path = Paths.get(sysFileConfig.getSystempath(), filename);
        try {
            Files.copy(file.getInputStream(), path);
        } catch (IOException e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
        return "redirect:/";
    }

    /**
     * 获取文件
     * @param filename
     * @return
     */
    @GetMapping(value = "/files/{filename:.+}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public Resource getFile(@PathVariable String filename) {
        System.out.println("开始获取文件...");
        System.out.println("文件路径：" + sysFileConfig.getContainerpath());
        Path path = Paths.get(sysFileConfig.getContainerpath(), filename);
        System.out.println("文件名：" + path.getFileName());
        String path1 = path.toFile().getPath();
        System.out.println("完整文件路径：" + path1);
        Resource resource = new FileSystemResource(path.toFile());
        if (resource.exists() || resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("Could not read the file!");
        }
    }

    /**
     * 抛异常时输出指定内容
     * @return
     */
    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException ex){
        return "chatgpt响应内容较长，正在为您处理，可稍后重新查看！";
    }

    /**
     * 抛异常时重定向到指定的html页面
     * @return
     */
//    @ExceptionHandler(RuntimeException.class)
//    public ModelAndView handleRuntimeException(){
//        ModelAndView modelAndView = new ModelAndView("redirect:/chat.html");
//        return modelAndView;
//    }

    /**
     * 提取出xml数据包中的加密消息
     * @param xmltext 待提取的xml字符串
     * @return 提取出的加密消息字符串
     * @throws Exception
     */
    public Object[] extract(String xmltext) throws Exception     {
        Object[] result = new Object[3];
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            StringReader sr = new StringReader(xmltext);
            InputSource is = new InputSource(sr);
            Document document = db.parse(is);

            Element root = document.getDocumentElement();
            NodeList nodelist1 = root.getElementsByTagName("Encrypt");
            NodeList nodelist2 = root.getElementsByTagName("ToUserName");
            result[0] = 0;
            result[1] = nodelist1.item(0).getTextContent();
            result[2] = nodelist2.item(0).getTextContent();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception();
        }
    }
}