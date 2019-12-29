package org.openhab.binding.smainverter.connector;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SMAAbstractRemoteJob {

    private final Logger logger = LoggerFactory.getLogger(WebLoginJob.class);

    private static final String SMA_KEY_PARAM_SESSIONID = "sid";

    protected String connectionURL;
    protected HttpClient httpClient;

    protected String getURLForPath(String relativePath) {
        String totalURL = this.connectionURL;

        if (totalURL.startsWith("http://")) {
            totalURL.replace("http://", "https://");
        } else if (!totalURL.startsWith("https://")) {
            totalURL = "https://" + totalURL;
        }

        if (totalURL.endsWith("/")) {
            totalURL = totalURL.substring(0, totalURL.length() - 1);
            totalURL += relativePath;
        } else {
            totalURL += relativePath;
        }

        return totalURL;
    }

    protected Request createJSONRequest(String url, String sessionID) {
        Request request = createJSONRequest(url);
        request.param(SMA_KEY_PARAM_SESSIONID, sessionID);
        return request;
    }

    protected Request createJSONRequest(String url) {
        this.httpClient = new HttpClient(new SslContextFactory(true));
        this.httpClient.setFollowRedirects(true);
        Request request = httpClient.newRequest(url);
        request.method(HttpMethod.POST);
        request.header(HttpHeader.CONTENT_TYPE, SMAWebConnectorConstants.CONTENT_TYPE_JSON);
        request.header(HttpHeader.ACCEPT, SMAWebConnectorConstants.CONTENT_TYPE_JSON);
        return request;
    }

    protected void executeRequest(Request request) {
        try {
            this.httpClient.start();
            ContentResponse response = request.send();
            String responseString = response.getContentAsString();
            String errorMessage = "Wasn't able to get total and current production value - ";

            if (responseString == null || responseString.isEmpty()) {
                errorMessage += "response is null";
                logger.debug(errorMessage);
                this.responseWithHTTPError(request, response, errorMessage);
            } else if (response.getStatus() == HttpStatus.UNAUTHORIZED_401) {
                errorMessage += "authorization failed" + response.getStatus();
                logger.debug(errorMessage);
                this.responseWithHTTPError(request, response, errorMessage);
            } else if (response.getStatus() != HttpStatus.OK_200) {
                errorMessage += "something went wrong: " + response.getContentAsString() + " HTTP Status code "
                        + response.getStatus();
                logger.debug(errorMessage);
                this.responseWithHTTPError(request, response, errorMessage);
            } else {
                logger.debug("Response successful");
                this.responseWithHTTPSuccess(request, response);
            }
        } catch (Exception e) {
            logger.debug("Something went wrong" + e.getMessage());
        } finally {
            try {
                this.httpClient.stop();
            } catch (Exception e) {
                logger.debug("Something went wrong when trying to close the http client" + e.getMessage());
            }
        }
    }

    abstract void responseWithHTTPError(Request request, ContentResponse response, String errorMsg);

    abstract void responseWithHTTPSuccess(Request request, ContentResponse response);
}
