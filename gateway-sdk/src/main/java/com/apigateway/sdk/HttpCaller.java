/*
 * Licensed Materials - Property of tenxcloud.com
 * (C) Copyright 2019 TenxCloud. All Rights Reserved.
 *
 * 2021/8/10 @author peiyun
 */
package com.apigateway.sdk;

import com.alibaba.fastjson.JSONObject;
import com.apigateway.sdk.constants.PluginConstant;
import com.apigateway.sdk.dto.AuthParam;
import com.apigateway.sdk.dto.ContentBody;
import com.apigateway.sdk.dto.ContentEncoding;
import com.apigateway.sdk.dto.HttpReturn;
import com.apigateway.sdk.exception.HttpCallerException;
import com.apigateway.sdk.utils.*;
import com.apigateway.sdk.utils.internel.HttpClientConnManager;
import com.apigateway.sdk.utils.internel.HttpClientHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HttpCaller
 * @author hpy
 * @date 2021
**/
@Slf4j
public class HttpCaller {

    public static final String DEFAULT_CHARSET = HTTP.UTF_8;
    public static final String GZIP = "gzip";

    protected static ThreadLocal<Boolean> toHttpRequest = new ThreadLocal<Boolean>();
    protected static ThreadLocal<HttpHost> proxyConfigThreadLocal = new ThreadLocal<HttpHost>();

    protected static final RequestConfig.Builder requestConfigBuilder = HttpClientConnManager.createConnBuilder();
    protected static final ThreadLocal<RequestConfig.Builder> requestConfigBuilderLocal = new ThreadLocal<RequestConfig.Builder>();

    public static final long TOTAL_FILE_SIZE;

    static {
        //默认15M
        TOTAL_FILE_SIZE = Integer.getInteger("httpAttachmentTotalMBSize", 64) * 1024 * 1024;
    }

    protected HttpCaller() {

    }

    /**
     * 为接下来的调用设置代理参数。 注意：本次设置只对本线程起作用
     *
     * @param hostname
     * @param port
     * @param scheme   如果设置为null时, scheme为 "http"
     */
    public static void setProxyHost(final String hostname, final int port, final String scheme) {
        proxyConfigThreadLocal.set(new HttpHost(hostname, port, scheme));
    }

    /**
     * 为接下来的调用设置新的连接参数。 注意：本次设置只对本线程起作用
     *
     * @param params
     */
    public static void setConnectionParams(Map<String, String> params) {
        if (params == null || params.size() == 0) {
            requestConfigBuilderLocal.set(requestConfigBuilder);
        } else {
            requestConfigBuilderLocal.set(HttpClientConnManager.createConnBuilder(params));
        }
    }

    private static RequestConfig getRequestConfig() {
        RequestConfig.Builder rcBuilder = null;
        if (requestConfigBuilderLocal.get() == null) {
            rcBuilder = requestConfigBuilder;
        } else {
            rcBuilder = requestConfigBuilderLocal.get();
        }

        rcBuilder.setProxy(proxyConfigThreadLocal.get());

        return rcBuilder.build();
    }

    /**
     * 把一个串的字符集从旧的的字符集到一个新的字符集合, 一个辅助方法，主要用于HTTP调用返回值的转换
     *
     * @param result         要装换的字符串
     * @param OldcharsetName 源编码方式
     * @param charsetName    目标编码方式
     * @return 返回转换后的字符串
     * @throws HttpCallerException
     */
    public static String changeCharset(String result, String OldcharsetName, String charsetName)
            throws HttpCallerException {
        if (result == null) {
            return result;
        }

        try {
            return new String(result.getBytes(OldcharsetName), charsetName);
        } catch (UnsupportedEncodingException e) {
            throw new HttpCallerException(e);
        }
    }

    /**
     * 把一个串的字符集从"ISO-8859-1"改变到"UTF-8", 一个辅助方法，主要用于HTTP调用返回值的转换
     *
     * @param result 要装换的字符串
     * @return 返回转换后的字符串
     * @throws HttpCallerException
     */
    public static String changeCharset(String result) throws HttpCallerException {
        return changeCharset(result, "ISO-8859-1", DEFAULT_CHARSET);
    }

