package com.apigateway.sdk.exception;

/**
 * 调用HttpCaller产生的异常
 * 
 * @author hpy
 * @date 2021
 */
public class HttpCallerException extends Exception {
	public HttpCallerException(Exception e) {
		super(e);
	}
	
	public HttpCallerException(String msg, Exception e) {
		super(msg, e);
	}
	
	public HttpCallerException(String msg) {
		super(msg);
	}
}
