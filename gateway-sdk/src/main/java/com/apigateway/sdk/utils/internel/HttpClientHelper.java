package com.apigateway.sdk.utils.internel;

import com.apigateway.sdk.HttpCaller;
import com.apigateway.sdk.dto.ContentBody;
import com.apigateway.sdk.dto.ContentEncoding;
import com.apigateway.sdk.HttpParameters;
import com.apigateway.sdk.exception.HttpCallerException;
import com.apigateway.sdk.utils.GZipUtils;
import com.apigateway.sdk.utils.SdkLogger;
import org.apache.http.*;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.Map.Entry;

import static com.apigateway.sdk.HttpCaller.GZIP;

/**
 * @author hpy
 * @date 2021
 */
public class HttpClientHelper {
    public static void printDebugInfo(String msg) {
        if (SdkLogger.isLoggable())
            SdkLogger.print(msg);
    }

    public static Map<String, List<String>> convertStrMap2ListStrMap(Map<String, String> paramsMap) {
        if (paramsMap == null) {
            return null;
        }
        Map<String, List<String>> stringListMap = new HashMap<String, List<String>>((int) (paramsMap.size() * 1.5));
        for (Entry<String, String> entry : paramsMap.entrySet()) {
            stringListMap.put(entry.getKey(), Arrays.asList(entry.getValue()));
        }
        return stringListMap;
    }

    public static void mergeParams(Map<String, List<String>> urlParamsMap, Map<String, String> paramsMap) throws HttpCallerException {
        mergeParamsList(urlParamsMap, convertStrMap2ListStrMap(paramsMap));
    }

    public static void mergeParamsList(Map<String, List<String>> urlParamsMap, Map<String, List<String>> paramsMap) throws HttpCallerException {
        if (paramsMap != null) {
            for (Entry<String, List<String>> kv : paramsMap.entrySet()) {
                urlParamsMap.put(kv.getKey(), kv.getValue());
            }
        }
    }

    public static String trimWhiteSpaces(String value) {
        if (value == null) return value;

        return value.trim();
    }

    public static String trimUrl(String requestURL) {
        int pos = requestURL.indexOf("?");
        String ret = requestURL;

        if (pos >= 0) {
            ret = requestURL.substring(0, pos);
        }

        return ret;
    }

    private static String decodeValue(String key, String value, String charset, boolean decodeFlag) throws HttpCallerException {
        if (decodeFlag) {
            if (value == null) {
                throw new HttpCallerException("bad params, the value for key {" + key + "} is null!");
            }
            return urlDecoding(value, charset == null ? "UTF-8" : charset);
        }

        return value;
    }

    /**
     * Parse URL parameters to Map, url-decode all values
     *
     * @param requestURL
     * @return
     * @throws HttpCallerException
     */
    public static Map<String, List<String>> parseUrlParamsMap(String requestURL, String charset, boolean decodeFlag) throws HttpCallerException {
        boolean questionMarkFlag = requestURL.contains("?");
        Map<String, List<String>> urlParamsMap = new HashMap<String, List<String>>();
        String key;
        String value;
        if (questionMarkFlag) {
            // parse params
            int pos = requestURL.indexOf("?");
            String paramStr = requestURL.substring(pos + 1);
            // requestURL = requestURL.substring(0, pos);
            // The caller needs to ensure the url-encode for a parameter value!!
            String[] params = paramStr.split("&");
            for (String param : params) {
                pos = param.indexOf("=");
                if (pos <= 0) {
                    throw new HttpCallerException("bad request URL, url params error:" + requestURL);
                }
                key = decodeValue("", param.substring(0, pos), charset, decodeFlag);
                value = param.substring(pos + 1);
                List<String> values = urlParamsMap.get(key);
                if (values == null) {
                    values = new ArrayList<String>();
                }
                values.add(decodeValue(key, value, charset, decodeFlag));
                urlParamsMap.put(key, values);
            }
        }

        return urlParamsMap;
    }
//
//	public static StringEntity jsonProcess(Map<String, String> params) {
//		JSONObject jsonParam = new JSONObject();
//		for (Entry<String, String> entry : params.entrySet())
//			jsonParam.put(entry.getKey(), entry.getValue());
//
//		StringEntity entity = new StringEntity(jsonParam.toString(), HTTP.UTF_8);// 解决中文乱码问题
//		entity.setContentEncoding(HTTP.UTF_8);
//		entity.setContentType("application/json");
//		return entity;
//	}

