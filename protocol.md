# API Standard Behaviour #

The API (Meant to run on the system that makes use of the images) starts a server that listens on the given port.

If the adb port forwarding is successful, the server should get a message such as below periodically from a connected device.

~~~json
{
    "spectra": "ping",
    "device_id": "%device_id%",
    "device_index": "-1 or n"
}
~~~

The Server/API will respond each time with:

~~~json
{
    "spectra": "ping",
    "device_id": "%device_id",
    "device_index": "-1 or n" // Note {Data type here is int} the index of the device in the array of connected devices in the API/Server end.
}
~~~

The above communication is a means to signal the API that an android device running the CamCapture app is still connected.

When a Capture is taken from the device and sent, the following JSON packet will be sent to the API/Server

~~~json
{
    "spectra": "capture",
    "device_id": "%device_id%",
    "device_index": "-1 or n",
    "dataType": "image",
    "data": "base_64 string"
}
~~~

Yes, The image is transmitted as a base 64 string.

The API/Server responds with the below:

~~~json
{
    "spectra": "capture",
    "device_id": "%device_id%",
    "device_index": "-1 or n",
    "dataType": "echo",
    "data": "1 or 0" // Note {Data type is int.} 1 - Successful image reception, 0 - not successful.
}
~~~