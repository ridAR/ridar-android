package com.techgeekme.ridar;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

/**
 * Created by anirudh on 21/02/17.
 */
public class DiscoveredDevice {

    private BluetoothDevice bluetoothDevice;

    public DiscoveredDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    @Override
    public String toString() {
        if (bluetoothDevice.getName().equals("HC-05")) {
            return "Smart Helmet";
        }
        return bluetoothDevice.getName();
    }

    @Override
    public boolean equals(Object o) {
        return bluetoothDevice.equals(((DiscoveredDevice)o).bluetoothDevice);
    }

    @Override
    public int hashCode() {
        return bluetoothDevice.hashCode();
    }

    public String getAddress() {
        return bluetoothDevice.getAddress();
    }
}
