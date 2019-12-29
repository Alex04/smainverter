package org.openhab.binding.smainverter.connector;

import org.openhab.binding.smainverter.internal.SMAInverterConfig;
import org.openhab.binding.smainverter.internal.SMAInverterHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SMAInverterWebConnector {

    private final Logger logger = LoggerFactory.getLogger(SMAInverterWebConnector.class);

    private SMAInverterConfig config;

    private Runnable loginJob;

    private Runnable logoutJob;

    private Runnable dataJob;

    public SMAInverterWebConnector(SMAInverterHandler handler, SMAInverterConfig config,
            SMAInverterLoginCallback loginCallback) {
        this.loginJob = new WebLoginJob(loginCallback, config.inverterip, config.password);
        this.config = config;
    }

    private boolean preCheck() {
        if (this.config.inverterip != null && this.config.inverterip.isEmpty()) {
            logger.debug("It seems that no network address has been set:" + config.inverterip);
            return false;
            // updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.CONFIGURATION_ERROR);
        } else if (this.config.password != null && this.config.password.isEmpty()) {
            logger.warn("Password has not been set, either no password or configuration error");
            return true;
        } else {
            logger.debug("Network address has been set:" + config.inverterip + "thing set thing to online");
            return true;
            // updateStatus(ThingStatus.ONLINE);
        }
    }

    public Runnable getLoginJob() {
        return loginJob;
    }

    public void setLoginJob(Runnable loginJob) {
        this.loginJob = loginJob;
    }

    public Runnable getDataJob(SMAInverterDataUpdateCallback dataCallback, String sessionID) {
        this.dataJob = (new WebRequestJob(dataCallback, config.inverterip, sessionID));
        return dataJob;
    }

}