    public static HttpReturn doGet(String requestURL, Map<String, String> headerParam, String contentType, Map<String, String> paramsMap, AuthParam authParam) throws HttpCallerException {
        setAuthorizationHeader(requestURL,headerParam,paramsMap,authParam);
        HttpParameters hp = HttpParameters.newBuilder().requestURL(requestURL).method("GET").putHeaderParamsMapAll(headerParam).putParamsMapAll(paramsMap).build();
        if(StringUtils.isNotBlank(contentType)){
            hp = HttpParameters.newBuilder().requestURL(requestURL).method("GET")
                    .putHeaderParamsMapAll(headerParam).putParamsMapAll(paramsMap).contentType(contentType).build();
        }
        return doGet(hp);
    }
    private static HttpReturn doGet(HttpParameters hp) throws HttpCallerException {
        final String requestURL = hp.getRequestUrl();
        Map<String, List<String>> paramsMapList = hp.getParamsMap();
        Map<String, String> directParamsMap = hp.getHeaderParamsMap();
        HttpReturn ret = new HttpReturn();
        long startT = System.currentTimeMillis();
        long initT = startT;
        String charset = hp.getContentType() == null || hp.getContentType().getCharset() == null ? null : hp.getContentType().getCharset().name();
        Map<String, List<String>> urlParamsMap = HttpClientHelper.parseUrlParamsMap(requestURL, charset, true);
        HttpClientHelper.mergeParamsList(urlParamsMap, paramsMapList);
        if (SdkLogger.isLoggable()) {
            SdkLogger.print("--+++ prepare params costs = " + (System.currentTimeMillis() - startT) + " ms ");
        }
        Map<String, String> headerParamsMap = new HashMap<>();
        if (hp.getContentType() != null) {
            headerParamsMap.put("Content-Type", hp.getContentType().toString());
        }
        String newRequestURL = HttpClientHelper.generateAsEncodeRequestUrl(requestURL, charset, urlParamsMap);
        HttpGet httpGet = new HttpGet(newRequestURL);
        httpGet.setConfig(getRequestConfig());
        // first step to set the direct http headers
        if (directParamsMap != null){
            HttpClientHelper.setHeaders(httpGet, directParamsMap);
        }

        String msg = null;
        try {
            ret = doSyncHttpReq(requestURL, httpGet, ret);
            return ret;
        } catch (HttpCallerException e) {
            msg = e.getMessage();
            throw e;
        } finally {
            log(hp, startT, requestURL, ret, msg);
            if (SdkLogger.isLoggable()) {
                SdkLogger.print("-- total = " + (System.currentTimeMillis() - initT) + " ms ");
            }
        }
    }

    public static HttpReturn doDelete(String requestURL, Map<String, String> headerParam, String contentType, Map<String, String> paramsMap, AuthParam authParam) throws HttpCallerException {
        setAuthorizationHeader(requestURL,headerParam,paramsMap,authParam);
        HttpParameters hp = HttpParameters.newBuilder().requestURL(requestURL).method("DELETE").putHeaderParamsMapAll(headerParam).putParamsMapAll(paramsMap).build();
        if(StringUtils.isNotBlank(contentType)){
            hp = HttpParameters.newBuilder().requestURL(requestURL).method("DELETE")
                    .putHeaderParamsMapAll(headerParam).putParamsMapAll(paramsMap).contentType(contentType).build();
        }
        return doDelete(hp);
    }

    private static HttpReturn doDelete(HttpParameters hp) throws HttpCallerException {
        final String requestURL = hp.getRequestUrl();
        Map<String, String> directParamsMap = hp.getHeaderParamsMap();
        Map<String, List<String>> paramsMapList = hp.getParamsMap();
        HttpReturn ret = new HttpReturn();
        long startT = System.currentTimeMillis();
        long initT = startT;
        String charset = hp.getContentType() == null || hp.getContentType().getCharset() == null ? null : hp.getContentType().getCharset().name();
        Map<String, List<String>> urlParamsMap = HttpClientHelper.parseUrlParamsMap(requestURL, charset, true);
        HttpClientHelper.mergeParamsList(urlParamsMap, paramsMapList);
        if (SdkLogger.isLoggable()) {
            SdkLogger.print("--+++ prepare params costs = " + (System.currentTimeMillis() - startT) + " ms ");
        }
        Map<String, String> headerParamsMap = new HashMap<>();
        if (hp.getContentType() != null) {
            headerParamsMap.put("Content-Type", hp.getContentType().toString());
        }
        String newRequestURL = HttpClientHelper.generateAsEncodeRequestUrl(requestURL, charset, urlParamsMap);
        HttpDelete httpDelete = new HttpDelete(newRequestURL);
        httpDelete.setConfig(getRequestConfig());
        // first step to set the direct http headers
        if (directParamsMap != null){
            HttpClientHelper.setHeaders(httpDelete, directParamsMap);
        }

        String msg = null;
        try {
            ret = doSyncHttpReq(requestURL, httpDelete, ret);
            return ret;
        } catch (HttpCallerException e) {
            msg = e.getMessage();
            throw e;
        } finally {
            log(hp, startT, requestURL, ret, msg);
            if (SdkLogger.isLoggable()) {
                SdkLogger.print("-- total = " + (System.currentTimeMillis() - initT) + " ms ");
            }
        }
    }

