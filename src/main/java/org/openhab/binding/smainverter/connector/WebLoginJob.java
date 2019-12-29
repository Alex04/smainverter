package org.openhab.binding.smainverter.connector;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class WebLoginJob implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(WebLoginJob.class);

    private static final String SMA_LOGIN_KEY_RESULT = "result";
    private static final String SMA_LOGIN_KEY_SESSIONID = "sid";

    private static final String SMA_LOGIN_KEY_RIGHT = "right";
    private static final String SMA_LOGIN_KEY_RIGHT_USR = "usr";
    private static final String SMA_LOGIN_KEY_PASSWORD = "pass";
    private static final String SMA_LOGIN_RELATIVE_URL = "/dyn/login.json";

    private String connectionURL;
    private String password;
    private SMAInverterLoginCallback callback;

    public WebLoginJob(SMAInverterLoginCallback callback, String connecionURL, String password) {
        super();
        this.callback = callback;
        this.connectionURL = connecionURL;
        this.password = password;
    }

    private String getLoginURL() {
        String loginURL = this.connectionURL;

        if (loginURL.startsWith("http://")) {
            loginURL.replace("http://", "https://");
        } else if (!loginURL.startsWith("https://")) {
            loginURL = "https://" + loginURL;
        }

        if (loginURL.endsWith("/")) {
            loginURL = loginURL.substring(0, loginURL.length() - 1);
            loginURL += SMA_LOGIN_RELATIVE_URL;
        } else {
            loginURL += SMA_LOGIN_RELATIVE_URL;
        }

        return loginURL;
    }

    @Override
    public void run() {
        Gson gson = new Gson();

        HttpClient httpClient = new HttpClient(new SslContextFactory(true));
        httpClient.setFollowRedirects(true);

        Request request = httpClient.newRequest(getLoginURL());
        request.method(HttpMethod.POST);
        request.header(HttpHeader.CONTENT_TYPE, SMAWebConnectorConstants.CONTENT_TYPE_JSON);
        request.header(HttpHeader.ACCEPT, SMAWebConnectorConstants.CONTENT_TYPE_JSON);

        HashMap<String, String> requestJSONMap = new HashMap<String, String>();
        requestJSONMap.put(SMA_LOGIN_KEY_RIGHT, SMA_LOGIN_KEY_RIGHT_USR);
        requestJSONMap.put(SMA_LOGIN_KEY_PASSWORD, this.password);

        request.content(new StringContentProvider(SMAWebConnectorConstants.CONTENT_TYPE_JSON,
                gson.toJson(requestJSONMap), StandardCharsets.UTF_8));

        try {
            httpClient.start();
            ContentResponse response = request.send();
            String responseString = response.getContentAsString();
            String errorMessage = null;

            if (responseString == null || responseString.isEmpty()) {
                errorMessage = "Login failed, response content was empty:";
                logger.debug(errorMessage);
                this.callback.loginFailed(errorMessage);
            } else if (response.getStatus() == HttpStatus.UNAUTHORIZED_401) {
                errorMessage = "Login failed, http status code:" + response.getStatus();
                logger.debug(errorMessage);
                callback.loginFailed(errorMessage);
            } else if (response.getStatus() != HttpStatus.OK_200) {
                errorMessage = "Something went wrong: " + response.getContentAsString() + " HTTP Status code "
                        + response.getStatus();
                logger.debug(errorMessage);
                this.callback.loginFailed(errorMessage);
                ;
            } else {
                logger.debug("Login successful");

                Type jsonType = new TypeToken<HashMap<String, HashMap<String, String>>>() {
                }.getType();
                HashMap<String, HashMap<String, String>> responseMap = gson.fromJson(responseString, jsonType);
                String sessionId = responseMap.get(SMA_LOGIN_KEY_RESULT).get(SMA_LOGIN_KEY_SESSIONID);
                logger.debug("Session Id:" + sessionId);
                callback.loginSuccessful(sessionId);
            }
        } catch (Exception e) {
            callback.loginFailed(e.toString());
        } finally {
            try {
                httpClient.stop();
            } catch (Exception e1) {
                logger.debug("Something went wrong when trying to close the http client" + e1.getMessage());
            }
        }
    }
}