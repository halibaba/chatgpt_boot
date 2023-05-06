package com.chat.message.utils;

import com.alibaba.fastjson.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @program: chatgpt_boot
 * @ClassName HttpUtil
 * @description:
 * @author: MT
 * @create: 2023-04-25 10:32
 **/
public class HttpUtil {

    public static JSONObject getHttpJson(String url) throws Exception {
        try {
            java.net.URL realUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 设定请求的方法为"POST"，默认是GET
            // 建立实际的连接
            connection.connect();
            //请求成功
            if (connection.getResponseCode() == 200) {
                //执行getInputStream方法才实际发送http请求
                InputStream is = connection.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                //10KB的缓存
                byte[] buffer = new byte[10240];
                int len = 0;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                String jsonString = baos.toString();
                baos.close();
                is.close();
                JSONObject jsonArray = JSONObject.parseObject(jsonString);
                return jsonArray;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * url为请求地址，data为请求体
     * @param url
     * @param data
     * @return
     * @throws Exception
     */
    public static JSONObject postHttpJson(String url, String data, String openAIToken) {
        try {
            URL realUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.setRequestProperty("Content-Type", "application/json");
            //设置请求头
            connection.setRequestProperty("Authorization", openAIToken);
//            connection.setRequestProperty("Authorization", "Bearer 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");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);

            // 设置请求正文，将data替换为您要发送的数据
            OutputStream os = connection.getOutputStream();
            os.write(data.getBytes("UTF-8"));
            os.close();

            // 建立实际的连接
            connection.connect();

            //请求成功
            if (connection.getResponseCode() == 200) {
                //执行getInputStream方法才实际发送http请求
                InputStream is = connection.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                //10KB的缓存
                byte[] buffer = new byte[10240];
                int len = 0;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                String jsonString = baos.toString();
                baos.close();
                is.close();
                JSONObject jsonArray = JSONObject.parseObject(jsonString);
                return jsonArray;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}