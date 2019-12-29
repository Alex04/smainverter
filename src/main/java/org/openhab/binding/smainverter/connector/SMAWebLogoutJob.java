package org.openhab.binding.smainverter.connector;

import java.lang.reflect.Type;
import java.util.HashMap;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class SMAWebLogoutJob extends SMAAbstractRemoteJob implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(WebLoginJob.class);

    private static final String SMA_LOGOUT_RELATIVE_URL = "/dyn/logout.json";

    private String sessionID;

    private String getLoginURL() {
        return getURLForPath(SMA_LOGOUT_RELATIVE_URL);
    }

    @Override
    public void run() {
        HttpClient httpClient = new HttpClient(new SslContextFactory(true));
        httpClient.setFollowRedirects(true);

        Request request = createJSONRequest(SMA_LOGOUT_RELATIVE_URL, HttpMethod.POST, this.sessionID);
        executeRequest(request);
    }

    @Override
    void responseWithHTTPError(Request request, ContentResponse response, String errorMsg) {
        // TODO Auto-generated method stub

    }

    @Override
    void responseWithHTTPSuccess(Request request, ContentResponse response) {
        // Parse Json to really check if response was ok an logout was successful
        // Json response by SMA is:
        // {"result": {"isLogin": false}}
        Gson gson = new Gson();
        String responseString = response.getContentAsString();
        Type jsonType = new TypeToken<HashMap<String, HashMap<String, Boolean>>>() {
        }.getType();
        HashMap<String, HashMap<String, Boolean>> responseMap = gson.fromJson(responseString, jsonType);
        if (responseMap.get("result").get("isLogin") == false) {
            logger.debug("logout successful");
        } else {
            logger.debug("something during logout process failed. Json response was: " + responseString);
        }
    }
}
