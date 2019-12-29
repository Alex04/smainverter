package org.openhab.binding.smainverter.connector;

public interface SMAInverterDataUpdateCallback {

    public void onTotalProductionReceived(Number totalProduction);

    public void ondailyProductionReceived(Number dailyProduction);

    public void onCurrentProductionReceived(Number currentProduction);

    public void onDataUpdateFailed(String errorMessage);

}
