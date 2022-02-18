/*
 * Licensed Materials - Property of tenxcloud.com
 * (C) Copyright 2019 TenxCloud. All Rights Reserved.
 */

package com.apigateway.sdk;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.apigateway.sdk.constants.PluginConstant;
import com.apigateway.sdk.dto.AuthParam;
import com.apigateway.sdk.exception.BadRequest;
import com.apigateway.sdk.utils.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author hpy
 * @date 2021
 */
@Slf4j
public class TokenHelper {
    private static String CLIENT_CREDENTIALS = "client_credentials";

    private static String oauth2ClientCredentialsToken(String url, String clientId, String clientSecret, String scope) {
        log.info("oauth2ClientCredentialsToken url={}, client_id={}, client_secret={}, scope={}", url, clientId, clientSecret, scope);
        String token = null;
        HashMap<String, String> params = new HashMap<>();
        try{
            params.put("client_id", clientId);
            params.put("client_secret", clientSecret);
            params.put("scope", scope == null ? "" : scope);
            params.put("grant_type", CLIENT_CREDENTIALS);
            String result = HttpUtil.sendPost(url, params);
            JSONObject responseJson = JSON.parseObject(result);
            String message = responseJson.getString("message");
            if (StringUtils.isNotBlank(message)) {
                throw new BadRequest("please check your api，it must contain post method");
            }
            token = responseJson.getString("access_token");
        }catch (Exception e){
            log.error("oauth2ClientCredentialsToken erro: ", e);
        }finally {
            return token;
        }
    }

    private static String jwtToken(String algorithm, String key, String privateKeyOrSecret) {
        if(algorithm.startsWith("HS")){
            return tokenAlgorithmHS(algorithm, key, privateKeyOrSecret);
        }
        if(algorithm.startsWith("ES")){
            verifyES256PrivateKey(privateKeyOrSecret);
            return tokenAlgorithmES(algorithm, key, privateKeyOrSecret);
        }
        if(algorithm.startsWith("RS")){
            verifyRS256PrivateKey(privateKeyOrSecret);
            return tokenAlgorithmRS(algorithm, key, privateKeyOrSecret);
        }
        return null;
    }

