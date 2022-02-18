/*
 * Licensed Materials - Property of tenxcloud.com
 * (C) Copyright 2019 TenxCloud. All Rights Reserved.
 *
 * 2021/8/3 @author peiyun
 */
package com.apigateway.sdk.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.apigateway.sdk.HttpCaller;
import com.apigateway.sdk.HttpParameters;
import com.apigateway.sdk.dto.AuthParam;
import com.apigateway.sdk.dto.ContentBody;
import com.apigateway.sdk.dto.ContentEncoding;
import com.apigateway.sdk.dto.HttpReturn;
import com.apigateway.sdk.exception.HttpCallerException;
import com.apigateway.sdk.model.User;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test
 * @author hpy
 * @date 2021/08/03
**/
@Slf4j
@RestController
public class TestController {

    @GetMapping("/test")
    @ResponseBody
    public String Test(HttpServletRequest request) throws IOException {
        log.info("call test api");
        String url="http://192.168.1.214/testfourapi/test";
        Map<String, String> headerParam = new HashMap<>();
        Map<String, String> paramsMap = new HashMap<>();
        try {
            HttpReturn httpReturn= HttpCaller.doGet(url,headerParam,"",paramsMap,null);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    @GetMapping("/testbasic")
    @ResponseBody
    public String Testbasic(HttpServletRequest request) throws IOException {
        log.info("call test api");
        String url="http://192.168.1.70/weatherapi/test";
        Map<String, String> headerParam = new HashMap<>();
        AuthParam authParam = new AuthParam();
        authParam.setAuthType("basic-auth");
        authParam.setUsername("ccc");
        authParam.setPassword("2sHGo~j&c");
        Map<String, String> paramsMap = new HashMap<>();
        try {
            HttpReturn httpReturn= HttpCaller.doGet(url,headerParam,"",paramsMap,authParam);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    @GetMapping("/testauth2")
    @ResponseBody
    public String Testauth2(HttpServletRequest request) throws IOException {
        log.info("call test api");
        String url="http://192.168.2.171/demo05/test";
        Map<String, String> headerParam = new HashMap<>();
        AuthParam authParam = buildOauth2();
        Map<String, String> paramsMap = new HashMap<>();
        try {
            HttpReturn httpReturn= HttpCaller.doGet(url,headerParam,"",paramsMap,authParam);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    private AuthParam buildOauth2() {
        AuthParam authParam = new AuthParam();
        authParam.setAuthType("oauth2");
        authParam.setClientId("HF8jgp3TM3yq8xJSuP29YVdB68OkZP8a");
        authParam.setClientSecret("1EROs1AVyCBmbu7I6OVAEIItEDdmSzEG");
        //authParam.setScope("user");
        return authParam;
    }

    @GetMapping("/testjwt")
    @ResponseBody
    public String Testjwt(HttpServletRequest request) throws IOException {
        log.info("call test api");
        String url="http://192.168.1.70/brandapi/test";
        Map<String, String> headerParam = new HashMap<>();
        AuthParam authParam = buildEsJwt();
        Map<String, String> paramsMap = new HashMap<>();
        try {
            HttpReturn httpReturn= HttpCaller.doGet(url,headerParam,"",paramsMap,authParam);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    private AuthParam buildEsJwt() {
        AuthParam authParam = new AuthParam();
        authParam.setAuthType("jwt");
        String privateKey = "-----BEGIN PRIVATE KEY-----\n" +
                "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg4JiqQudfFPvniKAB\n" +
                "4ykOqxN+YfnWSXm2ViPipEp0oGKhRANCAATl0XFL7qvlpjG4Yw4HgrYCySLR2XPF\n" +
                "jPpcDwyBMhv8AMlN7CT+qgvOCCh9vxc1PjGLyrX29YDFnEGC7v83zguv\n" +
                "-----END PRIVATE KEY-----";
        authParam.setKey("1yfKkiLGbPdLH71VVyeJmbw0Z04arvOi");
        authParam.setSecretOrPrivateKey(privateKey);
        authParam.setAlgorithm("ES256");
        return authParam;
    }

    @GetMapping("/testjwt2")
    @ResponseBody
    public String Testjwt2(HttpServletRequest request) throws IOException {
        log.info("call test api");
        String url="http://192.168.1.70/brandapi/test";
        Map<String, String> headerParam = new HashMap<>();
        //AuthParam authParam = buildHsJwt();
        AuthParam authParam = buildRsJwt();
        Map<String, String> paramsMap = new HashMap<>();
        try {
            HttpReturn httpReturn= HttpCaller.doGet(url,headerParam,"",paramsMap,authParam);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    private AuthParam buildRsJwt() {
        AuthParam authParam = new AuthParam();
        authParam.setAuthType("jwt");
        String privateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
                "MIIEpAIBAAKCAQEAw0N9fjY0F/KU7p0PYcZRSCz1DFTRLxwwBiWaaiWMDReee9dy\n" +
                "KhBbC8mKMZguFB7JppEdrKsQerjwQGoqyAe2NREclElkkjWhYydSYhr/v1xFma+E\n" +
                "zT8QEQshGRkaNAfJBlDuKg+wwyjzOAEGCdLtZYm8ZiOKbqNYsQpd5VTQwWTRt3a4\n" +
                "p3RByU+DSiPgCvv+QYpOqqU1yoPnOEn7aVc7MRFQ2aF6GyOvdeGq6PZPGhmCZhAy\n" +
                "kVMEjPwt47aDnvzGK5fAQYlkcrpsLus0mL3ZC03lpNSwmG7mik6B5Yg8RQput0Oy\n" +
                "4zWkPdZSu8fh8ECpkc2/ezneG0dC7ROJtxZAJQIDAQABAoIBAQCluPQbWFmiMYph\n" +
                "cf+KFk0KGs3np1zaxOula+jAShmxSuiBZvtH5BXBzU1yhhwli3PN4L/0ukMERU3y\n" +
                "/ahNmO314A4HdLrctJPCma78ALoqaV/Q7fyiTfGO5YkzyfBJVIoyyCE6pZJ6MJks\n" +
                "YQIE+HXvhZTWgzeMayptJ8VFTFTYrQEDzTRa2NvMlE3jgSukAIYnFbSfBAJWdclB\n" +
                "ZNk00792cmtzZGwilhShtyLwpcuUl9jwAUQNu9XUA+R8VOosPvxkaMKPVJZwdxNj\n" +
                "4FZZqFG8kIOlwFov6wGwdbgNBJnxgE8CCAC4RdU7OjGpKeaKE2vP2WdK9UQ2lec/\n" +
                "1kLYWtBBAoGBAPiPxHiNaAkFdRj7SYGxSjeIJpxknc/2NWBfcFuSybsYFu91hbpi\n" +
                "QTJjVjm1jEqLvxqDrF8KGRzWiEbzElNihccHPz6NfjGP7byLJBIAGvtQ76uspX2R\n" +
                "POOyMNa1gwZr/hIt8V21qGBOVGeCSs5XspRLJdcOPWLTlO66LFFkvQFdAoGBAMkb\n" +
                "aBgiKTSNP6sCwnnxCG5sKQQTMi9/0qD+NnHZ+PxYe9u56vupFCmMG1v06wZSVth6\n" +
                "/6EzL2Uhow6VufcRXvWgdeTLa0zIr+4skLbVW8Km7QzTtHqKWg38FR/GTEvKxSrw\n" +
                "ODScAliPH+sd97mncI3T8mhpBomHssfcWdZgs2VpAoGANRE8uSA3nfV0UqkTwITB\n" +
                "e6mt1KYGq2e9bZ3ytxpXx4IVI3rL2kcv9DoRJrEECsZbPqXuHzxgsC99WOh8eSEH\n" +
                "vr5lWk2LK7m24BI02UL8f7t/7/8kNfLUNSjOFIkcODrGO8tjRMaL9EPE8XR31CYV\n" +
                "EmQY6Bk6MedKXM2SuK3xTGECgYEArdCdOHaWDJCkXabF9WUI939ON/JrZrhgkLsG\n" +
                "zYIqe9fyPSfEW/x3q60nnGPjmhQicXplZKr7pLnc12c7UrpVy5ADNVtHgxbVT9J+\n" +
                "xTdE1sk1iueTTnIzgXDFxS+7k6pOnV0AByBruqUBXlIJYRHB6yMrmWhkuQtqzui5\n" +
                "UNtFUjkCgYAK8WzxLgxXjDc/2Dny3s7QFYD0U/2F9Esy0y4J/KXr+5JEWc03IOYh\n" +
                "IwUsc4CXH9wsGblEqzJn2LbUEvVSOU/jfjVMo+9x9ZECy3SkB2M+mOojq+V34dqI\n" +
                "fnbdhlhBSTvOPm17+MunGk3Hu+JrnSOV3+yADZuObSqNx/aZrjMGxQ==\n" +
                "-----END RSA PRIVATE KEY-----";
        authParam.setKey("KEpajiOY2vOaOxirKklXYu57VJvWOTHi");
        authParam.setSecretOrPrivateKey(privateKey);
        authParam.setAlgorithm("RS256");
        return authParam;
    }

    private AuthParam buildHsJwt() {
        AuthParam authParam = new AuthParam();
        authParam.setAuthType("jwt");
        authParam.setKey("vFx06jGdzd5IPTTlYal5AcjmoVD2L0PQ");
        authParam.setSecretOrPrivateKey("6rR8rDJAKNqbkaxuoF5bK6nRNGBTqbVhCXArnx2VF5kCrM1SxiWu4HMNuo5nlKWE");
        authParam.setAlgorithm("HS512");
        return authParam;
    }

    @GetMapping("/testPost")
    @ResponseBody
    public String TestPost(HttpServletRequest request) throws IOException {
        log.info("call test api");
        //String url="http://192.168.1.214/demoSvcApi01/user/tranformPost/1";
        String url="http://127.0.0.1:8082/user/tranformPost/1";
        Map<String, String> headerParam = new HashMap<>();
        //headerParam.put("auth","testauth");
        //AuthParam authParam = buildBasic();
        //AuthParam authParam = buildOauth2();
        //AuthParam authParam =buildHsJwt();
        //AuthParam authParam =buildRsJwt();
        //AuthParam authParam =buildEsJwt();
        AuthParam authParam = new AuthParam();
        authParam.setAuthType("basic-auth");
        authParam.setUsername("demo01");
        authParam.setPassword("8vRv~ey&o");
        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("name", "testname");
        paramsMap.put("age", "15");
        paramsMap.put("id", "1");
        try {
            HttpReturn httpReturn = HttpCaller.doPost(url,headerParam,"", paramsMap,authParam);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    private AuthParam buildBasic() {
        AuthParam authParam = new AuthParam();
        authParam.setAuthType("basic-auth");
        authParam.setUsername("ccc");
        authParam.setPassword("2sHGo~j&c");
        return authParam;
    }

    @GetMapping("/testPostJSON")
    @ResponseBody
    public String TestPostJson(HttpServletRequest request) throws IOException {
        log.info("call test api");
        String url="http://192.168.1.70/demo1/user/tranformPost2/1";
        Map<String, String> headerParam = new HashMap<>();
        headerParam.put("auth","testauth");
        JSONObject requestJ = new JSONObject();
        //AuthParam authParam = buildBasic();
        //AuthParam authParam = buildOauth2();
        //AuthParam authParam =buildHsJwt();
        //AuthParam authParam =buildRsJwt();
        AuthParam authParam =buildEsJwt();
        requestJ.put("name", "testname2");
        requestJ.put("age", 12);
        try {
            HttpReturn httpReturn = HttpCaller.doPost(url,headerParam,"", new ContentBody(requestJ.toJSONString()),authParam);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    @GetMapping("/testPostXml")
    @ResponseBody
    public String TestPostXml(HttpServletRequest request) throws IOException {
        log.info("call test api");
        String url="http://192.168.1.70/demo1/user/demopostxml";
        Map<String, String> headerParam = new HashMap<>();
        headerParam.put("auth","testauth");
        JSONObject requestJ = new JSONObject();
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("user2");
        Element name = root.addElement("name");
        Element password = root.addElement("password");
        name.addText("namexml");
        password.addText("123456");
        String output = document.asXML();
        String contentType="application/xml";
        //AuthParam authParam = buildBasic();
        //AuthParam authParam = buildOauth2();
        //AuthParam authParam = buildHsJwt();
        //AuthParam authParam =buildRsJwt();
        AuthParam authParam =buildEsJwt();
        try {
            HttpReturn httpReturn = HttpCaller.doPost(url,headerParam,contentType, new ContentBody(output),authParam);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    /**
     * {"authType":"hmac-auth","hmacUserName":"ccccc","hmacSecret":"goIDYISSg33kRYz1M229vgcp82ezt8Rv"}
     * {"authType":"hmac-auth","hmacUserName":"ccccc","hmacSecret":"goIDYISSg33kRYz1M229vgcp82ezt8Rv","hmacAlgorithm":"hmac-sha256"}
     * {"authType":"hmac-auth","hmacUserName":"ccccc","hmacSecret":"goIDYISSg33kRYz1M229vgcp82ezt8Rv","hmacAlgorithm":"hmac-sha384"}
     * {"authType":"hmac-auth","hmacUserName":"ccccc","hmacSecret":"goIDYISSg33kRYz1M229vgcp82ezt8Rv","hmacAlgorithm":"hmac-sha512"}
     */
    @PostMapping("/testhamc")
    @ResponseBody
    public String TestHmac(@RequestBody AuthParam authParam, HttpServletRequest request) throws IOException {
        log.info("call test hamc api");
        String url="http://192.168.1.70/demo1/test";
        Map<String, String> headerParam = new HashMap<>();
        headerParam.put("auth","1234");
        Map<String, String> paramsMap = new HashMap<>();
        try {
            HttpReturn httpReturn= HttpCaller.doGet(url,headerParam,"",paramsMap,authParam);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    @PostMapping("/testHmacPost")
    @ResponseBody
    public String TestHmacPost(@RequestBody AuthParam authParam,HttpServletRequest request) throws IOException {
        log.info("call test api");
        String url="http://192.168.1.214/testfiveapi/user/add";
        Map<String, String> headerParam = new HashMap<>();
        //headerParam.put("auth","testauth");
        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("name", "testname");
//        paramsMap.put("age", "15");
//        paramsMap.put("id","1");
        try {
            HttpReturn httpReturn = HttpCaller.doPost(url,headerParam,"", paramsMap,authParam);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    @PostMapping("/testPostHmacJSON")
    @ResponseBody
    public String TestPostHmacJson(@RequestBody AuthParam authParam,HttpServletRequest request) throws IOException {
        log.info("call test api");
        String url="http://192.168.1.214/testfiveapi/user/tranformPost2/1";
        Map<String, String> headerParam = new HashMap<>();
        headerParam.put("auth","testauth");
        JSONObject requestJ = new JSONObject();
        requestJ.put("name", "testname2");
        requestJ.put("age", 12);
        try {
            HttpReturn httpReturn = HttpCaller.doPost(url,headerParam,"", new ContentBody(requestJ.toJSONString()),authParam);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    @PostMapping("/testPostHmacXml")
    @ResponseBody
    public String TestPostHmacXml(@RequestBody AuthParam authParam,HttpServletRequest request) throws IOException {
        log.info("call test api");
        String url="http://192.168.1.214/testfiveapi/user/demopostxml";
        Map<String, String> headerParam = new HashMap<>();
        headerParam.put("auth","testauth");
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("user2");
        Element name = root.addElement("name");
        Element password = root.addElement("password");
        name.addText("namexml");
        password.addText("123456");
        String output = document.asXML();
        String contentType="application/xml";
        try {
            HttpReturn httpReturn = HttpCaller.doPost(url,headerParam,contentType, new ContentBody(output),authParam);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    @GetMapping("/testPut")
    @ResponseBody
    public String TestPut(HttpServletRequest request) throws IOException {
        log.info("call test api");
        String url="http://192.168.1.214/testfourapi/user/tranformPut2/1";
        //String url="http://192.168.1.214/testfourapi/user/tranformPut/1";
        Map<String, String> headerParam = new HashMap<>();
        headerParam.put("auth","testauthput2");
        //AuthParam authParam = buildBasic();
        //AuthParam authParam = buildOauth2();
        //AuthParam authParam =buildHsJwt();
        //AuthParam authParam =buildRsJwt();
        //AuthParam authParam =buildEsJwt();
        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("name", "testname");
        paramsMap.put("age", "15");
        paramsMap.put("id", "1");
        JSONObject requestJ = new JSONObject();
        requestJ.put("name", "testname2");
        requestJ.put("age", 12);
        try {
            //HttpReturn httpReturn = HttpCaller.doPut(url,headerParam,"", paramsMap,null);
            HttpReturn httpReturn = HttpCaller.doPut(url,headerParam,"", new ContentBody(requestJ.toJSONString()),null);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    @GetMapping("/testPutXml")
    @ResponseBody
    public String TestPutXml(HttpServletRequest request) throws IOException {
        log.info("call test api");
        String url="http://192.168.1.214/testfourapi/user/demoputxml";
        Map<String, String> headerParam = new HashMap<>();
        headerParam.put("auth","testauth");
        JSONObject requestJ = new JSONObject();
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("user2");
        Element name = root.addElement("name");
        Element password = root.addElement("password");
        name.addText("namexml");
        password.addText("123456");
        String output = document.asXML();
        String contentType="application/xml";
        //AuthParam authParam = buildBasic();
        //AuthParam authParam = buildOauth2();
        //AuthParam authParam = buildHsJwt();
        //AuthParam authParam =buildRsJwt();
        //AuthParam authParam =buildEsJwt();
        try {
            HttpReturn httpReturn = HttpCaller.doPut(url,headerParam,contentType, new ContentBody(output),null);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    @GetMapping("/testPatch")
    @ResponseBody
    public String TestPatch(HttpServletRequest request) throws IOException {
        log.info("call test api");
        String url="http://192.168.1.214/testfourapi/user/tranformPatch2/1";
        //String url="http://192.168.1.214/testfourapi/user/tranformPatch/1";
        Map<String, String> headerParam = new HashMap<>();
        headerParam.put("auth","testauthpatch2");
        //AuthParam authParam = buildBasic();
        //AuthParam authParam = buildOauth2();
        //AuthParam authParam =buildHsJwt();
        //AuthParam authParam =buildRsJwt();
        //AuthParam authParam =buildEsJwt();
        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("name", "testname");
        paramsMap.put("age", "15");
        paramsMap.put("id", "1");
        JSONObject requestJ = new JSONObject();
        requestJ.put("name", "testname2");
        requestJ.put("age", 12);
        try {
            //HttpReturn httpReturn = HttpCaller.doPatch(url,headerParam,"", paramsMap,null);
            HttpReturn httpReturn = HttpCaller.doPatch(url,headerParam,"", new ContentBody(requestJ.toJSONString()),null);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    @GetMapping("/testPatchXml")
    @ResponseBody
    public String TestPatchXml(HttpServletRequest request) throws IOException {
        log.info("call test api");
        String url="http://192.168.1.214/testfourapi/user/demopatchxml";
        Map<String, String> headerParam = new HashMap<>();
        headerParam.put("auth","testPatchXml");
        JSONObject requestJ = new JSONObject();
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("user2");
        Element name = root.addElement("name");
        Element password = root.addElement("password");
        name.addText("namexml");
        password.addText("123456");
        String output = document.asXML();
        String contentType="application/xml";
        //AuthParam authParam = buildBasic();
        //AuthParam authParam = buildOauth2();
        //AuthParam authParam = buildHsJwt();
        //AuthParam authParam =buildRsJwt();
        //AuthParam authParam =buildEsJwt();
        try {
            HttpReturn httpReturn = HttpCaller.doPatch(url,headerParam,contentType, new ContentBody(output),null);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    @GetMapping("/testHead")
    @ResponseBody
    public String TestHead(HttpServletRequest request) throws IOException {
        log.info("call test api");
        String url="http://192.168.1.214/testfourapi/user/testHead";
        Map<String, String> headerParam = new HashMap<>();
        Map<String, String> paramsMap = new HashMap<>();
        try {
            HttpReturn httpReturn= HttpCaller.doHead(url,headerParam,"",paramsMap,null);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    @GetMapping("/testOptions")
    @ResponseBody
    public String TestOptions(HttpServletRequest request) throws IOException {
        log.info("call test api");
        String url="http://192.168.1.214/testfourapi/user/testOptions";
        Map<String, String> headerParam = new HashMap<>();
        Map<String, String> paramsMap = new HashMap<>();
        try {
            HttpReturn httpReturn= HttpCaller.doOptions(url,headerParam,"",paramsMap,null);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    @GetMapping("/testDelete")
    @ResponseBody
    public String TestDelete(HttpServletRequest request) throws IOException {
        log.info("call test api");
        String url="http://192.168.1.214/testfourapi/user/tranformDelete/1";
        Map<String, String> headerParam = new HashMap<>();
        Map<String, String> paramsMap = new HashMap<>();
        try {
            HttpReturn httpReturn= HttpCaller.doDelete(url,headerParam,"",paramsMap,null);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    @PostMapping("/testUpload")
    @ResponseBody
    public String TestUpload(HttpServletRequest request) throws IOException {
        log.info("call test api");
        //String url="http://192.168.1.214/testfourapi/user/tranformTest3/upload";
        String url = "http://127.0.0.1:8082/user/tranformTest3/upload";
        Map<String, String> headerParam = new HashMap<>();
        Map<String, String> paramsMap = new HashMap<>();
        MultipartHttpServletRequest params = ((MultipartHttpServletRequest) request);
        paramsMap.put("name", params.getParameter("name"));
        paramsMap.put("id", params.getParameter("id"));
        List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("file");
        MultipartFile file = null;
        try {
            for (int i = 0; i < files.size(); ++i) {
                file = files.get(i);

                if (!file.isEmpty()) {
                    InputStream in = file.getInputStream();
                    HttpReturn httpReturn = HttpCaller.doPost(url, headerParam, "",
                            paramsMap, file.getName(), in, ContentEncoding.none, null);
                    log.info(httpReturn.toString());
                    return JSONObject.toJSONString(httpReturn);
                } else {
                    log.info("You failed to upload " + i + " because the file was empty.");
                }
            }
            //String file = "/ltwork/csb-install/httpsdk1.7.jar";
            //byte[] fc = HttpCaller.readFileAsByteArray(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "hello";
    }

    @GetMapping("/testExport")
    @ResponseBody
    public void TestExport(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("call test api");
        String url="http://127.0.0.1:8082/user/exportUser";
        Map<String, String> headerParam = new HashMap<>();
        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("name","test");
        paramsMap.put("age","12");
        paramsMap.put("id","1");
        response.reset(); // 非常重要
        response.setHeader("Content-type", "text/csv; charset=UTF-8");
        try {
            HttpReturn httpReturn= HttpCaller.doGet(url,headerParam,"",paramsMap,null);
            httpReturn.getHeaderMap().get("fileName");
            response.setHeader("Content-Disposition", "attachment; filename=\""+httpReturn.getHeaderMap().get("fileName")+"\";charset=utf-8");
            //取文件流
            byte[] bytes = httpReturn.getResponseBytes();
            OutputStream out = response.getOutputStream();
            InputStream br = new ByteArrayInputStream(bytes);
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = br.read(buf)) > 0)
                out.write(buf, 0, len);
            br.close();
            out.close();
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/testError")
    @ResponseBody
    public String TestError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("call test api");
        String url="http://127.0.0.1:8082/user/hello500";
        Map<String, String> headerParam = new HashMap<>();
        Map<String, String> paramsMap = new HashMap<>();
        try {
            HttpReturn httpReturn= HttpCaller.doGet(url,headerParam,"",paramsMap,null);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
        }
        return "hello";
    }

    @GetMapping("/testSleep")
    @ResponseBody
    public String TestSleep(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("call test api");
        String url="http://127.0.0.1:8082/user/sleep/100000";
        Map<String, String> headerParam = new HashMap<>();
        Map<String, String> paramsMap = new HashMap<>();
        try {
            HttpReturn httpReturn= HttpCaller.doGet(url,headerParam,"",paramsMap,null);
            log.info(httpReturn.toString());
            return JSONObject.toJSONString(httpReturn);
        } catch (HttpCallerException e) {
            e.printStackTrace();
            String message = e.getCause().getMessage();
            log.info(message);
        }
        return "hello";
    }

    @GetMapping("/testInvoke")
    @ResponseBody
    public String Testinvoke(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("call test api");
        //nginx_worker_processes
        //KONG_DNS_RESOLVER
        HttpParameters.Builder builder = new HttpParameters.Builder();
        builder.requestURL("http://192.168.1.214/testfourapi/user/tranform/1") // 设置请求的URL
                .method("get"); // 设置调用方式, get/post
        AuthParam authParam = buildHsJwt();
        builder.authParam(authParam);

        try {
            builder.setContentEncoding(ContentEncoding.gzip);

            // 设置请求参数
            builder.putParamsMap("testNum", "12");
            builder.putHeaderParamsMap("auth", "testHeader");
            HttpReturn ret = HttpCaller.invokeReturn(builder.build());
            System.out.println("------- ret=" + JSON.toJSONString(ret));
            System.out.println("------- ret=" + ret.getResponseStr());
            return JSONObject.toJSONString(ret);
        } catch (HttpCallerException e) {
            // error process
            e.printStackTrace(System.out);
        }
        return "hello";
    }

    @GetMapping("/testInvokePost")
    @ResponseBody
    public String TestinvokePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("call test api");
        HttpParameters.Builder builder = new HttpParameters.Builder();
        //builder.requestURL("http://192.168.1.214/demoSvcApi01/user/tranformPost/1") // 设置请求的URL
        builder.requestURL("http://127.0.0.1:8082/user/tranformPost/1")
                .method("POST");
                //.contentType("application/x-www-form-urlencoded;charset=utf-8");
        try {
            builder.setContentEncoding(ContentEncoding.none);
            // 设置请求参数
            builder.putParamsMap("name", "testname");
            builder.putParamsMap("age", "15");
            builder.putParamsMap("id", "1");
            builder.putHeaderParamsMap("auth", "testHeader");
            AuthParam authParam = new AuthParam();
            authParam.setAuthType("basic-auth");
            authParam.setUsername("demo01");
            authParam.setPassword("8vRv~ey&o");
            builder.authParam(authParam);
            HttpReturn ret = HttpCaller.invokeReturn(builder.build());
            System.out.println("------- ret=" + JSON.toJSONString(ret));
            System.out.println("------- ret=" + ret.getResponseStr());
            return JSONObject.toJSONString(ret);
        } catch (HttpCallerException e) {
            // error process
            e.printStackTrace(System.out);
        }
        return "hello";
    }
}
