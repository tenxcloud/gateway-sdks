/*
 * Licensed Materials - Property of tenxcloud.com
 * (C) Copyright 2019 TenxCloud. All Rights Reserved.
 */

package com.apigateway.sdk.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hpy
 * @date 2021
 */
@Data
public class AuthParam {
    /**
     * 认证类型，basic-auth，jwt，oauth2，hmac-auth
     */
    public String authType;
    /**
     * basic-auth 用户名
     */
    public String username;
    /**
     * basic-auth 秘钥
     */
    public String password;
    //hmac-auth
    /**
     * hmac-auth 用户名
     */
    public String hmacUserName;
    /**
     * hmac-auth 秘钥
     */
    public String hmacSecret;
    /**
     * hmac-auth 加密算法，hmac-sha1，hmac-sha256，hmac-sha384，hmac-sha512，默认值是：hmac-sha1
     */
    public String hmacAlgorithm="hmac-sha1";
    /**
     * hmac-auth 加密header
     */
    public List<String> enforceHeaders=new ArrayList<>();
    /**
     * hmac-auth 是否针对body加密，默认：否
     */
    public Boolean validateRequestBody=false;
    //jwt
    /**
     * jwt 参数key
     */
    public String key;
    /**
     * jwt 秘钥或者是私钥
     */
    public String secretOrPrivateKey;
    /**
     * jwt 加密算法，包括：HS256，HS384，HS512，RS256，ES256
     */
    public String algorithm;

    //oauth2
    /**
     * oauth2 clientId
     */
    public String clientId;
    /**
     * oauth2 clientSecret
     */
    public String clientSecret;
    /**
     * oauth2 url
     */
    public String url;
    /**
     * oauth2 scope
     */
    public String scope;

}
