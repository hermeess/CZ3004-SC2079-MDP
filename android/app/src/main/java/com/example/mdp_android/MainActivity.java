package com.example.mdp_android;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.mdp_android.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    private static final int REQUEST_BLUETOOTH_CONNECT_PERMISSION = 1;
    private final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    };
    private final int REQUEST_ALL_PERMISSIONS = 1;
    private MapFragment mapFragment;
    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (!hasAllPermissions()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_ALL_PERMISSIONS);
        }
        settingsFragment = new SettingsFragment();
        mapFragment = new MapFragment();


        // Add both fragments if not added already
        if (!mapFragment.isAdded()) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.frame_layout, mapFragment, MapFragment.class.getName())
                    .commit();
        }


        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.map) {
                // Show the map fragment
                replaceFragment(mapFragment);
            } else if (itemId == R.id.settings) {
                // Navigate to the SettingsFragment activity
                Intent settingsIntent = new Intent(MainActivity.this, SettingsFragment.class);
                startActivity(settingsIntent);
            } else if (itemId == R.id.connect) {
                // Navigate to the Connect activity
                Intent connectIntent = new Intent(MainActivity.this, Connect.class);
                startActivity(connectIntent);
            }

            return true;
        });


    }
    private boolean hasAllPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment, MapFragment.class.getName());
        fragmentTransaction.commit();
    }


}


