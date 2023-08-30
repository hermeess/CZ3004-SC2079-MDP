package com.example.mdp_android;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.os.Build;

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
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        MainActivity mainActivity = (MainActivity) requireActivity();
        permissionCallback = mainActivity;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Button connectButton = rootView.findViewById(R.id.connectBtn);
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
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(requireActivity(),
                                    new String[]{
                                            android.Manifest.permission.BLUETOOTH,
                                            android.Manifest.permission.BLUETOOTH_ADMIN,
                                            android.Manifest.permission.BLUETOOTH_CONNECT
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
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(requireActivity(),
                                    new String[]{
                                            android.Manifest.permission.BLUETOOTH,
                                            android.Manifest.permission.BLUETOOTH_ADMIN
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
        UUID sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(sppUuid);
            bluetoothSocket.connect();
            connectedDevice = device;
            Log.d("Bluetooth", "Connected to " + device.getName());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Bluetooth", "Connection error: " + e.getMessage());
        }
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

