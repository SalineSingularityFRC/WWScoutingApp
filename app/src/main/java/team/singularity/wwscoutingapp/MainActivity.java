package team.singularity.wwscoutingapp;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import team.singularity.wwscoutingapp.singularitynet.Bluetooth;
import team.singularity.wwscoutingapp.singularitynet.Network;

public class MainActivity extends AppCompatActivity {

    Bluetooth bluetooth;
    Button newMatchButton;
    final String TAG = "Main Activity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.e(TAG, "Before Bluetooth");
        bluetooth = new Bluetooth(this);
        Log.e(TAG, "After Bluetooth");
        newMatchButton = findViewById(R.id.newMatchBtn);

        String[] names = bluetooth.getPairedDeviceNames();
        for (int i = 0; i < names.length; i++) {
            Log.i(TAG, "Device " + i +": " + names[i]);
        }

        newMatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }
}