    public static HttpReturn doHead(String requestURL, Map<String, String> headerParam, String contentType, Map<String, String> paramsMap, AuthParam authParam) throws HttpCallerException {
        setAuthorizationHeader(requestURL,headerParam,paramsMap,authParam);
        HttpParameters hp = HttpParameters.newBuilder().requestURL(requestURL).method("HEAD").putHeaderParamsMapAll(headerParam).putParamsMapAll(paramsMap).build();
        if(StringUtils.isNotBlank(contentType)){
            hp = HttpParameters.newBuilder().requestURL(requestURL).method("HEAD")
                    .putHeaderParamsMapAll(headerParam).putParamsMapAll(paramsMap).contentType(contentType).build();
        }
        return doHead(hp);
    }

    private static HttpReturn doHead( HttpParameters hp) throws HttpCallerException {
        final String requestURL = hp.getRequestUrl();
        Map<String, String> directParamsMap = hp.getHeaderParamsMap();
        Map<String, List<String>> paramsMapList = hp.getParamsMap();
        HttpReturn ret = new HttpReturn();
        long startT = System.currentTimeMillis();
        long initT = startT;
        String charset = hp.getContentType() == null || hp.getContentType().getCharset() == null ? null : hp.getContentType().getCharset().name();
        Map<String, List<String>> urlParamsMap = HttpClientHelper.parseUrlParamsMap(requestURL, charset, true);
        HttpClientHelper.mergeParamsList(urlParamsMap, paramsMapList);
        if (SdkLogger.isLoggable()) {
            SdkLogger.print("--+++ prepare params costs = " + (System.currentTimeMillis() - startT) + " ms ");
        }
        Map<String, String> headerParamsMap = new HashMap<>();
        if (hp.getContentType() != null) {
            headerParamsMap.put("Content-Type", hp.getContentType().toString());
        }
        String newRequestURL = HttpClientHelper.generateAsEncodeRequestUrl(requestURL, charset, urlParamsMap);
        HttpHead httpHead = new HttpHead(newRequestURL);
        httpHead.setConfig(getRequestConfig());
        // first step to set the direct http headers
        if (directParamsMap != null){
            HttpClientHelper.setHeaders(httpHead, directParamsMap);
        }

        String msg = null;
        try {
            ret = doSyncHttpReq(requestURL, httpHead, ret);
            return ret;
        } catch (HttpCallerException e) {
            msg = e.getMessage();
            throw e;
        } finally {
            log(hp, startT, requestURL, ret, msg);
            if (SdkLogger.isLoggable()) {
                SdkLogger.print("-- total = " + (System.currentTimeMillis() - initT) + " ms ");
            }
        }
    }

    public static HttpReturn doOptions(String requestURL, Map<String, String> headerParam, String contentType, Map<String, String> paramsMap, AuthParam authParam) throws HttpCallerException {
        setAuthorizationHeader(requestURL,headerParam,paramsMap,authParam);
        HttpParameters hp = HttpParameters.newBuilder().requestURL(requestURL).method("OPTIONS").putHeaderParamsMapAll(headerParam).putParamsMapAll(paramsMap).build();
        if(StringUtils.isNotBlank(contentType)){
            hp = HttpParameters.newBuilder().requestURL(requestURL).method("OPTIONS")
                    .putHeaderParamsMapAll(headerParam).putParamsMapAll(paramsMap).contentType(contentType).build();
        }
        return doOptions(hp);
    }
    private static HttpReturn doOptions(HttpParameters hp) throws HttpCallerException {
        final String requestURL = hp.getRequestUrl();
        Map<String, String> directParamsMap = hp.getHeaderParamsMap();
        Map<String, List<String>> paramsMapList = hp.getParamsMap();
        HttpReturn ret = new HttpReturn();
        long startT = System.currentTimeMillis();
        long initT = startT;
        String charset = hp.getContentType() == null || hp.getContentType().getCharset() == null ? null : hp.getContentType().getCharset().name();
        Map<String, List<String>> urlParamsMap = HttpClientHelper.parseUrlParamsMap(requestURL, charset, true);
        HttpClientHelper.mergeParamsList(urlParamsMap, paramsMapList);
        if (SdkLogger.isLoggable()) {
            SdkLogger.print("--+++ prepare params costs = " + (System.currentTimeMillis() - startT) + " ms ");
        }
        Map<String, String> headerParamsMap = new HashMap<>();
        if (hp.getContentType() != null) {
            headerParamsMap.put("Content-Type", hp.getContentType().toString());
        }
        String newRequestURL = HttpClientHelper.generateAsEncodeRequestUrl(requestURL, charset, urlParamsMap);
        HttpOptions httpOptions = new HttpOptions(newRequestURL);
        httpOptions.setConfig(getRequestConfig());
        // first step to set the direct http headers
        if (directParamsMap != null){
            HttpClientHelper.setHeaders(httpOptions, directParamsMap);
        }
        String msg = null;
        try {
            ret = doSyncHttpReq(requestURL, httpOptions, ret);
            return ret;
        } catch (HttpCallerException e) {
            msg = e.getMessage();
            throw e;
        } finally {
            log(hp, startT, requestURL, ret, msg);
            if (SdkLogger.isLoggable()) {
                SdkLogger.print("-- total = " + (System.currentTimeMillis() - initT) + " ms ");
            }
        }
    }

