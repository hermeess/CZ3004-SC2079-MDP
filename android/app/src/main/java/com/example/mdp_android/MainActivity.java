package com.example.mdp_android;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.widget.Toast;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.example.mdp_android.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements SettingsFragment.PermissionCallback {

    ActivityMainBinding binding;
    private static final int REQUEST_BLUETOOTH_CONNECT_PERMISSION = 1;

    private MapFragment mapFragment;
    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        settingsFragment = new SettingsFragment();
        mapFragment = new MapFragment();
        settingsFragment.setPermissionCallback(this);

        // Add both fragments if not added already
        if (!mapFragment.isAdded()) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.frame_layout, mapFragment, MapFragment.class.getName())
                    .commit();
        }
        if (!settingsFragment.isAdded()) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.frame_layout, settingsFragment, SettingsFragment.class.getName())
                    .hide(settingsFragment)
                    .commit();
        }

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Show/hide fragments based on tab selection
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (itemId == R.id.map) {
                transaction.hide(settingsFragment);
                transaction.show(mapFragment);
            } else if (itemId == R.id.settings) {
                transaction.hide(mapFragment);
                transaction.show(settingsFragment);
            }
            transaction.commit();

            return true;
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment, MapFragment.class.getName());
        fragmentTransaction.commit();
    }

    @Override
    public void onBluetoothPermissionGranted() {
        // Handle the Bluetooth permission granted callback here
        // You can show a Toast or perform other actions
        Toast.makeText(this, "Bluetooth permission granted!", Toast.LENGTH_SHORT).show();
    }
}


