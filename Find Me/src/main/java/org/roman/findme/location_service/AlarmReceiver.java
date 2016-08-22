package org.roman.findme.location_service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    public AlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(Constants.TAG, "AlarmReceiver onReceive");
        Intent runService = new Intent(context, LocationService.class);
        context.startService(runService);
    }
}