    public static HttpReturn doPost(String requestURL, Map<String, String> headerParam, String contentType, Map<String, String> paramsMap, AuthParam authParam) throws HttpCallerException{
        setAuthorizationHeader(requestURL,headerParam,paramsMap,authParam);
        HttpParameters hp = HttpParameters.newBuilder().requestURL(requestURL).method("POST").putHeaderParamsMapAll(headerParam).putParamsMapAll(paramsMap).build();
        if(StringUtils.isNotBlank(contentType)){
            hp = HttpParameters.newBuilder().requestURL(requestURL).method("POST")
                    .putHeaderParamsMapAll(headerParam).putParamsMapAll(paramsMap).contentType(contentType).build();
        }
        return doPost(hp);
    }

    public static HttpReturn doPost(String requestURL, Map<String, String> headerParam, String contentType, Map<String, String> paramsMap,
                                    String fileName, InputStream inputStream, ContentEncoding contentEncoding, AuthParam authParam) throws HttpCallerException{
        setAuthorizationHeader(requestURL,headerParam,paramsMap,authParam);
        HttpParameters hp = HttpParameters.newBuilder().requestURL(requestURL).method("POST").putHeaderParamsMapAll(headerParam)
                .addAttachFile(fileName,inputStream,contentEncoding).putParamsMapAll(paramsMap).build();
        if(StringUtils.isNotBlank(contentType)){
            hp = HttpParameters.newBuilder().requestURL(requestURL).method("POST")
                    .putHeaderParamsMapAll(headerParam).addAttachFile(fileName,inputStream,contentEncoding).putParamsMapAll(paramsMap).contentType(contentType).build();
        }
        return doPost(hp);
    }

    private static void setAuthorizationHeader(String requestURL,Map<String, String> headerParam, Map<String, String> paramsMap, AuthParam authParam) {
        headerParam = Optional.ofNullable(headerParam).orElse(new HashMap<>()); //可能需要初使用化 headers
        if(StringUtils.isBlank((String) headerParam.getOrDefault("Authorization", null))){
            // 为了修复前端没有传入 url 参数情况
            if(null != authParam && null == authParam.url){
                authParam.url = requestURL;
            }
            if(null != authParam && !PluginConstant.AUTH_TYPE_NONE.equals(authParam.getAuthType())){
                TokenHelper.setAuthorizationHeader(authParam, headerParam,paramsMap,"");
            }
        }
    }

    public static HttpReturn doPost(String requestURL, Map<String, String> headerParam, String contentType, ContentBody cb, AuthParam authParam) throws HttpCallerException{
        setAuthorizationHeader(requestURL,headerParam,cb,authParam);
        HttpParameters hp = HttpParameters.newBuilder().requestURL(requestURL).method("POST").putHeaderParamsMapAll(headerParam).contentBody(cb).build();
        if(StringUtils.isNotBlank(contentType)){
            hp = HttpParameters.newBuilder().requestURL(requestURL).method("POST").putHeaderParamsMapAll(headerParam).contentBody(cb).contentType(contentType).build();
        }
        return doPost(hp);
    }

    private static void setAuthorizationHeader(String requestURL,Map<String, String> headerParam, ContentBody cb, AuthParam authParam) {
        headerParam = Optional.ofNullable(headerParam).orElse(new HashMap<>()); //可能需要初使用化 headers
        if(StringUtils.isBlank((String) headerParam.getOrDefault("Authorization", null))){
            // 为了修复前端没有传入 url 参数情况
            if(null != authParam && null == authParam.url){
                authParam.url = requestURL;
            }
            if(null != authParam && !PluginConstant.AUTH_TYPE_NONE.equals(authParam.getAuthType())){
                TokenHelper.setAuthorizationHeader(authParam, headerParam,null,cb.getStrContentBody());
            }
        }
    }

