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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SettingsFragment settingsFragment = new SettingsFragment();
        settingsFragment.setPermissionCallback(this);
        replaceFragment(new ControlsFragment());

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {

            int itemId = item.getItemId(); // Get the selected item's ID

            if (itemId == R.id.controls) {
                replaceFragment(new ControlsFragment());
            } else if (itemId == R.id.message) {
                replaceFragment(new MessageFragment());
            } else if (itemId == R.id.map) {
                replaceFragment(new MapFragment());
            } else if (itemId == R.id.settings) {
                replaceFragment(new SettingsFragment());
            }

            return true;
        });
    }

    private void replaceFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onBluetoothPermissionGranted() {
        // Handle the Bluetooth permission granted callback here
        // You can show a Toast or perform other actions
        Toast.makeText(this, "Bluetooth permission granted!", Toast.LENGTH_SHORT).show();
    }
}