    private static void setHeaders(HttpPost httpPost, Map<String, String> newParamsMap) {
        if (newParamsMap != null) {
            for (Entry<String, String> kv : newParamsMap.entrySet())
                httpPost.addHeader(kv.getKey(), kv.getValue());
        }
    }

    public static void setHeaders(HttpRequestBase httpRequestBase, Map<String, String> newParamsMap) {
        if (newParamsMap != null) {
            for (Entry<String, String> kv : newParamsMap.entrySet())
                httpRequestBase.addHeader(kv.getKey(), kv.getValue());
        }
    }

    public static String genCurlHeaders(Map<String, String> newParamsMap) {
        if (newParamsMap != null) {
            StringBuffer sb = new StringBuffer();
            for (Entry<String, String> kv : newParamsMap.entrySet())
                sb.append("-H \"").append(kv.getKey()).append(":").append(kv.getValue()).append("\"  ");

            return sb.toString();
        } else
            return "";
    }

    public static String createPostCurlString(String url, Map<String, List<String>> params, Map<String, String> headerParams, ContentBody cb, Map<String, String> directHheaderParamsMap) {
        StringBuffer sb = new StringBuffer("curl ");

        //透传的http headers
        sb.append(genCurlHeaders(directHheaderParamsMap));

        sb.append(genCurlHeaders(headerParams));

        if (params != null) {
            StringBuffer postSB = new StringBuffer();
            for (Entry<String, List<String>> e : params.entrySet()) {
                if (postSB.length() > 0) {
                    postSB.append("&");
                }
                for (String value : e.getValue()) {
                    postSB.append(e.getKey()).append("=").append(urlEncoding(value, HTTP.UTF_8));
                }
            }
            if (postSB.length() > 0) {
                sb.append(" -d \"");
                postSB.append("\"");
                sb.append(postSB.toString());
            } else {
                sb.append("--data ''");
            }
        } else {
            // set params as
            //FIXME need this ??
            sb.append("--data '");
            sb.append(urlEncodedString(toNVP(params), HTTP.UTF_8));
            sb.append("'");
        }

        sb.append(" --insecure ");
        sb.append("\"");
        sb.append(url);
        sb.append("\"");
        return sb.toString();
    }

