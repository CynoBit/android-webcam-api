package com.cynobit.capture_cam;

/**
 * (c) CynoBit 2018
 * Created by Francis on 12/2/2018.
 */
public class CamServer {

    private int port;
    private String adbPath;

    public CamServer (int port, String adbPath) {
        this.port = port;
        this.adbPath = adbPath;
    }

}
