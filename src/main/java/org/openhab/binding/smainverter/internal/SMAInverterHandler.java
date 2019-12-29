/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.smainverter.internal;

import java.util.concurrent.TimeUnit;

import javax.measure.quantity.Energy;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.smainverter.connector.SMAInverterDataUpdateCallback;
import org.openhab.binding.smainverter.connector.SMAInverterLoginCallback;
import org.openhab.binding.smainverter.connector.SMAInverterWebConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SMAInverterHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexander Mack - Initial contribution
 */

public class SMAInverterHandler extends BaseThingHandler
        implements SMAInverterLoginCallback, SMAInverterDataUpdateCallback {

    private final Logger logger = LoggerFactory.getLogger(SMAInverterHandler.class);

    private Number cachedCurrentProd;
    private Number cachedTotalProd;
    private Number cachedDailyProd;

    @Nullable
    private SMAInverterConfig config;

    @Nullable
    private SMAInverterWebConnector webConnector;

    public SMAInverterHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handleCommand received for channel " + channelUID + " command: " + command);
        // if nothing cached -> nothing to update
        boolean cacheAvailable = notNull(this.cachedCurrentProd, this.cachedDailyProd, this.cachedTotalProd);

        if (command instanceof RefreshType && cacheAvailable) {
            switch (channelUID.getId()) {
                case SMAInverterBindingConstants.CHANNEL_CURRENT:
                    logger.debug("updating state for channel " + SMAInverterBindingConstants.CHANNEL_CURRENT);
                    updateState(SMAInverterBindingConstants.CHANNEL_CURRENT, getEnergyForValue(this.cachedCurrentProd));
                    break;
                case SMAInverterBindingConstants.CHANNEL_TOTAL:
                    logger.debug("updating state for channel " + SMAInverterBindingConstants.CHANNEL_TOTAL);
                    updateState(SMAInverterBindingConstants.CHANNEL_TOTAL, getEnergyForValue(this.cachedTotalProd));
                    break;
                case SMAInverterBindingConstants.CHANNEL_DAILY:
                    logger.debug("updating state for channel " + SMAInverterBindingConstants.CHANNEL_DAILY);
                    // updateState(SMAInverterBindingConstants.CHANNEL_DAILY, getEnergyForValue(this.cachedDailyProd));
                    updateState(SMAInverterBindingConstants.CHANNEL_DAILY,
                            new DecimalType(this.cachedDailyProd.doubleValue()));
                    break;
                default:
                    String cacheMsg = cacheAvailable ? " not available" : " available";
                    logger.debug("no update for command possible, cache " + cacheMsg);
            }
        }
    }

    /*
     * @Override
     * protected void updateState(@NonNull String channelID, @NonNull State state) {
     * State newState = state;
     * switch (channelID) {
     * case SMAInverterBindingConstants.CHANNEL_DAILY:
     * case SMAInverterBindingConstants.CHANNEL_CURRENT:
     * case SMAInverterBindingConstants.CHANNEL_TOTAL:
     * newState = new DecimalType(((DecimalType) state).doubleValue() / 1000);
     * }
     * super.updateState(channelID, newState);
     * }
     */
    private boolean notNull(Object... args) {
        for (Object current : args) {
            if (current == null) {
                return false;
            }
        }
        return true;
    }

    private QuantityType<Energy> getEnergyForValue(Number value) {
        Number kwh = (float) value.intValue() / 1000;
        QuantityType<Energy> quantityType = new QuantityType<Energy>(kwh, SmartHomeUnits.KILOWATT_HOUR);
        return quantityType;
    }

    @Override
    public void initialize() {
        this.config = getConfigAs(SMAInverterConfig.class);
        this.webConnector = new SMAInverterWebConnector(this, this.config, this);

        if (this.config.inverterip != null && this.config.inverterip.isEmpty()) {
            logger.debug("It seems that no network address has been set:" + config.inverterip);
            updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.CONFIGURATION_ERROR);
        } else {
            logger.debug("Network address has been set:" + config.inverterip + "thing set thing to online");
            updateStatus(ThingStatus.ONLINE);
        }

        scheduler.schedule(webConnector.getLoginJob(), 0, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    public void setStatusInfo(ThingStatus status, ThingStatusDetail statusDetail, String description) {
        super.updateStatus(status, statusDetail, description);
    }

    @Override
    public void loginFailed(String errorMessage) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, errorMessage);
    }

    @Override
    public void loginSuccessful(String sessionId) {
        updateStatus(ThingStatus.ONLINE);
        scheduler.scheduleAtFixedRate(webConnector.getDataJob(this, sessionId), 0, 60, TimeUnit.SECONDS);
    }

    @Override
    public void onCurrentProductionReceived(Number currentProduction) {
        logger.debug("Current Production received:" + currentProduction);
        this.cachedCurrentProd = currentProduction;
        updateState(SMAInverterBindingConstants.CHANNEL_CURRENT, getEnergyForValue(currentProduction));
    }

    @Override
    public void ondailyProductionReceived(Number dailyProduction) {
        logger.debug("Daily Production received:" + dailyProduction);
        this.cachedDailyProd = dailyProduction;
        updateState(SMAInverterBindingConstants.CHANNEL_DAILY, getEnergyForValue(dailyProduction));
        // updateState(SMAInverterBindingConstants.CHANNEL_DAILY, new DecimalType(dailyProduction.doubleValue()));
    }

    @Override
    public void onTotalProductionReceived(Number totalProduction) {
        logger.debug("Total Production received:" + totalProduction);
        this.cachedTotalProd = totalProduction;
        updateState(SMAInverterBindingConstants.CHANNEL_TOTAL, getEnergyForValue(totalProduction));
    }

    @Override
    public void onDataUpdateFailed(String errorMessage) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, errorMessage);
    }
}