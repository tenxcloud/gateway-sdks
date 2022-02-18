/*
 * Licensed Materials - Property of tenxcloud.com
 * (C) Copyright 2019 TenxCloud. All Rights Reserved.
 */

package com.apigateway.sdk.constants;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hpy
 * @date 2021
 */
public class PluginConstant {

    /*身份认证插件*/
    public static final String BASIC_AUTH = "basic-auth";
    public static final String JWT = "jwt";
    public static final String OAUTH2 = "oauth2";
    public static final String HMAC_AUTH = "hmac-auth";
    public static final String AUTH_TYPE_NONE = "none";

    public static final String RSA_PRIVATE_KEY_CONTEXT_BEGIN = "-----BEGIN RSA PRIVATE KEY-----";
    public static final String RSA_PRIVATE_KEY_CONTEXT_END = "-----END RSA PRIVATE KEY-----";

    public static final String PRIVATE_KEY_CONTEXT_BEGIN = "-----BEGIN PRIVATE KEY-----";
    public static final String PRIVATE_KEY_CONTEXT_END = "-----END PRIVATE KEY-----";

    /**
     * default created auth type.
     */
    public static Map<String, String> HMAC_ALGORITHM_MAP = new HashMap<String, String>();

    static {
        HMAC_ALGORITHM_MAP.put("hmac-sha1", "HmacSHA1");
        HMAC_ALGORITHM_MAP.put("hmac-sha256", "HmacSHA256");
        HMAC_ALGORITHM_MAP.put("hmac-sha384", "HmacSHA384");
        HMAC_ALGORITHM_MAP.put("hmac-sha512", "HmacSHA512");
    }

}