    public static String urlEncoding(String str, String encoding) {
        try {
            return URLEncoder.encode(str, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public static String urlDecoding(String str, String encoding) {
        try {
            return URLDecoder.decode(str, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private static String urlEncodedString(List<NameValuePair> parameters, String charset) {
        return URLEncodedUtils.format(parameters,
                charset != null ? charset : HTTP.DEF_CONTENT_CHARSET.name());
    }

    /**
     * 只能有以下组合：
     * 1. paramsMap: paramsMap以form表单方式提交
     * 2. contentbody: 以json或二进制的 body 方式提交
     * 4. paramsMap + attatchFileMap: multi part的 form 方式提交
     * 5. contentbody + attatchFileMap: 暂不支持，因为http协议需要给contentBody命名
     * 6. paramsMap+ contentbody + attatchFileMap: 暂不支持，因为http协议需要给contentBody命名
     *
     * @return
     */
    public static HttpPost createPost(final String url, Map<String, List<String>> urlParams, Map<String, String> headerParams, ContentBody cb, Map<String, HttpParameters.AttachFile> fileMap, ContentEncoding contentEncoding, ContentType contentType) {
        String charset = contentType == null || contentType.getCharset() == null ? HTTP.UTF_8 : contentType.getCharset().name();
        //set both cb and urlParams
        String newUrl = url;
        List<NameValuePair> nvps = toNVP(urlParams);
        if (cb != null && urlParams != null) {
            String newParamStr = urlEncodedString(nvps, charset);
            if ("".equals(newParamStr) == false) { //避免出现最后多一个&： http://ip:port/x?y=1&
                if (!url.contains("?")) {
                    newUrl = String.format("%s?%s", url, newParamStr);
                } else {
                    newUrl = String.format("%s&%s", url, newParamStr);
                }
            }
        }

        if (contentType == null) {
            if (cb != null) { //兼容历史未让用户设置content-type的场景
                if (cb.getStrContentBody() != null) {
                    contentType = ContentType.APPLICATION_JSON.withCharset(HttpCaller.DEFAULT_CHARSET);
                    try {
                        DocumentHelper.parseText(cb.getStrContentBody());
                        contentType = ContentType.APPLICATION_XML.withCharset(HttpCaller.DEFAULT_CHARSET);
                    } catch (DocumentException e) {//判断逻辑

                    }
                } else {
                    contentType = ContentType.APPLICATION_OCTET_STREAM;
                }
            } else {
                contentType = ContentType.APPLICATION_FORM_URLENCODED.withCharset(HttpCaller.DEFAULT_CHARSET); //默认值，兼容历史
            }
        }

        HttpPost httpost = new HttpPost(newUrl);
        setHeaders(httpost, headerParams);
        if (fileMap == null || fileMap.isEmpty()) { //不是多附件请求，则要显示设置content-type。附件时，则 MultipartEntityBuilder 自动设置
            httpost.addHeader(HTTP.CONTENT_TYPE, contentType.toString());
        }

        HttpEntity entity;
        try {
            if (fileMap != null && fileMap.isEmpty() == false) { //有附件，则使用 form+附件 提交
                MultipartEntityBuilder multiBuilder = MultipartEntityBuilder.create()
//                        .setContentType(ContentType.MULTIPART_FORM_DATA)
//                        .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                        .setMode(HttpMultipartMode.RFC6532)
                        .setCharset(contentType.getCharset());
                for (NameValuePair nvp : nvps) {
                    String name = nvp.getName();
                    String value = nvp.getValue();
                    if (contentType.getMimeType().equals(ContentType.APPLICATION_FORM_URLENCODED.getMimeType())) {
                        name = urlEncoding(nvp.getName(), contentType.getCharset().name());
                        value = urlEncoding(nvp.getValue(), contentType.getCharset().name());
                    }
                    if (ContentEncoding.gzip.equals(contentEncoding)) {
                        byte[] bytes = GZipUtils.gzipBytes(value.getBytes(HttpCaller.DEFAULT_CHARSET));
                        org.apache.http.entity.mime.content.ContentBody body = new ByteArrayBody(bytes, contentType, null);
                        FormBodyPartBuilder partBuilder = FormBodyPartBuilder.create(name, body);
                        partBuilder.setField(HTTP.CONTENT_ENCODING, GZIP);
                        multiBuilder.addPart(partBuilder.build());
                    } else {
                        multiBuilder.addTextBody(name, value, contentType);
                    }
                }

                for (Entry<String, HttpParameters.AttachFile> fileEntry : fileMap.entrySet()) {
                    HttpParameters.AttachFile file = fileEntry.getValue();
                    if (ContentEncoding.gzip.equals(file.getContentEncoding())) { //对附件进行压缩
                        FormBodyPartBuilder partBuilder = FormBodyPartBuilder.create(fileEntry.getKey(), new ByteArrayBody(GZipUtils.gzipBytes(file.getFileBytes()), file.getFileName()));
                        partBuilder.setField(HTTP.CONTENT_ENCODING, GZIP);
                        multiBuilder.addPart(partBuilder.build());
                    } else {
                        multiBuilder.addBinaryBody(fileEntry.getKey(), file.getFileBytes(), ContentType.DEFAULT_BINARY, file.getFileName());
                    }
                }
                entity = multiBuilder.build();
            } else if (cb == null) { //无附件，无body内容，则使用form提交
                entity = new UrlEncodedFormEntity(nvps, charset);
                InputStream s = entity.getContent();
                String ss = inputStreamToString(s);
                if (ContentEncoding.gzip.equals(contentEncoding)) {
                    entity = new GzipCompressingEntity(entity);
                    httpost.setHeader(HTTP.CONTENT_ENCODING, GZIP);
                }
            } else {
                if (ContentEncoding.gzip.equals(contentEncoding)) {
                    httpost.setHeader(HTTP.CONTENT_ENCODING, GZIP); //不参与签名，因为服务端需要先解析这个头，然后才参获得实际内容。同时兼容历史版本
                }

                if (cb.getStrContentBody() != null) {  //无附件，有json body内容，则 application/json 方式提交
                    entity = new StringEntity(cb.getStrContentBody(), contentType);// 解决中文乱码问题
                    if (ContentEncoding.gzip.equals(contentEncoding)) {
                        entity = new GzipCompressingEntity(entity);
                    }
                } else {  //无附件，有二进制body内容，则 APPLICATION_OCTET_STREAM 方式提交
                    entity = new ByteArrayEntity(cb.getBytesContentBody(), contentType);
                    if (ContentEncoding.gzip.equals(contentEncoding)) {
                        entity = new GzipCompressingEntity(entity);
                    }
                }
            }

            httpost.setEntity(entity);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return httpost;
    }

    private static String inputStreamToString(InputStream is) {

        String line = "";
        StringBuilder total = new StringBuilder();

        // Wrap a BufferedReader around the InputStream
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));

        try {
            // Read response until the end
            while ((line = rd.readLine()) != null) {
                total.append(line);
            }
        } catch (IOException e) {
        }
        return line;
        // Return full string
    }
        /**
         * 只能有以下组合：
         * 1. paramsMap: paramsMap以form表单方式提交
         * 2. contentbody: 以json或二进制的 body 方式提交
         * 4. paramsMap + attatchFileMap: multi part的 form 方式提交
         * 5. contentbody + attatchFileMap: 暂不支持，因为http协议需要给contentBody命名
         * 6. paramsMap+ contentbody + attatchFileMap: 暂不支持，因为http协议需要给contentBody命名
         *
         * @return
         */
    public static HttpPatch createPatch(final String url, Map<String, List<String>> urlParams, Map<String, String> headerParams, ContentBody cb, Map<String, HttpParameters.AttachFile> fileMap, ContentEncoding contentEncoding, ContentType contentType) {
        String charset = contentType == null || contentType.getCharset() == null ? HTTP.UTF_8 : contentType.getCharset().name();
        //set both cb and urlParams
        String newUrl = url;
        List<NameValuePair> nvps = toNVP(urlParams);
        if (cb != null && urlParams != null) {
            String newParamStr = urlEncodedString(nvps, charset);
            if ("".equals(newParamStr) == false) { //避免出现最后多一个&： http://ip:port/x?y=1&
                if (!url.contains("?")) {
                    newUrl = String.format("%s?%s", url, newParamStr);
                } else {
                    newUrl = String.format("%s&%s", url, newParamStr);
                }
            }
        }

        if (contentType == null) {
            if (cb != null) { //兼容历史未让用户设置content-type的场景
                if (cb.getStrContentBody() != null) {
                    contentType = ContentType.APPLICATION_JSON.withCharset(HttpCaller.DEFAULT_CHARSET);
                    try {
                        DocumentHelper.parseText(cb.getStrContentBody());
                        contentType = ContentType.APPLICATION_XML.withCharset(HttpCaller.DEFAULT_CHARSET);
                    } catch (DocumentException e) {//判断逻辑

                    }
                } else {
                    contentType = ContentType.APPLICATION_OCTET_STREAM;
                }
            } else {
                contentType = ContentType.APPLICATION_FORM_URLENCODED.withCharset(HttpCaller.DEFAULT_CHARSET); //默认值，兼容历史
            }
        }

        HttpPatch httpPatch = new HttpPatch(newUrl);
        if (headerParams != null) {
            for (Entry<String, String> kv : headerParams.entrySet())
                httpPatch.addHeader(kv.getKey(), kv.getValue());
        }
        if (fileMap == null || fileMap.isEmpty()) { //不是多附件请求，则要显示设置content-type。附件时，则 MultipartEntityBuilder 自动设置
            httpPatch.addHeader(HTTP.CONTENT_TYPE, contentType.toString());
        }

        HttpEntity entity;
        try {
            if (fileMap != null && fileMap.isEmpty() == false) { //有附件，则使用 form+附件 提交
                MultipartEntityBuilder multiBuilder = MultipartEntityBuilder.create()
//                        .setContentType(ContentType.MULTIPART_FORM_DATA)
//                        .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                        .setMode(HttpMultipartMode.RFC6532)
                        .setCharset(contentType.getCharset());
                for (NameValuePair nvp : nvps) {
                    String name = nvp.getName();
                    String value = nvp.getValue();
                    if (contentType.getMimeType().equals(ContentType.APPLICATION_FORM_URLENCODED.getMimeType())) {
                        name = urlEncoding(nvp.getName(), contentType.getCharset().name());
                        value = urlEncoding(nvp.getValue(), contentType.getCharset().name());
                    }
                    if (ContentEncoding.gzip.equals(contentEncoding)) {
                        byte[] bytes = GZipUtils.gzipBytes(value.getBytes(HttpCaller.DEFAULT_CHARSET));
                        org.apache.http.entity.mime.content.ContentBody body = new ByteArrayBody(bytes, contentType, null);
                        FormBodyPartBuilder partBuilder = FormBodyPartBuilder.create(name, body);
                        partBuilder.setField(HTTP.CONTENT_ENCODING, GZIP);
                        multiBuilder.addPart(partBuilder.build());
                    } else {
                        multiBuilder.addTextBody(name, value, contentType);
                    }
                }

                for (Entry<String, HttpParameters.AttachFile> fileEntry : fileMap.entrySet()) {
                    HttpParameters.AttachFile file = fileEntry.getValue();
                    if (ContentEncoding.gzip.equals(file.getContentEncoding())) { //对附件进行压缩
                        FormBodyPartBuilder partBuilder = FormBodyPartBuilder.create(fileEntry.getKey(), new ByteArrayBody(GZipUtils.gzipBytes(file.getFileBytes()), file.getFileName()));
                        partBuilder.setField(HTTP.CONTENT_ENCODING, GZIP);
                        multiBuilder.addPart(partBuilder.build());
                    } else {
                        multiBuilder.addBinaryBody(fileEntry.getKey(), file.getFileBytes(), ContentType.DEFAULT_BINARY, file.getFileName());
                    }
                }
                entity = multiBuilder.build();
            } else if (cb == null) { //无附件，无body内容，则使用form提交
                entity = new UrlEncodedFormEntity(nvps, charset);
                if (ContentEncoding.gzip.equals(contentEncoding)) {
                    entity = new GzipCompressingEntity(entity);
                    httpPatch.setHeader(HTTP.CONTENT_ENCODING, GZIP);
                }
            } else {
                if (ContentEncoding.gzip.equals(contentEncoding)) {
                    httpPatch.setHeader(HTTP.CONTENT_ENCODING, GZIP); //不参与签名，因为服务端需要先解析这个头，然后才参获得实际内容。同时兼容历史版本
                }

                if (cb.getStrContentBody() != null) {  //无附件，有json body内容，则 application/json 方式提交
                    entity = new StringEntity(cb.getStrContentBody(), contentType);// 解决中文乱码问题
                    if (ContentEncoding.gzip.equals(contentEncoding)) {
                        entity = new GzipCompressingEntity(entity);
                    }
                } else {  //无附件，有二进制body内容，则 APPLICATION_OCTET_STREAM 方式提交
                    entity = new ByteArrayEntity(cb.getBytesContentBody(), contentType);
                    if (ContentEncoding.gzip.equals(contentEncoding)) {
                        entity = new GzipCompressingEntity(entity);
                    }
                }
            }

            httpPatch.setEntity(entity);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return httpPatch;
    }

    /**
     * 只能有以下组合：
     * 1. paramsMap: paramsMap以form表单方式提交
     * 2. contentbody: 以json或二进制的 body 方式提交
     * 4. paramsMap + attatchFileMap: multi part的 form 方式提交
     * 5. contentbody + attatchFileMap: 暂不支持，因为http协议需要给contentBody命名
     * 6. paramsMap+ contentbody + attatchFileMap: 暂不支持，因为http协议需要给contentBody命名
     *
     * @return
     */
    public static HttpPut createPut(final String url, Map<String, List<String>> urlParams, Map<String, String> headerParams, ContentBody cb, Map<String, HttpParameters.AttachFile> fileMap, ContentEncoding contentEncoding, ContentType contentType) {
        String charset = contentType == null || contentType.getCharset() == null ? HTTP.UTF_8 : contentType.getCharset().name();
        //set both cb and urlParams
        String newUrl = url;
        List<NameValuePair> nvps = toNVP(urlParams);
        if (cb != null && urlParams != null) {
            String newParamStr = urlEncodedString(nvps, charset);
            if ("".equals(newParamStr) == false) { //避免出现最后多一个&： http://ip:port/x?y=1&
                if (!url.contains("?")) {
                    newUrl = String.format("%s?%s", url, newParamStr);
                } else {
                    newUrl = String.format("%s&%s", url, newParamStr);
                }
            }
        }

        if (contentType == null) {
            if (cb != null) { //兼容历史未让用户设置content-type的场景
                if (cb.getStrContentBody() != null) {
                    contentType = ContentType.APPLICATION_JSON.withCharset(HttpCaller.DEFAULT_CHARSET);
                    try {
                        DocumentHelper.parseText(cb.getStrContentBody());
                        contentType = ContentType.APPLICATION_XML.withCharset(HttpCaller.DEFAULT_CHARSET);
                    } catch (DocumentException e) {//判断逻辑

                    }
                } else {
                    contentType = ContentType.APPLICATION_OCTET_STREAM;
                }
            } else {
                contentType = ContentType.APPLICATION_FORM_URLENCODED.withCharset(HttpCaller.DEFAULT_CHARSET); //默认值，兼容历史
            }
        }

        HttpPut httpPut = new HttpPut(newUrl);
        if (headerParams != null) {
            for (Entry<String, String> kv : headerParams.entrySet())
                httpPut.addHeader(kv.getKey(), kv.getValue());
        }
        if (fileMap == null || fileMap.isEmpty()) { //不是多附件请求，则要显示设置content-type。附件时，则 MultipartEntityBuilder 自动设置
            httpPut.addHeader(HTTP.CONTENT_TYPE, contentType.toString());
        }

        HttpEntity entity;
        try {
            if (fileMap != null && fileMap.isEmpty() == false) { //有附件，则使用 form+附件 提交
                MultipartEntityBuilder multiBuilder = MultipartEntityBuilder.create()
//                        .setContentType(ContentType.MULTIPART_FORM_DATA)
//                        .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                        .setMode(HttpMultipartMode.RFC6532)
                        .setCharset(contentType.getCharset());
                for (NameValuePair nvp : nvps) {
                    String name = nvp.getName();
                    String value = nvp.getValue();
                    if (contentType.getMimeType().equals(ContentType.APPLICATION_FORM_URLENCODED.getMimeType())) {
                        name = urlEncoding(nvp.getName(), contentType.getCharset().name());
                        value = urlEncoding(nvp.getValue(), contentType.getCharset().name());
                    }
                    if (ContentEncoding.gzip.equals(contentEncoding)) {
                        byte[] bytes = GZipUtils.gzipBytes(value.getBytes(HttpCaller.DEFAULT_CHARSET));
                        org.apache.http.entity.mime.content.ContentBody body = new ByteArrayBody(bytes, contentType, null);
                        FormBodyPartBuilder partBuilder = FormBodyPartBuilder.create(name, body);
                        partBuilder.setField(HTTP.CONTENT_ENCODING, GZIP);
                        multiBuilder.addPart(partBuilder.build());
                    } else {
                        multiBuilder.addTextBody(name, value, contentType);
                    }
                }

                for (Entry<String, HttpParameters.AttachFile> fileEntry : fileMap.entrySet()) {
                    HttpParameters.AttachFile file = fileEntry.getValue();
                    if (ContentEncoding.gzip.equals(file.getContentEncoding())) { //对附件进行压缩
                        FormBodyPartBuilder partBuilder = FormBodyPartBuilder.create(fileEntry.getKey(), new ByteArrayBody(GZipUtils.gzipBytes(file.getFileBytes()), file.getFileName()));
                        partBuilder.setField(HTTP.CONTENT_ENCODING, GZIP);
                        multiBuilder.addPart(partBuilder.build());
                    } else {
                        multiBuilder.addBinaryBody(fileEntry.getKey(), file.getFileBytes(), ContentType.DEFAULT_BINARY, file.getFileName());
                    }
                }
                entity = multiBuilder.build();
            } else if (cb == null) { //无附件，无body内容，则使用form提交
                entity = new UrlEncodedFormEntity(nvps, charset);
                if (ContentEncoding.gzip.equals(contentEncoding)) {
                    entity = new GzipCompressingEntity(entity);
                    httpPut.setHeader(HTTP.CONTENT_ENCODING, GZIP);
                }
            } else {
                if (ContentEncoding.gzip.equals(contentEncoding)) {
                    httpPut.setHeader(HTTP.CONTENT_ENCODING, GZIP); //不参与签名，因为服务端需要先解析这个头，然后才参获得实际内容。同时兼容历史版本
                }

                if (cb.getStrContentBody() != null) {  //无附件，有json body内容，则 application/json 方式提交
                    entity = new StringEntity(cb.getStrContentBody(), contentType);// 解决中文乱码问题
                    if (ContentEncoding.gzip.equals(contentEncoding)) {
                        entity = new GzipCompressingEntity(entity);
                    }
                } else {  //无附件，有二进制body内容，则 APPLICATION_OCTET_STREAM 方式提交
                    entity = new ByteArrayEntity(cb.getBytesContentBody(), contentType);
                    if (ContentEncoding.gzip.equals(contentEncoding)) {
                        entity = new GzipCompressingEntity(entity);
                    }
                }
            }

            httpPut.setEntity(entity);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return httpPut;
    }

    public static List<NameValuePair> toNVP(Map<String, List<String>> urlParams) {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();

        //fix NPE
        if (urlParams != null) {
            Set<String> keySet = urlParams.keySet();
            for (String key : keySet) {
                for (String value : urlParams.get(key)) {
                    nvps.add(new BasicNameValuePair(key, value));
                }
            }
        }
        return nvps;
    }

    public static void setDirectHeaders(HttpPost httpPost, Map<String, String> directHheaderParamsMap) {
        if (directHheaderParamsMap == null) {
            //do nothing
            return;
        } else {
            for (Entry<String, String> kv : directHheaderParamsMap.entrySet()) {
                if (kv.getKey() == null) {
                    //log.info("ignore empty key");
                } else {
                    if (HTTP.CONTENT_TYPE.equals(kv.getKey()) || !httpPost.containsHeader(kv.getKey())) {
                        // direct header has no chance to overwrite the normal headers, except it is the content-type
                        httpPost.addHeader(kv.getKey(), kv.getValue());
                    }
                }
            }
        }
    }

    public static String getUrlPathInfo(String url) throws HttpCallerException {
        URL urlStr = null;
        try {
            urlStr = new URL(url);
        } catch (Exception e) {
            throw new HttpCallerException("url is unformat, url is " + url);
        }
        String path = urlStr.getPath();
        return path;
    }

    public static Map<String, String> fetchResHeaderMap(final HttpResponse response) {
        Map<String, String> headerMap = new HashMap<String, String>();
        if (response != null) {
            headerMap.put("HTTP-STATUS", String.valueOf(response.getStatusLine().getStatusCode()));
            Header dispositionHeader = response.getFirstHeader("Content-disposition");
            if (null != dispositionHeader) {
                HeaderElement[] headerElements = dispositionHeader.getElements();
                NameValuePair nameValuePair = headerElements[0].getParameterByName("filename");
                String fileName = nameValuePair.getValue();
                headerMap.put("fileName", fileName);
            }
            for (Header header : response.getAllHeaders()) {
                headerMap.put(header.getName(), header.getValue());
            }
        }

        return headerMap;
    }

    public static String fetchResHeaders(final HttpResponse response) {
        if (response != null) {
            StringBuffer body = new StringBuffer();
            //add response http status
            body.append(String.format("\"%s\":\"%s\"", "HTTP-STATUS", response.getStatusLine()));
            for (Header header : response.getAllHeaders()) {
                if (body.length() > 0)
                    body.append(",");
                body.append(String.format("\"%s\":\"%s\"", header.getName(), header.getValue()));
            }
            return String.format("{%s}", body.toString());
        }

        return null;
    }


    public static String generateAsEncodeRequestUrl(String requestURL, String charset, Map<String, List<String>> urlParamsMap) {
        requestURL = HttpClientHelper.trimUrl(requestURL);
        charset = charset == null ? "UTF-8" : charset;

        StringBuffer params = new StringBuffer();
        for (Entry<String, List<String>> kv : urlParamsMap.entrySet()) {
            if (kv.getValue() != null) {
                List<String> vlist = kv.getValue();
                for (String v : vlist) {
                    params.append("&")
                            .append(urlEncoding(kv.getKey(), charset))
                            .append("=")
                            .append(urlEncoding(v, charset));
                }
            }
        }

        String newRequestURL = requestURL;
        if (params.length() > 0) {
            newRequestURL += params.replace(0, 1, "?");
        }
        HttpClientHelper.printDebugInfo("-- requestURL=" + newRequestURL);
        return newRequestURL;
    }

    public static String getParamsUrlEncodingStr(Map<String, List<String>> params) {
        StringBuffer sb = new StringBuffer();
        if (params != null) {
            for (Entry<String, List<String>> e : params.entrySet()) {
                for (String value : e.getValue()) {
                    sb.append("&").append(urlEncoding(e.getKey(), HTTP.UTF_8)).append("=").append(urlEncoding(value, HTTP.UTF_8));
                }
            }
        }
        if (sb.length() > 0) {
            return sb.toString().substring(1); //去掉最前面的 &
        } else {
            return "";
        }
    }

    public static void main(String[] args) {
        try {
            String fileName = new String("文件-1604378082581".getBytes(), "GBK");
            System.out.println(fileName);

            fileName = new String(fileName.getBytes("GBK"));
            System.out.println(fileName);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