    private static HttpReturn doPost(HttpParameters hp) throws HttpCallerException{
        final String requestURL = hp.getRequestUrl();
        Map<String, List<String>> paramsMapList = hp.getParamsMap();
        ContentBody cb = hp.getContentBody();
        Map<String, String> directParamsMap = hp.getHeaderParamsMap();
        HttpReturn ret = new HttpReturn();
        long startT = System.currentTimeMillis();
        long initT = startT;
        String charset = hp.getContentType() == null || hp.getContentType().getCharset() == null ? null : hp.getContentType().getCharset().name();
        Map<String, List<String>> urlParamsMap = HttpClientHelper.parseUrlParamsMap(requestURL, charset, true);
        String newRequestURL = HttpClientHelper.generateAsEncodeRequestUrl(requestURL, charset, urlParamsMap);
        HttpClientHelper.mergeParamsList(urlParamsMap, paramsMapList);
        Map<String, String> headerParamsMap = new HashMap<>();
        if (hp.getContentType() != null) {
            headerParamsMap.put("Content-Type", hp.getContentType().toString());
        }
        HttpPost httpPost = HttpClientHelper.createPost(newRequestURL, paramsMapList, headerParamsMap, cb, hp.getAttachFileMap(), hp.getContentEncoding(), hp.getContentType());
        if (directParamsMap != null){
            HttpClientHelper.setHeaders(httpPost, directParamsMap);
        }
        httpPost.setConfig(getRequestConfig());
        if (SdkLogger.isLoggable()) {
            SdkLogger.print("--+++ prepare params costs = " + (System.currentTimeMillis() - startT) + " ms ");
        }
        String msg = null;
        try {
            ret = doSyncHttpReq(requestURL, httpPost, ret);
            return ret;
        } catch (HttpCallerException e) {
            msg = e.getMessage();
            throw e;
        } finally {
            log(hp, startT, requestURL, ret, msg);
            if (SdkLogger.isLoggable()) {
                SdkLogger.print("-- total = " + (System.currentTimeMillis() - initT) + " ms ");
            }
        }
    }

    public static HttpReturn doPatch(String requestURL, Map<String, String> headerParam, String contentType, Map<String, String> paramsMap, AuthParam authParam) throws HttpCallerException{
        setAuthorizationHeader(requestURL,headerParam,paramsMap,authParam);
        HttpParameters hp = HttpParameters.newBuilder().requestURL(requestURL).method("PATCH").putHeaderParamsMapAll(headerParam).putParamsMapAll(paramsMap).build();
        if(StringUtils.isNotBlank(contentType)){
            hp = HttpParameters.newBuilder().requestURL(requestURL).method("PATCH")
                    .putHeaderParamsMapAll(headerParam).putParamsMapAll(paramsMap).contentType(contentType).build();
        }
        return doPatch(hp);
    }

    public static HttpReturn doPatch(String requestURL, Map<String, String> headerParam, String contentType, ContentBody cb, AuthParam authParam) throws HttpCallerException{
        setAuthorizationHeader(requestURL,headerParam,cb,authParam);
        HttpParameters hp = HttpParameters.newBuilder().requestURL(requestURL).method("PATCH").putHeaderParamsMapAll(headerParam).contentBody(cb).build();
        if(StringUtils.isNotBlank(contentType)){
            hp = HttpParameters.newBuilder().requestURL(requestURL).method("PATCH").putHeaderParamsMapAll(headerParam).contentBody(cb).contentType(contentType).build();
        }
        return doPatch(hp);
    }

    private static HttpReturn doPatch(HttpParameters hp) throws HttpCallerException{
        final String requestURL = hp.getRequestUrl();
        Map<String, List<String>> paramsMapList = hp.getParamsMap();
        ContentBody cb = hp.getContentBody();
        Map<String, String> directParamsMap = hp.getHeaderParamsMap();
        HttpReturn ret = new HttpReturn();
        long startT = System.currentTimeMillis();
        long initT = startT;
        String charset = hp.getContentType() == null || hp.getContentType().getCharset() == null ? null : hp.getContentType().getCharset().name();
        Map<String, List<String>> urlParamsMap = HttpClientHelper.parseUrlParamsMap(requestURL, charset, true);
        String newRequestURL = HttpClientHelper.generateAsEncodeRequestUrl(requestURL, charset, urlParamsMap);
        HttpClientHelper.mergeParamsList(urlParamsMap, paramsMapList);
        if (SdkLogger.isLoggable()) {
            SdkLogger.print("--+++ prepare params costs = " + (System.currentTimeMillis() - startT) + " ms ");
        }
        Map<String, String> headerParamsMap = new HashMap<>();
        if (hp.getContentType() != null) {
            headerParamsMap.put("Content-Type", hp.getContentType().toString());
        }
        HttpPatch httpPatch = HttpClientHelper.createPatch(newRequestURL, paramsMapList, headerParamsMap, cb, hp.getAttachFileMap(), hp.getContentEncoding(), hp.getContentType());
        if (directParamsMap != null){
            HttpClientHelper.setHeaders(httpPatch, directParamsMap);
        }

        httpPatch.setConfig(getRequestConfig());
        String msg = null;
        try {
            ret = doSyncHttpReq(requestURL, httpPatch, ret);
            return ret;
        } catch (HttpCallerException e) {
            msg = e.getMessage();
            throw e;
        } finally {
            log(hp, startT, requestURL, ret, msg);
            if (SdkLogger.isLoggable()) {
                SdkLogger.print("-- total = " + (System.currentTimeMillis() - initT) + " ms ");
            }
        }
    }

