package team.singularity.wwscoutingapp.singularitynet;

import android.app.Activity;

public class Network {

    Bluetooth bluetooth;

    public Network(Activity activity) {
        bluetooth = new Bluetooth(activity);


    }
}
