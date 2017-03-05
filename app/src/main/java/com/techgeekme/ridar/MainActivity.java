package com.techgeekme.ridar;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class MainActivity extends Activity {

    public final static String MAC_MESSAGE = "com.techgeekme.ridar.MESSAGE";

    public final static int REQUEST_BLUETOOTH = 1;

    private HashSet<DiscoveredDevice> discoveredDevices;
    /**
     * permissions request code
     */
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    /**
     * Permissions that need to be explicitly requested from end user.
     */
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};


    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final String TAG = "MainActivity";

    private BluetoothAdapter btAdapter = null;

    private boolean helmetConnected;

    ArrayAdapter<DiscoveredDevice> deviceArrayAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        discoveredDevices = new HashSet<>();
        checkPermissions();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        deviceArrayAdapter = new ArrayAdapter<>(this, R.layout.list_item_device, R.id.textview_device);
        ListView deviceListView = (ListView) findViewById(R.id.listview_devices);
        deviceListView.setAdapter(deviceArrayAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String address = deviceArrayAdapter.getItem(position).getAddress();
                Toast.makeText(MainActivity.this, address, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, NavActivity.class);
                intent.putExtra(MAC_MESSAGE, address);
                startActivity(intent);
                btAdapter.cancelDiscovery();
                finish();
            }
        });
    }


    public void initialize() {

    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the DiscoveredDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                // TODO: Add to list of devices discovered
                Log.i(TAG, "Device: " + deviceName + ",MAC: " + deviceHardwareAddress);
                DiscoveredDevice discoveredDevice = new DiscoveredDevice(device);
                if (!discoveredDevices.contains(discoveredDevice) && !deviceHardwareAddress.equals("00:21:13:00:1F:F1")) {
                    discoveredDevices.add(discoveredDevice);
                    deviceArrayAdapter.add(discoveredDevice);
                }
            }
        }
    };


    /**
     * Checks the dynamically-controlled permissions and requests missing permissions from end user.
     */
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted
                initialize();
                break;
        }
    }


    private void errorExit(String title, String message) {
        Log.d(TAG, title + " - " + message);
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //sendBluetoothSignal(0);

        unregisterReceiver(mReceiver);

        Log.d(TAG, "...In onDestroy()...");


    }

    private void ensureBluetooth() {
        if (btAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }


        if (!btAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, REQUEST_BLUETOOTH);
        } else {
            discoverDevices();
        }
    }

    private void discoverDevices() {
        btAdapter.startDiscovery();
            }

    @Override
    public void onResume() {
        super.onResume();

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        ensureBluetooth();

    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice remoteDevice) throws IOException {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                final Method m = remoteDevice.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
                return (BluetoothSocket) m.invoke(remoteDevice, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection", e);
                Toast.makeText(this, "Could not connect to helmet", Toast.LENGTH_SHORT).show();
            }
        }
        return remoteDevice.createRfcommSocketToServiceRecord(MY_UUID);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                discoverDevices();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }

}