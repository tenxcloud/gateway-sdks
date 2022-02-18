package com.apigateway.sdk;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.apigateway.sdk.dto.AuthParam;
import com.apigateway.sdk.dto.ContentBody;
import com.apigateway.sdk.dto.ContentEncoding;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.entity.ContentType;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

import static com.apigateway.sdk.HttpCaller.TOTAL_FILE_SIZE;

/**
 * Http Parameters 参数构造器，使用(Builder)模式构造http调用的所有参数
 *
 * @author hpy
 * @date 2021
 */
public class HttpParameters {
    public static final long MAX_FILE_AMOUNT;

    static {
        MAX_FILE_AMOUNT = Integer.getInteger("httpAttachmentMaxAmount", 5); //一次最多上传5个附件
    }

    private Builder builder;

    String getMethod() {
        return builder.method;
    }

    String getRequestUrl() {
        return builder.requestUrl;
    }

    ContentBody getContentBody() {
        return builder.contentBody;
    }

    ContentType getContentType() {
        return builder.contentType;
    }

    Map<String, AttachFile> getAttachFileMap() {
        return builder.attatchFileMap;
    }

    public Map<String, List<String>> getParamsMap() {
        return builder.paramsMap;
    }

    public AuthParam getAuthParam() {
        return builder.authParam;
    }
    ContentEncoding getContentEncoding() {
        return builder.contentEncoding;
    }

    public Map<String, String> getHeaderParamsMap() {
        return builder.headerParamsMap;
    }

    boolean isTimestamp() {
        return builder.timestamp;
    }

   /* boolean isDiagnostic() {
        return builder.diagnostic;
    }*/

