package com.cynobit.capture_cam;

import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * (c) CynoBit 2018
 * Created by Francis on 12/2/2018.
 */
@SuppressWarnings("WeakerAccess")
public class CamServer {

    private int port = 0;
    private String adbPath;
    private ArrayList<Device> devices = new ArrayList<>();
    private static volatile CamServer server;
    private ServerSocket serverSocket;
    private ConnectivityListener connectivityListener;
    private Thread serverThread;

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

    private CamServer() {
        if (server != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static CamServer getInstance() {
        if (server == null) {
            synchronized (CamServer.class) {
                if (server == null) server = new CamServer();
            }
        }
        return server;
    }

    @SuppressWarnings("SameParameterValue")
    public void initialize(int port, String adbPath) throws FileNotFoundException, ServerPortException {
        if (this.port != 0 && this.port != port) throw new ServerPortException("A Port has been set already.");
        this.port = port;
        File file = new File(adbPath);
        if (!file.exists()) {
            throw new FileNotFoundException("Path to ADB (adb.exe or equivalent) does not exist.");
        }
        this.adbPath = adbPath;
    }

    public void setConnectivityListener(ConnectivityListener listener) {
        connectivityListener = listener;
    }

    public void begin() {
        serverThread = new Thread(new ServerRunnable());
        serverThread.start();
        new Timer(true).scheduleAtFixedRate(new ConnectivityWatchDog(), 5000, 15000);
    }

    public int queryDeviceCount() {
        devices.clear();
        int deviceCount = 0;
        try {
            Process process = Runtime.getRuntime().exec("cmd.exe /c " + adbPath + " devices -l");
            InputStream is = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("       ")) {
                    ++deviceCount;
                    String columns[] = line.split("\\s+");
                    Device device = new Device(columns[0],
                            columns[3].substring(columns[3].indexOf(":") + 1),
                            columns[2].substring(columns[2].indexOf(":") + 1));
                    devices.add(device);
                }
            }
            return deviceCount;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deviceCount;
    }

    public ArrayList<Device> getDevices() {
        return devices;
    }

    public class ServerPortException extends Exception {
        ServerPortException(String message) {
            super(message);
        }
    }

    private class ServerRunnable implements Runnable {

        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    socket = serverSocket.accept();
                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ConnectivityWatchDog extends TimerTask {

        private ArrayList<String> connectedIds = new ArrayList<>();

        @Override
        public void run() {
            System.out.println("Running Watch Dog.");
            for (Device device: devices) {
                if (device.getConnected()) {
                    if (!connectedIds.contains(device.getId())) {
                        connectedIds.add(device.getId());
                        connectivityListener.OnConnect(device);
                    }
                } else {
                    if (connectedIds.contains(device.getId())) {
                        connectedIds.remove(device.getId());
                        connectivityListener.OnDisconnect(device);
                    }
                }
                device.setConnected(false);
            }
        }
    }

    public interface ConnectivityListener {
        void OnDisconnect(Device device);

        void OnConnect(Device device);
    }

    private class CommunicationThread implements Runnable {

        private Socket clientSocket;
        private BufferedReader input;

        CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (input.ready()) {
                        String line = input.readLine();
                        System.out.println("Got: " + line);
                        try {
                            JSONObject json = new JSONObject(line);
                            if (json.getString(SPECTRA).equals(PING)) {
                                int deviceIndex = -1;
                                if (json.getInt(DEVICE_INDEX) == -1) {
                                    int x = 0;
                                    for (Device device : devices) {
                                        if (device.getId().equals(json.getString(DEVICE_ID))) {
                                            deviceIndex = x;
                                            break;
                                        }
                                        ++x;
                                    }
                                } else {
                                    deviceIndex = json.getInt(DEVICE_INDEX);
                                }
                                if (deviceIndex != -1) devices.get(deviceIndex).setConnected(true);
                                json = new JSONObject();
                                json.put(SPECTRA, "ping");
                                json.put(DEVICE_INDEX, deviceIndex);
                                json.put(DEVICE_ID, devices.get(deviceIndex).getId());
                                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                                out.write(json.toString() + "\n");
                                out.flush();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
