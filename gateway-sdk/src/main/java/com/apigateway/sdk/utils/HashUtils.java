/*
 * Licensed Materials - Property of tenxcloud.com
 * (C) Copyright 2019 TenxCloud. All Rights Reserved.
 *
 * 2020/8/6 @author peiyun
 */
package com.apigateway.sdk.utils;

import java.security.MessageDigest;
import java.util.Map;

/**
 * @author hpy
 * @date 2021
 */
public class HashUtils {
    private HashUtils() {

    }

    private static class HashUtilsInner{
        private static volatile HashUtils INSTANCE = new HashUtils();
    }

    public static HashUtils getInstance() {
        return HashUtilsInner.INSTANCE;
    }

    /**
     * @Description:封装SHA-1方法
     * @param text 需要加密的数据
     * @Return:String 返回结果
     */
    public byte[] SHA1(String text) {
        try {
            byte[] data = text.getBytes("US-ASCII");
            return encryptReturnByte(data,"SHA-1");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param text 需要加密的数据
     * @Return:String 返回结果
     */
    public byte[] SHA256ReturnByte(String text) {
        try {
            byte[] data = text.getBytes("US-ASCII");
            return encryptReturnByte(data,"SHA-256");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @Description:封装SHA-1方法
     * @param textMap 需要加密的数据
     * @Return:String 返回结果
     */
    public byte[] MapSHA256ReturnByte(Map<String,String> textMap) {
        try {
            byte[] data = textMap.toString().getBytes("US-ASCII");
            return encryptReturnByte(data,"SHA-256");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] encryptReturnByte(byte[] data, String algorithm) throws Exception {
        // 1. 根据算法名称获实现了算法的加密实例
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        // 2. 加密数据, 计算数据的哈希值
        byte[] cipher = digest.digest(data);

        // 3. 将结果转换为十六进制小写
        return cipher;
    }

}