    /**
     * 显示所设置的各个属性值
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("requestUrl=").append(this.getRequestUrl());
        sb.append("\n method=").append(this.getMethod());
        sb.append("\n contentBody=").append(this.getContentBody());
        sb.append("\n Timestamp=").append(this.isTimestamp());
        sb.append("\n params: \n");
        for (Entry<String, List<String>> entry : builder.paramsMap.entrySet()) {
            sb.append(entry.getKey()).append("=");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append(value);
            } else if (value instanceof String[]) {
                sb.append(Arrays.toString((String[]) value));
            } else if (value instanceof List) {
                sb.append(value);
            }
            sb.append("\n");
        }

        sb.append("\n http header params: \n");
        for (Entry<String, String> entry : builder.headerParamsMap.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }

        return sb.toString();
    }

    public static class AttachFile {
        @Setter
        private HttpParameters httpParameters;
        @Getter
        private String fileName;
        @Getter
        private byte[] fileBytes;
        private ContentEncoding contentEncoding;

        public AttachFile(String fileName, byte[] fileBytes, ContentEncoding contentEncoding) {
            this.fileName = fileName;
            this.fileBytes = fileBytes;
            this.contentEncoding = contentEncoding;
        }

        /**
         * 请求是否需要压缩：
         * 1. 如果用户明确不需要，则不压缩
         */
        public ContentEncoding getContentEncoding() {
            if (contentEncoding == null) {
                return httpParameters != null ? httpParameters.getContentEncoding() : null;
            } else {
                return contentEncoding;
            }
        }
    }

    /**
     * 内部静态类，用来设置HttpCaller调用的相关参数
     * 只能有以下组合：
     * 1. paramsMap: paramsMap以form表单方式提交
     * 2. contentbody: 以json或二进制的 body 方式提交
     * 3. paramsMap + contentBody:  paramsMap以query方式提交，contentBody通过httpBody提交
     * 4. paramsMap + attatchFileMap: multi part的 form 方式提交
     * 5. contentbody + attatchFileMap: 暂不支持，因为http协议需要给contentBody命名
     * 6. paramsMap+ contentbody + attatchFileMap: 暂不支持，因为http协议需要给contentBody命名
     */
    public static class Builder {
        private String method = "";
        private ContentType contentType;
        private ContentBody contentBody = null;
        private Map<String, AttachFile> attatchFileMap;
        private String requestUrl;
        private boolean timestamp = true;
        private Map<String, List<String>> paramsMap = new HashMap<String, List<String>>();
        private ContentEncoding contentEncoding;
        private Map<String, String> headerParamsMap = new HashMap<String, String>();
        //private boolean diagnostic = false;
        private HttpServletRequest request;
        private AuthParam authParam;

        public Builder() {
            headerParamsMap.put("Accept-Encoding", HttpCaller.GZIP);//默认设置接受gzip
        }

        public Builder copy() {
            HttpParameters.Builder builder = HttpParameters.newBuilder();
            builder.method = this.method;
            builder.contentType = this.contentType;
            builder.contentBody = this.contentBody;
            builder.attatchFileMap = this.attatchFileMap;
            builder.requestUrl = this.requestUrl;
            builder.authParam = this.authParam;
            builder.timestamp = this.timestamp;
            builder.paramsMap = new HashMap<String, List<String>>(this.paramsMap);
            builder.contentEncoding = this.contentEncoding;
            builder.headerParamsMap = new HashMap<String, String>(this.headerParamsMap);
            //builder.diagnostic = this.diagnostic;
            builder.request = this.request;
            return builder;
        }

        /**
         * 设置是否对 paramsMap和contentBody 进行压缩
         */
        public Builder setContentEncoding(ContentEncoding contentEncoding) {
            this.contentEncoding = contentEncoding;
            return this;
        }

        /**
         * 增加附件
         */
        public Builder addAttachFile(String fileName, InputStream inputStream) {
            return addAttachFile(fileName, inputStream, null);
        }

        /**
         * 增加附件
         */
        public Builder addAttachFile(File file) {
            return addAttachFile(file, null);
        }

        /**
         * 增加附件
         */
        public Builder addAttachFile(File file, ContentEncoding contentEncoding) {
            if (file == null) {
                throw new IllegalArgumentException("file不允许为空");
            }
            return addAttachFile(file.getName(), HttpCaller.readFile(file), contentEncoding);
        }

        /**
         * 增加附件
         */
        public Builder addAttachFile(String fileName, InputStream inputStream, ContentEncoding contentEncoding) {
            if (inputStream == null) {
                throw new IllegalArgumentException("file内容不允许为空");
            }
            return addAttachFile(fileName, HttpCaller.readInputStream(inputStream), contentEncoding);
        }

        private Builder addAttachFile(String fileName, byte[] bytes, ContentEncoding contentEncoding) {
            if (method.equalsIgnoreCase("POST") == false) {
                throw new IllegalArgumentException("发送附件必须使用POST");
            }
            if (contentBody != null) {
                throw new IllegalArgumentException("无法同时发送 contentBody 和 文件");
            }
            if (bytes == null) {
                throw new IllegalArgumentException("file内容不允许为空");
            }
            if (fileName == null) {
                throw new IllegalArgumentException("fileName不允许为空");
            }

            if (attatchFileMap == null) {
                attatchFileMap = new HashMap<String, AttachFile>();
            }

            if (attatchFileMap.size() >= MAX_FILE_AMOUNT) {
                throw new IllegalArgumentException("附件数量超过限制");
            }


            if (getTotalFileSize() + bytes.length > TOTAL_FILE_SIZE) {
                throw new IllegalArgumentException("attach file is too large exceed the MAX-SIZE");
            }

            attatchFileMap.put(fileName, new AttachFile(fileName, bytes, contentEncoding));
            return this;
        }

        public int getTotalFileSize() {
            int totalSize = 0;
            if (attatchFileMap != null) {
                for (Entry<String, AttachFile> stringAttachFileEntry : attatchFileMap.entrySet()) {
                    totalSize += stringAttachFileEntry.getValue().fileBytes.length;
                }
            }
            return totalSize;
        }


        /**
         * @param timestampFlag, 是否生成时间戳，默认是生成的
         * @return
         */
        public Builder timestamp(boolean timestampFlag) {
            this.timestamp = timestampFlag;

            return this;
        }

        /**
         * 设置HTTP请求的URL串
         *
         * @param url
         * @return
         */
        public Builder requestURL(String url) {
            this.requestUrl = url;
            return this;
        }

        public Builder authParam(AuthParam authParam) {
            this.authParam = authParam;
            return this;
        }
        /**
         * 设置调用的方式： 目前支持的取值是: get,post,patch,put,delete,options,head
         *
         * @param method
         * @return
         */
        public Builder method(String method) {
            this.method = method;
            return this;
        }

        /**
         * 清除已经设置的参数对
         *
         * @return
         */
        public Builder clearParamsMap() {
            this.paramsMap.clear();
            return this;
        }

        /**
         * 设置一个参数对列表
         *
         * @param key
         * @param valueList
         * @return
         */
        public Builder putParamsMap(String key, List<String> valueList) {
            if (valueList == null) {
                throw new IllegalArgumentException("valueList is not allow null.");
            }
            if (valueList instanceof ArrayList) {
                this.paramsMap.put(key, valueList);

            } else { //保证是数组list
                this.paramsMap.put(key, Arrays.asList(valueList.toArray(new String[valueList.size()])));
            }
            return this;
        }

        /**
         * 设置一个参数对列表
         *
         * @param key
         * @param value
         * @return
         */
        public Builder putParamsMap(String key, String... value) {
            if (value == null) {
                throw new IllegalArgumentException("value is not allow null.");
            }
            this.paramsMap.put(key, Arrays.asList(value));
            return this;
        }

        /**
         * 设置参数对集合
         *
         * @param map
         * @return
         */
        public Builder putParamsMapAll(Map<String, String> map) {
            if (map != null) {
                for (Entry<String, String> entry : map.entrySet()) {
                    this.paramsMap.put(entry.getKey(), Arrays.asList(entry.getValue()));
                }
            } else {
                throw new IllegalArgumentException("empty map!!");
            }
            return this;
        }

        /**
         * 设置参数对集合
         *
         * @param map
         * @return
         */
        public Builder putParamsMap(Map<String, List<String>> map) {
            if (map == null) {
                throw new IllegalArgumentException("empty map!!");
            }
            for (Entry<String, List<String>> entry : map.entrySet()) {
                this.paramsMap.put(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /**
         * 设置参数对集合
         *
         * @param map
         * @return
         */
        public Builder putParamsMapListAll(Map<String, List<String>> map) {
            if (map != null) {
                this.paramsMap.putAll(map);
            } else {
                throw new IllegalArgumentException("empty map!!");
            }
            return this;
        }

        /**
         * 清除所有已经设置的HTTP Header参数对
         *
         * @return
         */
        public Builder clearHeaderParamsMap() {
            this.headerParamsMap.clear();
            return this;
        }

        /**
         * 设置一个HTTP Header参数对
         *
         * @param key
         * @param value
         * @return
         */
        public Builder putHeaderParamsMap(String key, String value) {
            this.headerParamsMap.put(key, value);
            return this;
        }

        /**
         * 添加所有的Http Header参数对集合
         *
         * @param map
         * @return
         */
        public Builder putHeaderParamsMapAll(Map<String, String> map) {
            if (map != null) {
                this.headerParamsMap.putAll(map);
            } else {
                throw new IllegalArgumentException("empty map!!");
            }
            return this;
        }

        /**
         * 设置contentType
         *
         * @param contentTypeStr
         * @return
         */
        public Builder contentType(String contentTypeStr) {
            this.contentType = org.apache.http.entity.ContentType.parse(contentTypeStr);
            return this;
        }

        /**
         * 设置contentBody
         *
         * @param cb
         * @return
         */
        public Builder contentBody(ContentBody cb) {
            if (attatchFileMap != null && attatchFileMap.isEmpty() == false) {
                throw new IllegalArgumentException("无法同时发送 contentBody 和 文件");
            }

            this.contentBody = cb;
            return this;
        }

        /**
         * 生成最终的参数集合
         *
         * @return
         */
        public HttpParameters build() {
            HttpParameters httpParameters = new HttpParameters(this);
            if (attatchFileMap != null) {
                for (AttachFile attachFile : attatchFileMap.values()) {
                    attachFile.setHttpParameters(httpParameters);
                }
            }
            return httpParameters;
        }

        public String url() {
            return this.requestUrl;
        }

        public Map<String, List<String>> paramsMap() {
            return this.paramsMap;
        }

        public Map<String, String> headerMap() {
            return this.headerParamsMap;
        }

        public ContentBody contentBody() {
            return this.contentBody;
        }

        public ContentType contentType() {
            return this.contentType;
        }

        public ContentEncoding contentEncoding() {
            return this.contentEncoding;
        }
    }

    /**
     * private作用域的参数构造器，防止外部调用生成该实例
     *
     * @param builder
     */
    private HttpParameters(Builder builder) {
        this.builder = builder;
    }

    /**
     * 构造一个参数生成器
     *
     * @return
     */
    public static Builder newBuilder() {
        return new Builder();
    }

}
