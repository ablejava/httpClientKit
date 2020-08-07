package kit;

import entity.Response;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.EncodingUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class HttpClientKit {

    public String httpclientGet(String url, Map<String, String> parameterMap, String accessToken) {
        String result = null;

        HttpClient client = null;

        HttpMethodBase method = null;
        try {
            client = new HttpClient();
            client.getHttpConnectionManager().getParams().setConnectionTimeout(15000);
            client.getHttpConnectionManager().getParams().setSoTimeout(15000);

            method = new GetMethod(url);
            method.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
            method.setRequestHeader("Accept", "application/json;charset=UTF-8");
            if (StringUtils.isNotEmpty(accessToken)) {
                method.setRequestHeader("access-Token", accessToken);
            }

            generateNameValuePair(method, parameterMap);

            client.executeMethod(method);
            int code = method.getStatusCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
            StringBuffer sb = new StringBuffer();
            String str= "";
            while ((str = reader.readLine()) != null) {
                sb.append(str);

            }
            result = sb.toString();



        } catch (Exception e){
            e.printStackTrace();
        } finally {
            method.releaseConnection();
            client.getHttpConnectionManager().closeIdleConnections(0);
            return result;
        }
    }


    public String httpclientPost(String url, String paramsJson, Map<String, String> urlParams, boolean ssl) {

        Response response = new Response();

        // 设置请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json;charset=UTF-8");
        headers.put("Content-Type", "application/json");
        headers.put("Accept-Encoding", "identity");

        // 设置请求token
//        Map<String, String> urlParams = new HashMap<>();
//        urlParams.put("access_toke", "a");
//        urlParams.put("request_id", UUID.randomUUID().toString());


        String postJson = doPostJson(url, headers, urlParams, paramsJson, ssl, new ResponseHandler<String>() {
            @Override
            public String handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {
                return EntityUtils.toString(httpResponse.getEntity());
            }
        });
        return postJson;
    }

    private static <T> T doPostJson(String url, Map<String, String> headers, Map<String, String> urlParams, String paramsJson, boolean ssl, ResponseHandler<T> handler) {

        T resp = null;

        try {
            // 设置请求参数
            if (MapUtils.isNotEmpty(urlParams)) {
                StringBuilder sb = new StringBuilder(url);
                sb.append("?");
                urlParams.forEach((key, value) ->{
                    sb.append(key).append("=").append(value).append("&");
                });
                url = StringUtils.chomp(sb.toString(), "&");
            }

            CloseableHttpClient httpClient = ssl ? sslClient() : client();
            HttpPost post = new HttpPost(url);

            // 设置请求头
            headers.forEach((key, value) ->{
                post.addHeader(key, value);
            });

            HttpEntity entity = EntityBuilder.create().setText(paramsJson).setContentType(ContentType.APPLICATION_JSON).setContentEncoding("UTF-8").build();

            post.setEntity(entity);

            resp = httpClient.execute(post, handler);
        } catch (Exception e){

            e.printStackTrace();
        }
        return resp;
    }

    private static CloseableHttpClient client() throws Exception{
        return HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom().setConnectionRequestTimeout(500).setConnectTimeout(5000).setSocketTimeout(30000).build()).build();

    }

    private static CloseableHttpClient sslClient() throws Exception{
        SSLContext ctx = SSLContext.getInstance("TLS");
        X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
        ctx.init(null, new TrustManager[]{tm}, new SecureRandom());
        return HttpClients.custom().setDefaultRequestConfig(RequestConfig.custom().setConnectionRequestTimeout(5000).setConnectTimeout(5000).setSocketTimeout(30000).build()).setSSLContext(ctx).build();
    }


    private static  HttpMethodBase generateNameValuePair(HttpMethodBase method, Map<String, String> properties) {
        NameValuePair[] nameValuePairs = new NameValuePair[properties.size()];
        int i =0;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            nameValuePairs[i++] = new NameValuePair(entry.getKey(), entry.getValue());
        }
        method.setQueryString(EncodingUtil.formUrlEncode(nameValuePairs, "UTF-8"));
        return method;
    }

}
