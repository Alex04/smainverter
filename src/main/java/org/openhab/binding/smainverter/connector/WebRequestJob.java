package org.openhab.binding.smainverter.connector;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class WebRequestJob extends SMAAbstractRemoteJob implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(WebLoginJob.class);

    private static final String SMA_TOTAL_CURRENT_URL = "/dyn/getValues.json";
    private static final String SMA_DAILY_URL = "/dyn/getLogger.json";

    private static final String SMA_KEY_DATA_START_TIME = "tStart";
    private static final String SMA_KEY_DATA_END_TIME = "tEnd";
    private static final String SMA_KEY_DEST_DEV = "destDev";
    private static final String SMA_KEY_KEY = "key";
    private static final String SMA_KEY_KEYS = "keys";

    private static final String SMA_KEY_RESULT = "result";
    private static final String SMA_KEY_INVERTER_ID = "0198-B32BABB8";
    private static final String SMA_KEY_CURRENT_PROD = "6100_40263F00";
    private static final String SMA_KEY_TOTAL_PROD = "6400_00260100";

    private SMAInverterDataUpdateCallback dataCallback;
    private String sessionID;

    public WebRequestJob(SMAInverterDataUpdateCallback callback, String connectionURL, String sessionId) {
        super();
        this.dataCallback = callback;
        this.connectionURL = connectionURL;
        this.sessionID = sessionId;
    }

    private String getTotalProductionURL() {
        return getURLForPath(SMA_TOTAL_CURRENT_URL);
    }

    private String getDailyProductionIRL() {
        return getURLForPath(SMA_DAILY_URL);
    }

    private void configureTotalCurrentProductionRequest(Request request) {
        Gson gson = new Gson();
        HashMap<String, ArrayList<String>> requestJSONMap = new HashMap<String, ArrayList<String>>();
        requestJSONMap.put(SMA_KEY_DEST_DEV, new ArrayList<String>());
        requestJSONMap.put(SMA_KEY_KEYS,
                new ArrayList<String>(Arrays.asList(SMA_KEY_CURRENT_PROD, SMA_KEY_TOTAL_PROD)));
        request.content(new StringContentProvider(SMAWebConnectorConstants.CONTENT_TYPE_JSON,
                gson.toJson(requestJSONMap), StandardCharsets.UTF_8));
        logger.debug("Gson Payload for total production: " + gson.toJson(requestJSONMap).toString());
    }

    private void configureDailyProductionRequest(Timestamp start, Timestamp end, Request request) {
        // SMA currently uses the logger like
        // {"result":{"0198-B32BABB8":{"6100_40263F00":{"1":[{"val":null}]},"6400_00260100":{"1":[{"val":616391}]}}}}
        Gson gson = new Gson();
        HashMap<String, Object> requestJSONMap = new HashMap<String, Object>();
        requestJSONMap.put(SMA_KEY_DEST_DEV, new ArrayList<String>());
        requestJSONMap.put(SMA_KEY_KEY, 28672);
        requestJSONMap.put(SMA_KEY_DATA_START_TIME, start.getTime() / 1000);
        requestJSONMap.put(SMA_KEY_DATA_END_TIME, end.getTime() / 1000);

        request.content(new StringContentProvider(SMAWebConnectorConstants.CONTENT_TYPE_JSON,
                gson.toJson(requestJSONMap), StandardCharsets.UTF_8));
        logger.debug("Gson Payload for daily production: " + gson.toJson(requestJSONMap).toString());
    }

    private void refreshTotalCurrentProduction(Gson gson, String responseString) {
        // response is like:
        // {"result":{"0198-B32BABB8":{"6100_40263F00":{"1":[{"val":null}]}}}}
        Type jsonType = new TypeToken<HashMap<String, HashMap<String, HashMap<String, HashMap<String, ArrayList<HashMap<String, Number>>>>>>>() {
        }.getType();
        HashMap<String, HashMap<String, HashMap<String, HashMap<String, ArrayList<HashMap<String, Number>>>>>> responseMap = gson
                .fromJson(responseString, jsonType);

        Number currentProduction = responseMap.get(SMA_KEY_RESULT).get(SMA_KEY_INVERTER_ID).get(SMA_KEY_CURRENT_PROD)
                .get("1").get(0).get("val");

        Number totalProduction = responseMap.get(SMA_KEY_RESULT).get(SMA_KEY_INVERTER_ID).get(SMA_KEY_TOTAL_PROD)
                .get("1").get(0).get("val");

        this.dataCallback.onCurrentProductionReceived(currentProduction == null ? 0 : currentProduction);
        this.dataCallback.onTotalProductionReceived(totalProduction == null ? 0 : totalProduction);
    }

    private void refreshDailyProduction(Gson gson, String responseString) {
        Type jsonType = new TypeToken<HashMap<String, HashMap<String, ArrayList<HashMap<String, Number>>>>>() {
        }.getType();
        HashMap<String, HashMap<String, ArrayList<HashMap<String, Number>>>> responseMap = gson.fromJson(responseString,
                jsonType);
        ArrayList<HashMap<String, Number>> dailyProductionList = responseMap.get(SMA_KEY_RESULT)
                .get(SMA_KEY_INVERTER_ID);

        ArrayList<Integer> intList = new ArrayList<Integer>();

        for (HashMap<String, Number> map : dailyProductionList) {
            if (map.get("v") != null) {
                intList.add(map.get("v").intValue());
            }
        }

        Integer dailyProductionResult = Collections.max(intList) - Collections.min(intList);

        this.dataCallback.ondailyProductionReceived(dailyProductionResult);
    }

    @Override
    public void run() {
        Request request = null;
        request = createJSONRequest(getTotalProductionURL(), HttpMethod.POST, this.sessionID);
        configureTotalCurrentProductionRequest(request);
        logger.debug("First we want to get the total amount produced");
        executeRequest(request);
        logger.debug("No we want to get the daily ");
        request = createJSONRequest(getDailyProductionIRL(), HttpMethod.POST, this.sessionID);
        configureDailyProductionRequest(getMidnightOfYesterday(), getMidnightOfToday(), request);
        executeRequest(request);
    }

    @Override
    void responseWithHTTPError(Request request, ContentResponse response, String errorMsg) {
        // TODO Auto-generated method stub

    }

    @Override
    void responseWithHTTPSuccess(Request request, ContentResponse response) {
        Gson gson = new Gson();
        String responseString = response.getContentAsString();
        if (request.getURI().toString().contains(getTotalProductionURL())) {
            refreshTotalCurrentProduction(gson, responseString);
        } else {
            refreshDailyProduction(gson, responseString);
        }
    }

    private Timestamp getMidnightOfToday() {
        // today
        TimeZone tz = TimeZone.getTimeZone("Europe/Berlin");
        TimeZone.setDefault(tz);
        Calendar date = Calendar.getInstance(tz, Locale.GERMAN);
        // reset hour, minutes, seconds and millis
        date.set(Calendar.HOUR_OF_DAY, 24);
        date.set(Calendar.MINUTE, -5);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        return new Timestamp(date.getTimeInMillis());
    }

    private Timestamp getMidnightOfYesterday() {
        // today
        TimeZone tz = TimeZone.getTimeZone("Europe/Berlin");
        TimeZone.setDefault(tz);
        Calendar date = Calendar.getInstance(tz, Locale.GERMAN);
        // reset hour, minutes, seconds and millis
        date.add(Calendar.DAY_OF_MONTH, -1);
        date.set(Calendar.HOUR_OF_DAY, 24);
        date.set(Calendar.MINUTE, -5);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        return new Timestamp(date.getTimeInMillis());
    }
}