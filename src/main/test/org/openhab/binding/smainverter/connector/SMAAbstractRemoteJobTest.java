package org.openhab.binding.smainverter.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SMAAbstractRemoteJobTest {

    @Test
    public void testGetURLForPath() {
        String ip = "128.1.1.0";
        String path = "/test/path";
        WebLoginJob loginJob = new WebLoginJob(null, ip, null);
        assertEquals("https://" + ip + path, loginJob.getURLForPath(path));
        ip = "http://128.1.1.0";
        loginJob = new WebLoginJob(null, ip, null);
        assertEquals("https://128.1.1.0" + path, loginJob.getURLForPath(path));
        path += "/";
        assertEquals("https://128.1.1.0" + path, loginJob.getURLForPath(path));
        ip = "https://128.1.1.0";
        loginJob = new WebLoginJob(null, ip, null);
        assertEquals("https://128.1.1.0" + path, loginJob.getURLForPath(path));
    }

}
