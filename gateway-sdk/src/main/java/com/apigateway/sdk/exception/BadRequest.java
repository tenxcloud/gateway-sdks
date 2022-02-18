/*
 * Licensed Materials - Property of tenxcloud.com
 * (C) Copyright 2019 TenxCloud. All Rights Reserved.
 */

package com.apigateway.sdk.exception;

/**
 * @author hpy
 * @date 2021
 */
public class BadRequest extends RuntimeException{
    public BadRequest(String message, Exception exception) {
        super(message, exception);
    }
    public BadRequest(String message) {
        super(message);
    }
    public BadRequest() {
        super("Bad Request: please check your request body.");
    }
}
