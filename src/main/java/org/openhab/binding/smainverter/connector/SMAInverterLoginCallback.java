package org.openhab.binding.smainverter.connector;

public interface SMAInverterLoginCallback {

    public void loginSuccessful(String sessionId);

    public void loginFailed(String errorMessage);

}
