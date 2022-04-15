package team.singularity.wwscoutingapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import team.singularity.wwscoutingapp.singularitynet.Bluetooth;
import team.singularity.wwscoutingapp.singularitynet.Network;

public class MainActivity extends AppCompatActivity {

    Network network;
    Button newMatchButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        network = new Network(this);
        newMatchButton = findViewById(R.id.newMatchBtn);

        newMatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }
}