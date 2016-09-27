package org.roman.findme.location_service;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.roman.findme.db.DBHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.roman.findme.RegistrationIntentService.makeResponse;
import static org.roman.findme.location_service.Constants.TAG;
import static org.roman.findme.location_service.Constants.dbTableName;

public class GPSTracker extends Service implements LocationListener {
    private int ACTION_DELETE = 1;
    private int ACTION_INSERT = 2;
    private int ACTION_SELECT = 3;

    ArrayList<String[]> coordinates = new ArrayList<>();

    private final Context mContext;

    // flag for GPS status
    boolean isGPSEnabled = false;

    // flag for network status
    boolean isNetworkEnabled = false;

    // flag for GPS status
    boolean canGetLocation = false;

    Location location; // location
    double latitude; // latitude
    double longitude; // longitude

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 0; // 1 minute

    // Declaring a Location Manager
    protected LocationManager locationManager;

    public GPSTracker(Context context) {
        this.mContext = context;
        getLocation();
    }

    public Location getLocation() {
        try {
            locationManager = (LocationManager) mContext
                    .getSystemService(LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                this.canGetLocation = true;
                // First get location from Network Provider
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d("Network", "Network");
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        Log.d("GPS Enabled", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }
            }

        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return location;
    }


    public void stopUsingGPS() {
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(GPSTracker.this);
        }
    }


    public double getLatitude(){
        if(location != null){
            latitude = location.getLatitude();
        }

        // return latitude
        return latitude;
    }


    public double getLongitude(){
        if(location != null){
            longitude = location.getLongitude();
        }

        // return longitude
        return longitude;
    }


    public boolean canGetLocation() {
        return this.canGetLocation;
    }


    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
                dialog.cancel();
            }
        });


        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    public void sendMyLocation(String longitude, String latitude){

        Log.v(TAG, "sendMyLocation  " + longitude + latitude);

        String uid = Build.SERIAL;

        if (isConnectingToInternet(getApplicationContext())) {
            String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

            nameValuePairs.add(new BasicNameValuePair("action", "coordinates"));
            nameValuePairs.add(new BasicNameValuePair("latitude", latitude));
            nameValuePairs.add(new BasicNameValuePair("longitude", longitude));
            nameValuePairs.add(new BasicNameValuePair("time", currentDate));
            nameValuePairs.add(new BasicNameValuePair("uid", uid));

            makeResponse(nameValuePairs);

            db_action(ACTION_SELECT, null, null);

            if(!(coordinates.size()==0)){
                for(String [] line: coordinates){
                    nameValuePairs.clear();

                    nameValuePairs.add(new BasicNameValuePair("action", "coordinates"));
                    nameValuePairs.add(new BasicNameValuePair("latitude", line[0]));
                    nameValuePairs.add(new BasicNameValuePair("longitude", line[1]));
                    nameValuePairs.add(new BasicNameValuePair("time", line[2]));
                    nameValuePairs.add(new BasicNameValuePair("uid", uid));

                    makeResponse(nameValuePairs);
                }

                db_action(ACTION_DELETE, null, null);
                coordinates.clear();
            }

        }else{
            db_action(ACTION_INSERT, latitude, longitude);
        }
    }

    public void db_action(int action, String latitude, String longitude){

         /*-------------for DB*/

        DBHelper dbHelper;
        dbHelper = new DBHelper(this);
        ContentValues cv = new ContentValues();
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        if(action==ACTION_INSERT){

            Log.d(TAG, "--- Insert in "+dbTableName+": ---");
            // подготовим данные для вставки в виде пар: наименование столбца - значение

            cv.put("latitude", String.valueOf(latitude));
            cv.put("longitude", String.valueOf(longitude));
            cv.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            // вставляем запись и получаем ее ID
            long rowID = db.insert(dbTableName, null, cv);
            Log.d(TAG, "row inserted, ID = " + rowID);


        }else if(action==ACTION_SELECT){
            Log.d(TAG, "--- Rows in "+dbTableName+": ---");
            // делаем запрос всех данных из таблицы, получаем Cursor
            Cursor c = db.query(dbTableName, null, null, null, null, null, null);

            // ставим позицию курсора на первую строку выборки
            // если в выборке нет строк, вернется false
            if (c.moveToFirst()) {

                // определяем номера столбцов по имени в выборке
                int idColIndex = c.getColumnIndex("id");
                int latitudeColIndex = c.getColumnIndex("latitude");
                int longitudeColIndex = c.getColumnIndex("longitude");
                int timeColIndex = c.getColumnIndex("time");

                do {
                    // получаем значения по номерам столбцов и пишем все в лог
                    Log.d(TAG,
                            "ID = " + c.getInt(idColIndex) +
                                    ", latitude = " + c.getString(latitudeColIndex) +
                                    ", longitude = " + c.getString(longitudeColIndex) +
                                    ", time = " + c.getString(timeColIndex));
                    // переход на следующую строку
                    // а если следующей нет (текущая - последняя), то false - выходим из цикла
                    coordinates.add(new String[] {c.getString(latitudeColIndex), c.getString(longitudeColIndex), c.getString(timeColIndex)});
                } while (c.moveToNext());
            } else
                Log.d(TAG, "0 rows");
            c.close();
        }else if(action==ACTION_DELETE){
            Log.d(TAG, "--- Clear "+dbTableName+": ---");
            // удаляем все записи
            int clearCount = db.delete(dbTableName, null, null);
            Log.d(TAG, "deleted rows count = " + clearCount);
        }

        dbHelper.close();
    }

    public boolean isConnectingToInternet(Context _context) {
        ConnectivityManager connectivity = (ConnectivityManager) _context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }

        }
        return false;
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}