    private static String tokenAlgorithmES(String algorithm, String key, String privateKeyStr) {

        String jwtToken = null;
        try {
            privateKeyStr = privateKeyStr
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("\n", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .trim();
            PrivateKey privateKey = KeyUtil.getECPrivateKey(privateKeyStr);
            jwtToken = Jwts.builder()
                    .setHeaderParam("typ", "jwt")
                    .setHeaderParam("alg", algorithm)
                    .setIssuer(key) //key
                    .signWith(privateKey, SignatureAlgorithm.forName(algorithm))
                    .compact();
        }catch (Exception e){
            log.info("algorithm={}, key={}, privateKey: ", algorithm, key);
            log.info("{}", privateKeyStr);
            log.error("tokenAlgorithmES error: ", e);
            throw new BadRequest("please check your privateKey");
        }
        return jwtToken;
    }

    private static String tokenAlgorithmRS(String algorithm, String key, String privateKeyStr) {
        String jwtToken = null;
        try {
            privateKeyStr = privateKeyStr
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("\n", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .trim();
            PrivateKey privateKey = KeyUtil.privateKey(privateKeyStr);
            jwtToken = Jwts.builder()
                    .setHeaderParam("typ", "jwt")
                    .setHeaderParam("alg", algorithm)
                    .setIssuer(key) //key
                    .signWith(privateKey, SignatureAlgorithm.forName(algorithm))
                    .compact();
        }catch (Exception e){
            log.info("algorithm={}, key={}, privateKey: ", algorithm, key);
            log.info("{}", privateKeyStr);
            log.error("tokenAlgorithmRS error: ", e);
            throw new BadRequest("please check your privateKey");
        }
        return jwtToken;
    }

    private static void verifyES256PrivateKey(String privateKey){
        if(StringUtils.isBlank(privateKey)) {
            throw new BadRequest("私钥内容不能为空!");
        }
        if (!privateKey.startsWith(PluginConstant.PRIVATE_KEY_CONTEXT_BEGIN)
                || !privateKey.endsWith(PluginConstant.PRIVATE_KEY_CONTEXT_END)){
            throw new BadRequest("私钥格式非法!");
        }
    }

    private static void verifyRS256PrivateKey(String privateKey){
        if(StringUtils.isBlank(privateKey)) {
            throw new BadRequest("私钥内容不能为空!");
        }
        if (!privateKey.startsWith(PluginConstant.RSA_PRIVATE_KEY_CONTEXT_BEGIN)
                || !privateKey.endsWith(PluginConstant.RSA_PRIVATE_KEY_CONTEXT_END)){
            throw new BadRequest("私钥格式非法!");
        }
    }

    public static PrivateKey privateKey(String privateKey) throws GeneralSecurityException, IOException {
        byte[] b1 = Base64.getDecoder().decode(privateKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(b1);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    private static String tokenAlgorithmHS(String algorithm, String key, String secret) {
        String jwtToken = null;
        try{
            jwtToken = Jwts.builder()
                    .setHeaderParam("typ", "jwt")
                    .setHeaderParam("alg", algorithm)
                    .setIssuer(key) //key
                    .signWith(SignatureAlgorithm.forName(algorithm), secret// secret
                            .getBytes(Charset.forName("utf-8")))
                    .compact();
        }catch (Exception e){
            log.info("algorithm={}, key={}, secret={}", algorithm, key, secret);
            log.error("tokenAlgorithmHS error: ", e);
        }

        return jwtToken;
    }

    private static String basicAuthToken(String userName, String password) {
        String base64Str = String.format("%s:%s", userName, password);
        return Base64Util.getBase64(base64Str);
    }

    public static void setAuthorizationHeader(AuthParam authParam, Map<String, String> headers,
                                              Map<String, String> paramsMap,String contentBody) {
        String token = null;
        Map<String,String> hmacTokenResult = new HashMap<>();
        switch (authParam.authType) {
            case PluginConstant.BASIC_AUTH:
                token = String.format("Basic %s", basicAuthToken(authParam.username, authParam.password));
                break;
            case PluginConstant.JWT:
                token = jwtToken(authParam.algorithm, authParam.key, authParam.secretOrPrivateKey);
                if(!StringUtils.isBlank(token)){
                    token = String.format("Bearer %s", token);
                }
                break;
            case PluginConstant.OAUTH2:
                // 获取 token 地址协议为 https
                authParam.url = authParam.url.startsWith("http:") ? authParam.url.replace("http:", "https:") :  authParam.url;
                authParam.url = authParam.url.contains("?")? authParam.url.substring(0,authParam.url.indexOf("?")) : authParam.url;
                String oauthTokenUrl = String.format("%s/oauth2/token", authParam.url);
                token = oauth2ClientCredentialsToken(oauthTokenUrl, authParam.clientId, authParam.clientSecret, authParam.scope);
                if(!StringUtils.isBlank(token)){
                    token = String.format("Bearer %s", token);
                } else {
                    throw new BadRequest("please check your api，it must contain post method");
                }
                break;
            case PluginConstant.HMAC_AUTH:
                hmacTokenResult = hmacCredentialsToken(authParam.hmacUserName,authParam.hmacSecret,
                        authParam.enforceHeaders,authParam.hmacAlgorithm,
                        authParam.validateRequestBody,headers,paramsMap, contentBody);
                if(!hmacTokenResult.isEmpty()){
                    token = hmacTokenResult.get("Authorization");
                }
        }
        if(StringUtils.isBlank(token)){
            return;
        }
        if(PluginConstant.HMAC_AUTH.equals(authParam.authType)){
            headers.putAll(hmacTokenResult);
        } else {
            headers.put("Authorization", token);
        }
        log.info("headers: {}", headers);
    }

    private static Map<String, String> hmacCredentialsToken(String hmacUserName, String hmacSecret,List<String> enforceHeaders,
                                                            String hmacAlgorithm, Boolean validateRequestBody,
                                                            Map<String, String> headers,Map<String, String> paramsMap, String contentBody) {
        Map<String, String> result = new HashMap<>();
        if (null == enforceHeaders || enforceHeaders.size() == 0) {
            enforceHeaders.add("date");
        }
        if (!enforceHeaders.contains("date")){
            enforceHeaders.add("date");
        }
        for (String enforceHeader : enforceHeaders) {
            if ("date".equals(enforceHeader)) {
                continue;
            }
            if (!headers.containsKey(enforceHeader)) {
                throw new IllegalArgumentException("hmac auth 强制加密的header数据没找到，请检查参数！");
            }
        }
        Date d = new Date();
        DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String hdate = format.format(d);
        log.info("date: "+ hdate);
        String digest = "";
        String body = "";
        if (validateRequestBody) {//针对body进行加密
            if (null!=paramsMap && !paramsMap.isEmpty()) {//form表单参数
                body = JSONObject.toJSONString(paramsMap);
                throw new IllegalArgumentException("hmac auth 对body体加密暂不支持form表单参数！");
            } else if (StringUtils.isNotBlank(contentBody)) {//针对json和xml的body加密
                body = contentBody;
            }
            if (StringUtils.isNotBlank(body)) {
                try {
                    digest = new String(Base64.getEncoder().encode(HashUtils.getInstance().SHA256ReturnByte(body)), "US-ASCII");
                    //digest = new String(Base64.getEncoder().encode(HashUtils.getInstance().MapSHA256ReturnByte(paramsMap)), "US-ASCII");
                    log.info("显示digest: " + "SHA-256="+digest);
                    result.put("digest", "SHA-256="+digest);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        StringBuilder stb = new StringBuilder();
        stb = stb.append("date: ").append(hdate);
        String headerStr = "date";
        for (String enforceHeader : enforceHeaders) {
            if ("date".equals(enforceHeader)) {
                continue;
            }
            headerStr = headerStr +" "+ enforceHeader;
            stb = stb.append("\n").append(enforceHeader).append(": ").append(headers.get(enforceHeader));
        }
        if(StringUtils.isNotBlank(digest)){
            headerStr = headerStr + " " + "digest";
            stb = stb.append("\n").append("digest: ").append("SHA-256=").append(digest);
        }
        String content =stb.toString();
        log.info("签名前内容: " + content);
        try {
            String hmacAuthType = PluginConstant.HMAC_ALGORITHM_MAP.get(hmacAlgorithm);
            String signature = new String(
                    Base64.getEncoder().encode(HmacShaUtil.signatureReturnBytesByType(content, hmacSecret,
                            hmacAuthType)), "US-ASCII");
            log.info("指定编码:"+signature);
            result.put("date", hdate);
            String authorization = "hmac username="+"\""+hmacUserName+"\""+", algorithm="+"\""+hmacAlgorithm+"\""+", headers="+"\""+headerStr+"\""+", signature="+"\""+signature+"\"";
            log.info("Authorization:"+authorization);
            result.put("Authorization", authorization);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
