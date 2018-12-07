package com.cynobit.capture_cam;

/**
 * (c) CynoBit 2018
 * Created by Francis on 12/3/2018.
 */
public class Device {

    private String model;
    private String id;
    private String productName;
    private boolean connected = false;

    Device(String id, String model, String productName) {
        this.id = id;
        this.model = model;
        this.productName = productName;
    }

    public String getModel() {
        return model;
    }

    public String getId() {
        return id;
    }

    boolean getConnected() {
        return connected;
    }

    void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getProductName() {
        return productName;
    }
}