    public static HttpReturn doPut(String requestURL, Map<String, String> headerParam, String contentType, Map<String, String> paramsMap, AuthParam authParam) throws HttpCallerException{
        setAuthorizationHeader(requestURL,headerParam,paramsMap,authParam);
        HttpParameters hp = HttpParameters.newBuilder().requestURL(requestURL).method("PUT").putHeaderParamsMapAll(headerParam).putParamsMapAll(paramsMap).build();
        if(StringUtils.isNotBlank(contentType)){
            hp = HttpParameters.newBuilder().requestURL(requestURL).method("PUT").putHeaderParamsMapAll(headerParam).putParamsMapAll(paramsMap).contentType(contentType).build();
        }
        return doPut(hp);
    }

    public static HttpReturn doPut(String requestURL, Map<String, String> headerParam, String contentType, ContentBody cb, AuthParam authParam) throws HttpCallerException{
        setAuthorizationHeader(requestURL,headerParam,cb,authParam);
        HttpParameters hp = HttpParameters.newBuilder().requestURL(requestURL).method("PUT").putHeaderParamsMapAll(headerParam).contentBody(cb).build();
        if(StringUtils.isNotBlank(contentType)){
            hp = HttpParameters.newBuilder().requestURL(requestURL).method("PUT").putHeaderParamsMapAll(headerParam).contentBody(cb).contentType(contentType).build();
        }
        return doPut(hp);
    }

    public static HttpReturn doPut(HttpParameters hp) throws HttpCallerException{
        final String requestURL = hp.getRequestUrl();
        Map<String, List<String>> paramsMapList = hp.getParamsMap();
        ContentBody cb = hp.getContentBody();
        Map<String, String> directParamsMap = hp.getHeaderParamsMap();
        HttpReturn ret = new HttpReturn();
        long startT = System.currentTimeMillis();
        long initT = startT;
        String charset = hp.getContentType() == null || hp.getContentType().getCharset() == null ? null : hp.getContentType().getCharset().name();
        Map<String, List<String>> urlParamsMap = HttpClientHelper.parseUrlParamsMap(requestURL, charset, true);
        String newRequestURL = HttpClientHelper.generateAsEncodeRequestUrl(requestURL, charset, urlParamsMap);
        HttpClientHelper.mergeParamsList(urlParamsMap, paramsMapList);
        if (SdkLogger.isLoggable()) {
            SdkLogger.print("--+++ prepare params costs = " + (System.currentTimeMillis() - startT) + " ms ");
        }
        Map<String, String> headerParamsMap = new HashMap<>();
        if (hp.getContentType() != null) {
            headerParamsMap.put("Content-Type", hp.getContentType().toString());
        }
        HttpPut httpPut = HttpClientHelper.createPut(newRequestURL, paramsMapList, headerParamsMap, cb, hp.getAttachFileMap(), hp.getContentEncoding(), hp.getContentType());
        if (directParamsMap != null){
            HttpClientHelper.setHeaders(httpPut, directParamsMap);
        }

        httpPut.setConfig(getRequestConfig());
        String msg = null;
        try {
            ret = doSyncHttpReq(requestURL, httpPut, ret);
            return ret;
        } catch (HttpCallerException e) {
            msg = e.getMessage();
            throw e;
        } finally {
            log(hp, startT, requestURL, ret, msg);
            if (SdkLogger.isLoggable()) {
                SdkLogger.print("-- total = " + (System.currentTimeMillis() - initT) + " ms ");
            }
        }
    }

    /**
     * 以GET的方式发送URL请求
     *
     * @param requestURL 请求的服务URL
     * @return 返回的JSON串
     * @throws HttpCallerException
     */
    public static String doGet(String requestURL) throws HttpCallerException {
        HttpGet httpGet = new HttpGet(requestURL);
        httpGet.setConfig(getRequestConfig());
        HttpClientHelper.printDebugInfo("requestURL=" + requestURL);
        return doSyncHttpReq(requestURL, httpGet, null).getResponseStr();
    }

