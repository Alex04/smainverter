package org.openhab.binding.smainverter.connector;

import java.lang.reflect.Type;
import java.util.HashMap;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class WebLoginJob extends SMAAbstractRemoteJob implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(WebLoginJob.class);

    private static final String SMA_LOGIN_KEY_RESULT = "result";
    private static final String SMA_LOGIN_KEY_SESSIONID = "sid";

    private static final String SMA_LOGIN_KEY_RIGHT = "right";
    private static final String SMA_LOGIN_KEY_RIGHT_USR = "usr";
    private static final String SMA_LOGIN_KEY_PASSWORD = "pass";
    private static final String SMA_LOGIN_RELATIVE_URL = "/dyn/login.json";

    private String password;
    private SMAInverterLoginCallback callback;

    public WebLoginJob(SMAInverterLoginCallback callback, String connecionURL, String password) {
        super();
        this.callback = callback;
        this.connectionURL = connecionURL;
        this.password = password;
    }

    private String getLoginURL() {
        return getURLForPath(SMA_LOGIN_RELATIVE_URL);
    }

    @Override
    public void run() {
        Request request = createJSONRequest(getLoginURL(), HttpMethod.POST);
        HashMap<String, String> requestJSONMap = new HashMap<String, String>();
        requestJSONMap.put(SMA_LOGIN_KEY_RIGHT, SMA_LOGIN_KEY_RIGHT_USR);
        requestJSONMap.put(SMA_LOGIN_KEY_PASSWORD, this.password);
        executeRequest(request);
    }

    @Override
    void responseWithHTTPError(Request request, ContentResponse response, String errorMsg) {
        this.callback.loginFailed(errorMsg);
    }

    @Override
    void responseWithHTTPSuccess(Request request, ContentResponse response) {
        Gson gson = new Gson();
        String responseString = response.getContentAsString();
        Type jsonType = new TypeToken<HashMap<String, HashMap<String, String>>>() {
        }.getType();
        HashMap<String, HashMap<String, String>> responseMap = gson.fromJson(responseString, jsonType);
        String sessionId = responseMap.get(SMA_LOGIN_KEY_RESULT).get(SMA_LOGIN_KEY_SESSIONID);
        logger.debug("Session Id:" + sessionId);
        callback.loginSuccessful(sessionId);
    }
}