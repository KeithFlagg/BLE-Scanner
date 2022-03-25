package com.example.bluetoothapplication;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class HomeActivity extends AppCompatActivity {
    private ListView listVw;
    // pass our array to this obj so it can be shown on the ui
    private ArrayAdapter aAdapter;
    // Array list of class Device to store and sort devices
    private ArrayList<Device> deviceInfo = new ArrayList<>();
    // bluetooth adapter obj stored
    private BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
    // bt low energy scanner
    private BluetoothLeScanner scanner = bAdapter.getBluetoothLeScanner();
    // stores actual bluetooth device obj
    private ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    private boolean scanning;
    private boolean connecting = false;
    private boolean sort = false;
    // handles timeout
    private Handler handler = new Handler();
    // Stops scanning after 30 seconds. Saves battery
    private static final long SCAN_PERIOD = 15000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        // grab elements from template
        Button startBtn = (Button) findViewById(R.id.btnGet);
        Button sortBtn = (Button) findViewById(R.id.btnSort);
        ListView listview = (ListView) findViewById(R.id.deviceList);

        // check permissions
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "insufficient permissions", Toast.LENGTH_SHORT).show();
            return;
        }
        // check if device has a bt adapter
        if (bAdapter == null) {
            Toast.makeText(getApplicationContext(), "bluetooth unavailable", Toast.LENGTH_SHORT).show();
        } else {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "bluetooth unavailable", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!bAdapter.isEnabled()) {
                Toast.makeText(getApplicationContext(), "bluetooth is not enabled", Toast.LENGTH_SHORT).show();
            }
        }
        // onclick listener for device list view
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!connecting) {
                    connecting = true;
                    BluetoothDevice device = bluetoothDevices.get(0);
                    for (int i = 0; i < bluetoothDevices.size(); i++) {
                        if (deviceInfo.get(position).macAddress.equals(bluetoothDevices.get(i).getAddress())) {
                            device = bluetoothDevices.get(i);
                        }
                    }

                    Log.i("SELECTED", "*** SELECTED DEVICE: "+device.getAddress());
                    // hard coded uuid
                    UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
                    if (bAdapter.isDiscovering()) {
                        bAdapter.cancelDiscovery();
                    }
                    // init BluetoothSocket
                    BluetoothSocket bsocket = null;
                    try {
                        // creates RFCOMM socket (Radio Frequency Communication)
                        device.createBond();
                        bsocket = device.createRfcommSocketToServiceRecord(SERIAL_UUID);
                        if (!bsocket.isConnected()) {
                            bsocket.connect();
                        }
                        // shows user they are connected
                        Toast.makeText(getApplicationContext(), "CONNECTED TO: "+device.getName()+", Address: "+device.getAddress(), Toast.LENGTH_LONG).show();
                        connecting = false;
                    } catch (IOException e) {
                        Log.e("SOCKET", "ERROR: "+e);
                        Toast.makeText(getApplicationContext(), "Unable to connect to socket!", Toast.LENGTH_LONG).show();
                        connecting = false;
                        try {
                            bsocket.close();
                        } catch (IOException ioException) {
                            Toast.makeText(getApplicationContext(), "Unable to close socket!", Toast.LENGTH_LONG).show();
                            ioException.printStackTrace();
                        }
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Cannot connect to a new device while pairing!", Toast.LENGTH_LONG).show();
                }
            }
        });
        // onclick listener for scan button
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bAdapter == null) {
                    Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(getApplicationContext(), "Please enable bluetooth permission in settings", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (bAdapter.isDiscovering()) {
                        bAdapter.cancelDiscovery();
                    }
                    bAdapter.startDiscovery();
                    // store devices
                    ArrayList deviceList = new ArrayList();
                    // store the text for ui
                    ArrayList textList = new ArrayList();
                    // set device info
                    deviceInfo = new ArrayList<>();
                    // reset stored devices
                    bluetoothDevices.clear();
                    // get version
                    int apiVersion = android.os.Build.VERSION.SDK_INT;

                    // assign ScanCallback to a var so it can be controlled by startScan() and stopScan()
                    ScanCallback bleScanCallBack = new ScanCallback() {
                        @Override
                        public void onScanResult(int callbackType, ScanResult result) {
                            // get the discovered device
                            // this will trigger each time a new device is found
                            BluetoothDevice device = result.getDevice();
                            String deviceName = device.getName()!=null ? device.getName() : "N/A";
                            String macAddress = device.getAddress();

                            Log.i("SCANNER", "*** deviceName: " + deviceName + " macAddress: " + macAddress + "RSSI: " + result.getRssi() + "***");
                            if (!deviceList.contains(device.getAddress())) {
                                // if not found append device to list
                                deviceList.add(device.getAddress());
                                bluetoothDevices.add(device);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    // ensures device can utilize isConnectable()
                                    Device deviceObj = new Device(device.getName(), device.getAddress(), result.isConnectable(), result.getRssi());
                                    deviceInfo.add(deviceObj);
                                    if (sort) {
                                        // if sort enabled, sort list as new devices are added
                                        Collections.sort(deviceInfo, new Comparator<Device>() {
                                            @Override
                                            public int compare(Device a1, Device a2) {
                                                return a2.rssi - a1.rssi;
                                            }
                                        });
                                        textList.clear();
                                        for (int k = 0; k < deviceInfo.size(); k++) {
                                            textList.add("Name: "+deviceInfo.get(k).name+", MAC Address: "+deviceInfo.get(k).macAddress+", RSSI: "+deviceInfo.get(k).rssi+ ", Connectable: " + deviceInfo.get(k).connectable);
                                        }
                                    } else {
                                        // don't sort
                                        textList.add("Name: "+deviceName+", MAC Address: "+macAddress+", RSSI: "+result.getRssi()+ ", Connectable: " + result.isConnectable());
                                    }
                                } else {
                                    // isConnectable() is not available
                                    Device deviceObj = new Device(device.getName(), device.getAddress(), null, result.getRssi());
                                    deviceInfo.add(deviceObj);
                                    if (sort) {
                                        // if sort enabled, sort list as new devices are added
                                        Collections.sort(deviceInfo, new Comparator<Device>() {
                                            @Override
                                            public int compare(Device a1, Device a2) {
                                                return a2.rssi - a1.rssi;
                                            }
                                        });
                                        textList.clear();
                                        for (int k = 0; k < deviceInfo.size(); k++) {
                                            textList.add("Name: "+deviceInfo.get(k).name+", MAC Address: "+deviceInfo.get(k).macAddress+", RSSI: "+deviceInfo.get(k).rssi+ ", Connectable: " + deviceInfo.get(k).connectable);
                                        }
                                    } else {
                                        // don't sort
                                        textList.add("Name: "+deviceName+", MAC Address: "+macAddress+", RSSI: "+result.getRssi());
                                    }
                                }
                                // update the listview with our device info
                                listVw = (ListView) findViewById(R.id.deviceList);
                                aAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, textList);
                                listVw.setAdapter(aAdapter);
                            } else {
                                // if found then update the rssi shown. (rssi continuous update)
                                // find the index and update
                                 int index = deviceList.lastIndexOf(device.getAddress());
                                 Log.i("INDEX", "INDEX: "+index);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    textList.set(index, "Name: " + deviceName + ", MAC Address: " + macAddress + ", RSSI: " + result.getRssi() + ", Connectable: " + result.isConnectable());
                                } else {
                                    textList.add("Name: "+deviceName+", MAC Address: "+macAddress+", RSSI: "+result.getRssi());
                                }
                            }
                        }
                    };

                    if (apiVersion > android.os.Build.VERSION_CODES.KITKAT) {
                        if (!scanning) {
                            // Stops scanning after a predefined scan period.
                            // calls run after scan period is over async function
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    scanning = false;
                                    scanner.stopScan(bleScanCallBack);
                                    Toast.makeText(getApplicationContext(), "Scan Finished (DURATION: "+SCAN_PERIOD/1000+" SECONDS)", Toast.LENGTH_LONG).show();
                                }
                            }, SCAN_PERIOD);
                            scanning = true;
                            scanner.startScan(bleScanCallBack);
                        } else {
                            scanning = false;
                            scanner.stopScan(bleScanCallBack);
                        }
                    } else {
                        // targets 5.0 or below unable to get connectable
                        bAdapter.startLeScan(new BluetoothAdapter.LeScanCallback() {
                            @Override
                            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                                // get the discovered device
                                String deviceName = device.getName();
                                String macAddress = device.getAddress();
                                Log.i("SCANNER", "*** deviceName: " + deviceName + " macAddress: " + macAddress + "RSSI: " + rssi + "***");
                                Toast.makeText(getApplicationContext(), "Please update android version >= 5.0", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }
        });

        sortBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // toggle sort
                sort = !sort;
                if (sort) {
                    Toast.makeText(getApplicationContext(), "Sort Enabled", Toast.LENGTH_SHORT).show();
                    // create sort operator
                    Collections.sort(deviceInfo, new Comparator<Device>() {
                        // sort rssi by desc
                        @Override
                        public int compare(Device a1, Device a2) {
                            return a2.rssi - a1.rssi;
                        }
                    });
                    // store the text for ui
                    ArrayList textList = new ArrayList();
                    listVw = (ListView) findViewById(R.id.deviceList);
                    for (int i = 0; i < deviceInfo.size(); i++) {
                        textList.add("Name: "+deviceInfo.get(i).name+", MAC Address: "+deviceInfo.get(i).macAddress+", RSSI: "+deviceInfo.get(i).rssi+ ", Connectable: "+deviceInfo.get(i).connectable);
                    }
                    aAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, textList);
                    listVw.setAdapter(aAdapter);
                } else {
                    Toast.makeText(getApplicationContext(), "Sort Disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
// helper class to make sorting list easy
class Device {
    String name;
    String macAddress;
    Boolean connectable;
    int rssi;
    public Device(String name, String macAddress, Boolean connectable, int rssi) {
        super();
        this.name = name;
        this.macAddress = macAddress;
        this.connectable = connectable;
        this.rssi = rssi;
    }
}
