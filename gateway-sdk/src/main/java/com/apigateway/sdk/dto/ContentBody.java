package com.apigateway.sdk.dto;

import com.apigateway.sdk.HttpCaller;

import java.io.File;

/**
 * 设置HTTP传输的body内容，可以是Json String或者是byte[]格式
 *
 * @author hpy
 * @date 2021
 */
public class ContentBody {

    private String jsonBody;
    private byte[] bytesBody;

    /**
     * 使用Json串构造ContentBody
     *
     * @param jsonStr
     */
    public ContentBody(String jsonStr) {
        this.jsonBody = jsonStr;
    }

    /**
     * 使用byte数组构造ContentBody
     *
     * @param bytes
     */
    public ContentBody(byte[] bytes) {
        this.bytesBody = bytes;
    }

    /**
     * 传输文件
     */
    public ContentBody(File file) {
        this.bytesBody = HttpCaller.readFile(file);
    }

    public String getStrContentBody() {
        return jsonBody;
    }

    public byte[] getBytesContentBody() {
        return bytesBody;
    }
}
