package org.openhab.binding.smainverter.connector;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    }
}