    private static HttpReturn doSyncHttpReq(String requestURL, HttpRequestBase httpRequestBase, final HttpReturn ret) throws HttpCallerException {
        if (SdkLogger.isLoggable()) {
            SdkLogger.print("doSyncHttpReq ");
        }
        HttpReturn rret = ret;
        if (ret == null) {
            rret = new HttpReturn();
        }

        long startT = System.currentTimeMillis();
        CloseableHttpResponse response = null;
        CloseableHttpClient httpClient = null;
        if (HttpClientConnManager.HTTP_CLIENT != null) {
            httpClient = HttpClientConnManager.HTTP_CLIENT;
        } else {
            httpClient = createSyncHttpClient(requestURL);
        }
        if (SdkLogger.isLoggable()) {
            SdkLogger.print("--+++ get httpclient costs = " + (System.currentTimeMillis() - startT) + " ms ");
            startT = System.currentTimeMillis();
        }
        try {
            try {
                response = httpClient.execute(httpRequestBase);
                rret.httpCode = response.getStatusLine().getStatusCode();
                rret.responseHttpStatus = response.getStatusLine().toString();
                rret.respHttpHeaderMap = HttpClientHelper.fetchResHeaderMap(response);
                String method = httpRequestBase.getMethod();
                if(!"HEAD".equals(method)){
                    fetchResponseBody(response, rret);
                }
                return rret;
            } finally {
                if (response != null) {
                    response.close();
                }
                //don't close the client for reusing
                if (HttpClientConnManager.HTTP_CLIENT == null) {
                    httpClient.close();
                }
                if (SdkLogger.isLoggable()) {
                    SdkLogger.print("-- http req & resp time = " + (System.currentTimeMillis() - startT) + " ms ");
                }
            }
        } catch (Exception e) {
            throw new HttpCallerException(e);
        }
    }

    static private void fetchResponseBody(HttpResponse response, HttpReturn rret) throws IOException {
        HttpEntity responseEntity = response.getEntity();
        Header header = responseEntity.getContentType();
        if (header == null) {
            rret.responseBytes = EntityUtils.toByteArray(responseEntity);
            rret.response = new String(rret.responseBytes, HTTP.UTF_8);
            return;
        }
        String contentType = header.getValue();
        if (contentType == null || contentType.equals("")) {
            rret.responseBytes = EntityUtils.toByteArray(responseEntity);
            rret.response = new String(rret.responseBytes, HTTP.UTF_8);
            return;
        }

        contentType = contentType.toLowerCase();
        if (contentType.startsWith("text") || contentType.contains("json") || contentType.contains("xml")) {
            if (contentType.contains("text/csv")) {
                rret.responseBytes = EntityUtils.toByteArray(responseEntity);
            } else {
                rret.response = EntityUtils.toString(responseEntity);
            }
        } else {
            rret.responseBytes = EntityUtils.toByteArray(responseEntity);
        }
    }

    public static HttpReturn invokeReturn(HttpParameters hp) throws HttpCallerException {
        if (hp == null)
            throw new IllegalArgumentException("null parameter!");
        HttpClientHelper.printDebugInfo("-- httpParameters=" + hp.toString());
        switch (hp.getMethod().toUpperCase()) {
            case "GET":
                setAuthorizationHeader(hp.getRequestUrl(), hp.getHeaderParamsMap(), getParamsMap(hp.getParamsMap()), hp.getAuthParam());
                return doGet(hp);
            case "POST":
                if (!hp.getParamsMap().isEmpty()) {
                    setAuthorizationHeader(hp.getRequestUrl(), hp.getHeaderParamsMap(), getParamsMap(hp.getParamsMap()), hp.getAuthParam());
                } else if (null != hp.getContentBody()) {
                    setAuthorizationHeader(hp.getRequestUrl(), hp.getHeaderParamsMap(), hp.getContentBody(), hp.getAuthParam());
                } else if(StringUtils.isNotBlank(hp.getAuthParam().getAuthType())) {
                    setAuthorizationHeader(hp.getRequestUrl(), hp.getHeaderParamsMap(), getParamsMap(hp.getParamsMap()), hp.getAuthParam());
                }
                return doPost(hp);
            case "DELETE":
                setAuthorizationHeader(hp.getRequestUrl(), hp.getHeaderParamsMap(), getParamsMap(hp.getParamsMap()), hp.getAuthParam());
                return doDelete(hp);
            case "PUT":
                if (!hp.getParamsMap().isEmpty()) {
                    setAuthorizationHeader(hp.getRequestUrl(), hp.getHeaderParamsMap(), getParamsMap(hp.getParamsMap()), hp.getAuthParam());
                } else if (null != hp.getContentBody()) {
                    setAuthorizationHeader(hp.getRequestUrl(), hp.getHeaderParamsMap(), hp.getContentBody(), hp.getAuthParam());
                } else if(StringUtils.isNotBlank(hp.getAuthParam().getAuthType())) {
                    setAuthorizationHeader(hp.getRequestUrl(), hp.getHeaderParamsMap(), getParamsMap(hp.getParamsMap()), hp.getAuthParam());
                }
                return doPut(hp);
            case "PATCH":
                if (!hp.getParamsMap().isEmpty()) {
                    setAuthorizationHeader(hp.getRequestUrl(), hp.getHeaderParamsMap(), getParamsMap(hp.getParamsMap()), hp.getAuthParam());
                } else if (null != hp.getContentBody()) {
                    setAuthorizationHeader(hp.getRequestUrl(), hp.getHeaderParamsMap(), hp.getContentBody(), hp.getAuthParam());
                } else if(StringUtils.isNotBlank(hp.getAuthParam().getAuthType())) {
                    setAuthorizationHeader(hp.getRequestUrl(), hp.getHeaderParamsMap(), getParamsMap(hp.getParamsMap()), hp.getAuthParam());
                }
                return doPatch(hp);
            case "HEAD":
                setAuthorizationHeader(hp.getRequestUrl(), hp.getHeaderParamsMap(), getParamsMap(hp.getParamsMap()), hp.getAuthParam());
                return doHead(hp);
            case "OPTIONS":
                setAuthorizationHeader(hp.getRequestUrl(), hp.getHeaderParamsMap(), getParamsMap(hp.getParamsMap()), hp.getAuthParam());
                return doOptions(hp);
            default:
                return errorRequest(hp);
        }
    }

