package com.chat.message.utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * @program: chatgpt_boot
 * @ClassName ChatStrUtil
 * @description:
 * @author: MT
 * @create: 2023-04-30 12:35
 **/
public class ChatStrUtil {

    /**
     * 字符串按字节数截取
     * @param src
     * @param byteType
     * @param bytes
     */
    public static List<String> chineseSplitFunction(String src,String byteType, int bytes){
        if(src == null){
            return null;
        }
        List<String> splitList = new ArrayList<String>();
        int startIndex = 0;    //字符串截取起始位置
        int endIndex = bytes > src.length() ? src.length() : bytes;  //字符串截取结束位置
        try {
            while(startIndex < src.length()){
                String subString = src.substring(startIndex,endIndex);                //截取的字符串的字节长度大于需要截取的长度时，说明包含中文字符
                //在GBK编码中，一个中文字符占2个字节，UTF-8编码格式，一个中文字符占3个字节。
                while (subString.getBytes(byteType).length > bytes) {
                    --endIndex;
                    subString = src.substring(startIndex,endIndex);
                }
                splitList.add(src.substring(startIndex,endIndex));
                startIndex = endIndex;
                //判断结束位置时要与字符串长度比较(src.length())，之前与字符串的bytes长度比较了，导致越界异常。
                endIndex = (startIndex + bytes) > src.length() ? src.length()  : startIndex+bytes ;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return splitList;
    }
}