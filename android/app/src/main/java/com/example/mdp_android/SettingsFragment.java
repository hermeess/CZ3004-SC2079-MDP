package com.example.mdp_android;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.os.Build;
import android.content.BroadcastReceiver;
import android.widget.AdapterView;
import android.content.IntentFilter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.os.ParcelUuid;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;


public class SettingsFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice connectedDevice;
    private BluetoothSocket bluetoothSocket;
    private PermissionCallback permissionCallback;

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
    public interface PermissionCallback {
        void onBluetoothPermissionGranted();
    }

    public void setPermissionCallback(PermissionCallback callback) {
        this.permissionCallback = callback;
    }
    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance(String param1, String param2) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    private ArrayList<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private View rootView;
    public void openDeviceListDialog() {
        showDeviceListDialog();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    private static final int REQUEST_ENABLE_BT = 1;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("SettingsFragment", "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        ListView deviceListView = rootView.findViewById(R.id.deviceListView);
        ListView listView = view.findViewById(R.id.deviceListView);
        MainActivity mainActivity = (MainActivity) requireActivity();
        permissionCallback = mainActivity;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        requireActivity().registerReceiver(mReceiver, filter);

        IntentFilter bondStateFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        requireActivity().registerReceiver(mBondStateReceiver, bondStateFilter);

        Button connectButton = rootView.findViewById(R.id.connectBtn);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // Cancel discovery because it's costly and we're about to connect
                bluetoothAdapter.cancelDiscovery();

                // Get the device MAC address, which is the last 17 chars in the View
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);

                Log.d("BondingProcess", "MAC address: " + address);

                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

                // Check and log the bond state before attempting to create a bond
                Log.d("BondingProcess", "Current bond state: " + device.getBondState());

                // Initiate bonding
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    boolean bondInitiated = device.createBond();
                    Log.d("BondingProcess", "Bond initiated: " + bondInitiated);
                }

                // Now proceed to connect to the device
                // Your existing connection code here
            }

        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Button Click", "Connect to robot button clicked");

                if (!bluetoothAdapter.isEnabled()) {
                    // Bluetooth is not enabled, prompt the user to enable it
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // For Android 12 and above
                        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH)
                                != PackageManager.PERMISSION_GRANTED
                                || ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_ADMIN)
                                != PackageManager.PERMISSION_GRANTED
                                || ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT)
                                != PackageManager.PERMISSION_GRANTED
                                || ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(requireActivity(),
                                    new String[]{
                                            android.Manifest.permission.BLUETOOTH,
                                            android.Manifest.permission.BLUETOOTH_ADMIN,
                                            android.Manifest.permission.BLUETOOTH_CONNECT,
                                            android.Manifest.permission.ACCESS_FINE_LOCATION
                                    },
                                    REQUEST_BLUETOOTH_PERMISSIONS);
                        } else {
                            // Permission already granted, proceed
                            if (permissionCallback != null) {
                                onBluetoothPermissionGranted();
                            }
                        }
                    } else {
                        // For Android 11 and below
                        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH)
                                != PackageManager.PERMISSION_GRANTED
                                || ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_ADMIN)
                                != PackageManager.PERMISSION_GRANTED
                                || ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(requireActivity(),
                                    new String[]{
                                            android.Manifest.permission.BLUETOOTH,
                                            android.Manifest.permission.BLUETOOTH_ADMIN,
                                            android.Manifest.permission.ACCESS_FINE_LOCATION
                                    },
                                    REQUEST_BLUETOOTH_PERMISSIONS);
                        } else {
                            // Permission already granted, proceed
                            if (permissionCallback != null) {
                                onBluetoothPermissionGranted();
                            }
                        }
                    }



                }
            }
        });
        Button scanButton = rootView.findViewById(R.id.scanBtn);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start Bluetooth device discovery
                startDeviceDiscovery();
            }
        });
        Button disconnectButton = rootView.findViewById(R.id.disconnectBtn);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Button Click", "Disconnect from robot button clicked");
                disconnectFromDevice();
            }
        });

        return rootView;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("SettingsFragment", "onViewCreated: rootView is not null: " + (rootView != null));

        // Initialize your views here
        ListView deviceListView = view.findViewById(R.id.deviceListView);

        Log.d("SettingsFragment", "onViewCreated: deviceListView is not null: " + (deviceListView != null));

        // Call the updateDeviceListUI method when the view is created
        updateDeviceListUI();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireActivity().unregisterReceiver(mReceiver);
    }

    private void startDeviceDiscovery() {
        Log.d("BluetoothDiscovery", "startDeviceDiscovery called");
        // Check if Bluetooth is already discovering or not enabled
        if (!bluetoothAdapter.isEnabled()) {
            // Bluetooth is not enabled, prompt the user to enable it
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            // Clear the existing discovered devices
            discoveredDevices.clear();

            // Check for Bluetooth and Location permissions
            boolean bluetoothPermission = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH)
                    == PackageManager.PERMISSION_GRANTED;
            boolean bluetoothAdminPermission = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_ADMIN)
                    == PackageManager.PERMISSION_GRANTED;
            boolean locationPermission = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
            boolean bluetoothScanPermission = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // For Android 12 and above
                boolean bluetoothConnectPermission = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED;

                if (bluetoothPermission && bluetoothAdminPermission && bluetoothConnectPermission && bluetoothScanPermission && locationPermission) {
                    // All permissions granted, proceed with discovery
                    Log.d("BluetoothDiscovery", "startDeviceDiscovery proceeded");
                    bluetoothAdapter.startDiscovery();
                } else {
                    Log.d("BluetoothDiscovery", "startDeviceDiscovery requesting perms");
                    // Request permissions
                    ActivityCompat.requestPermissions(requireActivity(),
                            new String[]{
                                    android.Manifest.permission.BLUETOOTH,
                                    android.Manifest.permission.BLUETOOTH_ADMIN,
                                    android.Manifest.permission.BLUETOOTH_CONNECT,
                                    android.Manifest.permission.BLUETOOTH_SCAN,
                                    android.Manifest.permission.ACCESS_FINE_LOCATION
                            },
                            REQUEST_BLUETOOTH_PERMISSIONS);
                    bluetoothAdapter.startDiscovery();
                }
            } else {
                // For Android 11 and below
                if (bluetoothPermission && bluetoothAdminPermission && bluetoothScanPermission && locationPermission) {
                    // All permissions granted, proceed with discovery
                    bluetoothAdapter.startDiscovery();
                } else {
                    // Request permissions
                    ActivityCompat.requestPermissions(requireActivity(),
                            new String[]{
                                    android.Manifest.permission.BLUETOOTH,
                                    android.Manifest.permission.BLUETOOTH_ADMIN,
                                    android.Manifest.permission.BLUETOOTH_SCAN,
                                    android.Manifest.permission.ACCESS_FINE_LOCATION
                            },
                            REQUEST_BLUETOOTH_PERMISSIONS);
                    bluetoothAdapter.startDiscovery();
                }
            }
        }
    }

    private final BroadcastReceiver mBondStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                int previousBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                Log.d("BondingProcess", "Bond state changed from " + previousBondState + " to " + bondState);
            }
        }
    };

    // Register this receiver in your onCreateView or similar


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address

                // Add device to a list, but don't initiate bonding here
                discoveredDevices.add(device);
                updateDeviceListUI();
                // You can update your ListView here to display the new device
            }
        }
    };



    private void updateDeviceListUI() {
        // Assuming you have a ListView with the id "deviceListView" in your XML layout
        ListView deviceListView = null;
        if (rootView != null) {
            deviceListView = rootView.findViewById(R.id.deviceListView);
            // Continue with your code...
        } else {
            // Handle the case when rootView is null (e.g., log an error).
            Log.e("SettingsFragment", "rootView is null. Unable to find deviceListView.");
        }


        // Create an ArrayAdapter to bind discoveredDevices to the ListView
        ArrayAdapter<BluetoothDevice> adapter = new ArrayAdapter<BluetoothDevice>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                discoveredDevices
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                BluetoothDevice device = getItem(position);
                TextView textView = view.findViewById(android.R.id.text1);
                if (device != null) {
                    String deviceName = device.getName();
                    if (deviceName != null && !deviceName.isEmpty()) {
                        textView.setText(deviceName);
                    } else {
                        // If device name is not available, display the MAC address
                        textView.setText(device.getAddress());
                    }
                }
                return view;
            }
        };



        // Set the adapter to the ListView
        deviceListView.setAdapter(adapter);

        // Set an OnItemClickListener to handle user selections
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected BluetoothDevice
                BluetoothDevice selectedDevice = discoveredDevices.get(position);

                // Connect to the selected device or perform other actions here
                connectToDevice(selectedDevice);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                // All permissions granted, proceed
                if (permissionCallback != null) {
                    onBluetoothPermissionGranted();
                }
            } else {
                // Some permissions denied, handle accordingly
                showToast("Bluetooth permissions denied. Cannot proceed.");
            }
        }
    }


    public void onBluetoothPermissionGranted() {
        Fragment fragment = getParentFragmentManager().findFragmentById(R.id.frame_layout);
        if (fragment instanceof SettingsFragment) {
            ((SettingsFragment) fragment).openDeviceListDialog();
        }
    }

    private AlertDialog dialog; // Declare the dialog as a member variable

    private void showDeviceListDialog() {
        Log.d("SettingsFragment", "showDeviceListDialog: Displaying device list dialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.device_list_dialog, null);
        ListView deviceListView = dialogView.findViewById(R.id.deviceListView);

        ArrayList<BluetoothDevice> pairedDevices = new ArrayList<>(bluetoothAdapter.getBondedDevices());
        Log.d("SettingsFragment", "Paired devices count: " + pairedDevices.size());
        DeviceListAdapter adapter = new DeviceListAdapter(requireContext(), pairedDevices);
        deviceListView.setAdapter(adapter);

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice selectedDevice = pairedDevices.get(position);
            connectToDevice(selectedDevice);
            dialog.dismiss(); // Now you can dismiss the dialog
        });

        builder.setView(dialogView)
                .setTitle("Select a Device")
                .setNegativeButton("Cancel", null);

        dialog = builder.create(); // Assign the created dialog to the member variable
        dialog.show();
    }

    private void connectToDevice(BluetoothDevice device) {
        new Thread(() -> {
            try {
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    device.createBond();
                    // You might want to add a delay here or listen for a bonding event to ensure the bond is complete before continuing
                    Thread.sleep(2000);
                }

                ParcelUuid[] uuids = device.getUuids();
                if (uuids != null) {
                    for (ParcelUuid uuid : uuids) {
                        try {
                            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid.getUuid());
                            bluetoothSocket.connect();
                            connectedDevice = device;
                            Log.d("Bluetooth", "Connected to " + device.getName() + " using UUID " + uuid.toString());
                            return; // Exit thread if connection is successful
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e("Bluetooth", "Connection error using UUID " + uuid.toString() + ": " + e.getMessage());
                        }
                    }
                } else {
                    // Fallback to hardcoded UUID if device.getUuids() returns null
                    UUID sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(sppUuid);
                    bluetoothSocket.connect();
                    connectedDevice = device;
                    Log.d("Bluetooth", "Connected to " + device.getName());
                }
            } catch (Exception e1) {
                e1.printStackTrace();
                Log.e("Bluetooth", "Connection error: " + e1.getMessage());
                // Fallback method for connection
                try {
                    bluetoothSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
                    bluetoothSocket.connect();
                    connectedDevice = device;
                    Log.d("Bluetooth", "Connected to " + device.getName() + " using fallback method.");
                } catch (Exception e2) {
                    e2.printStackTrace();
                    Log.e("Bluetooth", "Fallback connection failed: " + e2.getMessage());
                }
            }
        }).start();







    // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread
        manageMyConnectedSocket(bluetoothSocket);
    }
    private void manageMyConnectedSocket(BluetoothSocket socket) {
        // TODO: Perform work associated with the connection in a separate thread
    }


    private void disconnectFromDevice() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
                Log.d("Bluetooth", "Disconnected from " + connectedDevice.getName());
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("Bluetooth", "Disconnection error: " + e.getMessage());
            }
        }
    }

    // DeviceListAdapter class
    private class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

        public DeviceListAdapter(Context context, List<BluetoothDevice> devices) {
            super(context, 0, devices);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            BluetoothDevice device = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            }

            TextView deviceNameTextView = convertView.findViewById(android.R.id.text1);

            if (device != null) {
                deviceNameTextView.setText(device.getName());
            } else {
                // Display a message when no devices are found
                deviceNameTextView.setText("No devices found");
            }

            return convertView;
        }
    }

}

