package com.apigateway.sdk.utils;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hpy
 * @date 2021
 */
public class HttpUtil {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);

    private static final CloseableHttpClient httpclient = HttpClients.createDefault();


    public static String sendPost(String url, String json) throws IOException {
        logger.info("request, url: {}, body: {}", url, json);

        StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);

        HttpPost httppost = new HttpPost(url);
        httppost.setEntity(entity);
        CloseableHttpResponse response = httpclient.execute(httppost);
        HttpEntity entityRes = response.getEntity();

        String ret = EntityUtils.toString(entityRes);

        logger.info("get response: {}", ret);

        return ret;
    }


    /**
     * 发送HttpPost请求，参数为map
     *
     * @param url
     * @param map
     * @return
     */
    public static String sendPost(String url, Map<String, String> map) throws Exception {
        List<NameValuePair> formParams = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            formParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, Consts.UTF_8);
        HttpPost httppost = new HttpPost(url);
        httppost.setEntity(entity);
        if (url.startsWith("https")) {
            HttpClient httpClient = new SSLClient();
            HttpResponse response = httpClient.execute(httppost);
            HttpEntity entityRes = response.getEntity();
            return EntityUtils.toString(entityRes);
        } else {
            CloseableHttpResponse response = httpclient.execute(httppost);
            HttpEntity entityRes = response.getEntity();
            return EntityUtils.toString(entityRes);
        }

    }
    /**
     * 发送HttpGet请求
     *
     * @param url
     * @return
     */
    public static String sendGet(String url) throws IOException {

        HttpGet httpget = new HttpGet(url);
        CloseableHttpResponse response = httpclient.execute(httpget);

        String result = null;
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            result = EntityUtils.toString(entity);
        }
        return result;
    }

    /**
     * 发送HttpPut请求
     *
     * @param url
     * @param map
     * @return
     * @throws IOException
     */
    public static String sendPut(String url, Map<String, String> map) throws IOException {

        HttpPut httpPut = new HttpPut(url);

        List<NameValuePair> formParams = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            formParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, Consts.UTF_8);
        httpPut.setEntity(entity);
        CloseableHttpResponse response = httpclient.execute(httpPut);
        HttpEntity entityRes = response.getEntity();
        return EntityUtils.toString(entityRes);
    }

    public static Map<String, Object> sendGetAndGetCode(String url, List<Map<String, String>> header) throws IOException, Exception {
        Map<String, Object> result = new HashMap<>(3);
        HttpGet httpget = new HttpGet(url);
        if (null != header && header.size() > 0) {
            for (Map<String, String> headerMap : header) {
                String param = headerMap.get("param");
                String value = headerMap.get("value");
                httpget.setHeader(param, value);
            }
        }
        if (url.startsWith("https")) {
            HttpClient httpClient = new SSLClient();
            HttpResponse response = httpClient.execute(httpget);
            HttpEntity entityRes = response.getEntity();
            result.put("code", response.getStatusLine().getStatusCode());
            result.put("data", EntityUtils.toString(entityRes));
            result.put("contentType", response.getEntity().getContentType());
            return result;
        } else {
            CloseableHttpResponse response = httpclient.execute(httpget);
            HttpEntity entityRes = response.getEntity();
            result.put("code", response.getStatusLine().getStatusCode());
            result.put("data", EntityUtils.toString(entityRes));
            result.put("contentType", response.getEntity().getContentType());
            return result;
        }
    }

}