    private static CloseableHttpClient createSyncHttpClient(String requestURL) throws HttpCallerException {
        CloseableHttpClient httpClient = null;
        if (isSSLProtocol(requestURL)) {
            try {
                httpClient = HttpClients.custom().setSslcontext(new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                    public boolean isTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
                        return true;
                    }
                }).build()).setSSLHostnameVerifier(new org.apache.http.conn.ssl.NoopHostnameVerifier()).build();
            } catch (KeyManagementException e) {
                throw new HttpCallerException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new HttpCallerException(e);
            } catch (KeyStoreException e) {
                throw new HttpCallerException(e);
            }
        } else {
            httpClient = HttpClients.createDefault();
        }

        return httpClient;
    }

    private static boolean isSSLProtocol(String requestUrl) {
        if (requestUrl == null)
            return false;

        if (requestUrl.trim().toLowerCase().startsWith("https://")) {
            return true;
        }

        return false;
    }

    /**
     * 一个便利方法，读取一个文件并把其内容转换为 byte[]
     *
     * @param file 文件的全路径， 最大支持的上传文件的尺寸为10M
     * @return
     * @throws HttpCallerException
     */
    //TODO: remove this unrelated method out of the class
    public static byte[] readFileAsByteArray(String file) throws HttpCallerException {
        return readFile(new File(file));
    }


    public static byte[] readFile(File file) {
        if (file.exists() && file.isFile() && file.canRead()) {
            try {
                return readInputStream(new FileInputStream(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("bad file to read:" + file);
        }
    }

    public static byte[] readInputStream(InputStream inputStream) {
        if (inputStream != null) {
            ByteArrayOutputStream bos = null;
            try {
                bos = new ByteArrayOutputStream();
                byte[] b = new byte[1024];
                int n;
                while ((n = inputStream.read(b)) != -1) {
                    bos.write(b, 0, n);
                }
                return bos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    if (inputStream != null)
                        inputStream.close();
                    if (bos != null)
                        bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            throw new IllegalArgumentException("inputSteam must no null");
        }
    }

    private static void log(HttpParameters hp, long startTime, String requestUrl, HttpReturn httpReturn, String msg) {
        long endTime = System.currentTimeMillis();

        Map<String, String> headers = hp.getHeaderParamsMap();
        try {
            int qidx = requestUrl.indexOf("?");
            String url = qidx > -1 ? requestUrl.substring(0, qidx) : requestUrl;

            int cidx = url.indexOf(":");
            int pidx = url.indexOf(":", cidx + 3);
            if (pidx < 0) {
                pidx = url.indexOf("/", cidx + 3);
            }
            String dest = url.substring(cidx + 3, pidx);
            LogUtils.info("{}|{}|{}|{}|{}|{}|{}|{}|{}|{}", new Object[]{startTime, endTime, endTime - startTime
                    , "HTTP", IPUtils.getLocalHostIP(), dest
                    ,  hp.getMethod()
                    , url, httpReturn.httpCode, httpReturn.responseHttpStatus});
        } catch (Throwable e) {
            LogUtils.exception("invoke error", e);
        }
    }

    private static HttpReturn errorRequest(HttpParameters hp) {
        HttpReturn httpReturn = new HttpReturn(hp.getRequestUrl(), hp.getHeaderParamsMap(), "error method");
        return httpReturn;
    }

    private static Map<String,String> getParamsMap(Map<String, List<String>> map) {
        Map<String,String> paramMap = new HashMap<>();
        if (map != null) {
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                paramMap.put(entry.getKey(), JSONObject.toJSONString(entry.getValue()));
            }
        } else {
            throw new IllegalArgumentException("empty map!!");
        }
        return paramMap;
    }

    public static void main(String[] args) {

    }
}
