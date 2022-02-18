package com.apigateway.sdk.dto;

import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;

import java.util.HashMap;
import java.util.Map;

import static com.apigateway.sdk.HttpCaller.DEFAULT_CHARSET;

/**
 * Http Return 对象 包含调用的返回结果：
 * <pre>
 *
 *
 * 1. response                调用的返回值
 * 2. respHttpHeaderMap       返回的http headers
 * 3. responseHttpStatus      返回的http状态
 * 4. responseBytes           调用的字节
 *
 * </pre>
 *
 * @author hpy
 * @date 2021
 */
public class HttpReturn {
    private String requestUrl;//可能为空，目前只使用在生成http请求消息。
    public int httpCode;//请求http状态码
    public String responseHttpStatus;
    public String response;
    public byte[] responseBytes;

    public Map<String, String> respHttpHeaderMap;
    //public Map<String, String> diagnosticInfo = new HashMap<String, String>(); //定义成Map类型，方便增减新的诊断项
    //public boolean diagnosticFlag;

    public HttpReturn() {
    }

    /**
     * 根据请求消息，生成http GET请求内容
     */
    public HttpReturn(String requestUrl, Map<String, String> directParamsMap, Map<String, String> headerParamsMap) {
        this(requestUrl, directParamsMap, headerParamsMap, null);
    }

    /**
     * 根据请求消息，生成http GET请求内容。
     */
    public HttpReturn(String requestUrl, Map<String, String> headerParamsMap, String body) {
        this.requestUrl = requestUrl;
        this.respHttpHeaderMap = new HashMap<String, String>();
        respHttpHeaderMap.putAll(headerParamsMap);
        this.response = body;
    }
    /**
     * 根据请求消息，生成http POST请求内容。
     */
    public HttpReturn(String requestUrl, Map<String, String> directParamsMap, Map<String, String> headerParamsMap, String body) {
        this.requestUrl = requestUrl;
        this.respHttpHeaderMap = new HashMap<String, String>();
        respHttpHeaderMap.putAll(directParamsMap);
        respHttpHeaderMap.putAll(headerParamsMap);
        this.response = body;
    }

    public HttpReturn(String response) {
        this.response = response;
    }

    public String getRequestUrl() {
        return requestUrl;
    }


    public Map<String, String> getHeaderMap() {
        return respHttpHeaderMap;
    }

    public String getBodyStr() {
        return response;
    }


    /**
     * 不管响应类型是文本还是二进制，始终转换为string输出
     */
    public String getResponseStr() {
        if (response != null) {
            return response;
        } else if (responseBytes != null) {
            try {
                String charset = DEFAULT_CHARSET;//没有返回contentType，使用默认值，以便兼容历史http2ws无返回contentType的场景。
                if (respHttpHeaderMap != null) {
                    String contentTypeStr = respHttpHeaderMap.get(HTTP.CONTENT_TYPE);
                    if (contentTypeStr != null && contentTypeStr.equals("") == false) {
                        ContentType contentType = ContentType.parse(contentTypeStr);
                        if (contentType != null && contentType.getCharset() != null) {
                            charset = contentType.getCharset().name();
                        }
                    }
                }
                return new String(responseBytes, charset);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return null;
    }

    public byte[] getResponseBytes() {
        return responseBytes;
    }
}
