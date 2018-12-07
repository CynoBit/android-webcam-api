package com.cynobit.capture_cam;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * (c) CynoBit 2018
 * Created by Francis on 12/3/2018.
 */
public class CamServerTest {

    private CamServer camServer;

    /*
    Protocol Keys and VALUES.
     */
    private final static String SPECTRA = "spectra";
    private final static String PING = "ping";
    private final static String DEVICE_ID = "device_id";
    private final static String DEVICE_INDEX = "device_index";
    private final static String DATA = "data";
    private final static String DATA_TYPE = "private final static String";
    private final static String ECHO = "echo";

    @Before
    public void setUp() throws Exception {
        camServer = CamServer.getInstance();
        camServer.initialize(9000, "C:\\Users\\Francis\\Desktop\\platform-tools\\adb.exe");
    }

    @Test
    public void testAdbGetDevices() {
        int deviceCount = camServer.queryDeviceCount();
        System.out.println("Devices: " + deviceCount);
        ArrayList<Device> devices = camServer.getDevices();
        assertEquals(deviceCount, devices.size());
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    @Test
    public void testServerConnectivity() {
        MockConnectivityListener listener = new MockConnectivityListener();
        camServer.setConnectivityListener(listener);
        camServer.begin();
        Device mockClientDevice;
        ArrayList<Device> devices = camServer.getDevices();
        if (devices.size() > 0) {
            try {
                mockClientDevice = devices.get(0);
                Socket mockDevice = new Socket("localhost", 9000);
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(mockDevice.getOutputStream()));
                JSONObject object = new JSONObject();
                object.put(SPECTRA, PING);
                object.put(DEVICE_ID, mockClientDevice.getId());
                object.put(DEVICE_INDEX, -1);
                out.write(object.toString() + "\n");
                out.flush();
                BufferedReader response = new BufferedReader(new InputStreamReader(mockDevice.getInputStream()));
                JSONObject reply = new JSONObject(response.readLine());
                assertEquals(0, reply.getInt(DEVICE_INDEX));
                assertEquals(mockClientDevice.getId(), reply.getString(DEVICE_ID));
                synchronized (listener) {
                    listener.wait(20000);
                }
                assertTrue(listener.getConnectivity());
                out.write(object.toString() + "\n");
                out.flush();
                synchronized (listener) {
                    listener.wait(20000);
                }
                assertTrue(listener.getConnectivity());
                synchronized (listener) {
                    listener.wait(20000);
                }
                assertFalse(listener.getConnectivity());
            } catch (IOException | JSONException | InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Test continuing but you need at least a device connected for a complete test");
        }
    }

    private class MockConnectivityListener implements CamServer.ConnectivityListener {

        boolean connectivity = false;

        @Override
        public void OnDisconnect(Device device) {
            connectivity = device.getConnected();
            System.out.println("Triggered DisConnected Status.");
            synchronized (this) {
                notifyAll();
            }
        }

        @Override
        public void OnConnect(Device device) {
            connectivity = device.getConnected();
            System.out.println("Triggered Connected Status.");
            synchronized (this) {
                notifyAll();
            }
        }

        boolean getConnectivity() {
            return connectivity;
        }
    }

}