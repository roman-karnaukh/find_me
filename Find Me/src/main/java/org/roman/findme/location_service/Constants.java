package org.roman.findme.location_service;

import android.util.Log;

/**
 * Created by Karnaukh Roman on 22.08.2016.
 */
public class Constants {
    public static final String TRACK_URL = "http://task-master.zzz.com.ua/server.php";
    public static final String TAG = "FIND_ME";
    public static final String dbTableName = "notSendetCoordinates";

    public static void logger(String info){
        Log.d(TAG, info);
    }
}
