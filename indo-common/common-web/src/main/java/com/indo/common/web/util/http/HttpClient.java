package com.indo.common.web.util.http;


import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpClient {

    private static Logger logger = LoggerFactory.getLogger(HttpClient.class);

    private static void trustAllHosts() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
        }};
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    /**
     * ??????Http POST??????
     *
     * @param url
     * @param param
     * @param header
     * @return
     */
    public static String sendPost(String url, String param, Map<String, String> header) {
        OutputStreamWriter out = null;
        BufferedReader in = null;
        String result = "";
        HttpURLConnection conn;

        try {
            trustAllHosts();
            URL realUrl = new URL(url);
            // ????????????????????????????????????(http?????????https)
            if ("https".equals(realUrl.getProtocol().toLowerCase())) {
                HttpsURLConnection https = (HttpsURLConnection) realUrl.openConnection();
                https.setHostnameVerifier(DO_NOT_VERIFY);
                conn = https;
            } else {
                conn = (HttpURLConnection) realUrl.openConnection();
            }
            // ???????????????????????????
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            conn.setRequestProperty("Content-Type", "text/plain;charset=utf-8");
            // ??????POST??????????????????????????????

            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");    // POST??????
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            if (header != null) {
                for (Map.Entry entry : header.entrySet()) {
                    conn.setRequestProperty(entry.getKey().toString(), entry.getValue().toString());
                }
            }
            // ???????????????????????????
            conn.connect();

            // ??????URLConnection????????????????????????
            out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            // ??????????????????
            out.write(param);
            // flush??????????????????
            out.flush();
            // ??????BufferedReader??????????????????URL?????????
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            logger.error("?????? POST ?????????????????????===" + url + "===params===" + param + "===Error:" + e);
        }

        //??????finally?????????????????????????????????
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    public static String getRequestBodyFromMap(Map parametersMap, boolean isUrlEncoding) {
        StringBuffer sbuffer = new StringBuffer();
        for (Object obj : parametersMap.keySet()) {
            String value = (String) parametersMap.get(obj);
            if (isUrlEncoding) {
                try {
                    value = URLEncoder.encode(value, "UTF-8");
                    if (value != null && value.isEmpty() == false) {
                        parametersMap.put(obj, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            sbuffer.append(obj).append("=").append(value).append("&");
        }
        return sbuffer.toString().replaceAll("&$", "");
    }

    public static String formPost(String url, Map<String, String> params) {
        return formPost(url, params, null);
    }

    public static String formPost(String url, Map<String, String> params, Map<String, String> headers) {
        // ??????Httpclient??????
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        String resultString = "";
        try {
            // ??????Http Post??????
            HttpPost httpPost = new HttpPost(url);
            // ??????????????????
            if (params != null && params.size() > 0) {
                List<NameValuePair> paramList = new ArrayList<>();
                for (String key : params.keySet()) {
                    paramList.add(new BasicNameValuePair(key, params.get(key)));
                }
                // ????????????
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(paramList, Charset.forName("UTF-8"));
                httpPost.setEntity(entity);
            }

            if (null != headers && headers.size() > 0) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }
            }
            // ??????http??????
            response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            resultString = EntityUtils.toString(entity, "utf-8");
            if(StringUtils.isEmpty(resultString)){
                org.apache.http.Header locationHeader = response.getFirstHeader("Location");
                if(locationHeader!=null){
                    resultString =JSONObject.toJSONString(locationHeader);
                }
            }
            EntityUtils.consume(entity); // ??????????????????
        } catch (Exception e) {
            logger.error("http formPost occur error. e {} url:{}, params:{} ", e, url, JSONObject.toJSONString(params));
        } finally {
            closeResponse(response, url, JSONObject.toJSONString(params));
        }
        return resultString;
    }

    public static void closeResponse(CloseableHttpResponse response, String url, Object params) {
        if (null != response) {
            try {
                response.close();
            } catch (IOException e) {
                logger.error("closeResponse occur error, url:{}, params:{}", url, params, e);
            }
        }
    }


    /**
     * post??????
     * @param url
     * @param json
     * @return
     */
    public static JSONObject doPost(String url,String json){

        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(url);
        JSONObject response = null;
        try {
            StringEntity s = new StringEntity(json);
            s.setContentEncoding("UTF-8");
            s.setContentType("application/json");//??????json??????????????????contentType
            post.setEntity(s);
            HttpResponse res = httpclient.execute(post);
            if(res.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                String result = EntityUtils.toString(res.getEntity());// ??????json?????????
                response = JSONObject.parseObject(result);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return response;
    }


}
