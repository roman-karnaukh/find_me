package org.roman.findme.location_service;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import org.roman.findme.RegistrationIntentService;
import org.roman.findme.db.DBHelper;

import java.util.ArrayList;
import java.util.List;

import static org.roman.findme.RegistrationIntentService.makeResponse;
import static org.roman.findme.location_service.Constants.TAG;
import static org.roman.findme.location_service.Constants.dbTableName;
import static org.roman.findme.location_service.Constants.logger;

public class LocationService extends Service {

    private int ACTION_DELETE = 1;
    private int ACTION_INSERT = 2;
    private int ACTION_SELECT = 3;

    private LocationManager locationManager = null;
    private LocationListener locationListenerNetwork = null;
    private LocationListener locationListenerGPS = null;
    private find_me myCoordinatesTask;
    public static String myToken = null;

    ArrayList<String[]> coordinates = new ArrayList<>();

    SharedPreferences sharedPreferences;

    PowerManager.WakeLock wakeLock;
    Context mContext = this;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();



        sharedPreferences = getSharedPreferences(RegistrationIntentService.APP_PREFERENCES, Context.MODE_PRIVATE);
        myToken = sharedPreferences.getString("my_token", null);

        PowerManager pm = (PowerManager) getSystemService(this.POWER_SERVICE);

        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DoNotSleep");

        Toast.makeText(getApplicationContext(), "Service Created",
                Toast.LENGTH_SHORT).show();

        Log.e(TAG, "Service Created");

    }

    @Override
    @Deprecated
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        myCoordinatesTask = new find_me();
        myCoordinatesTask.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        myCoordinatesTask.cancel(false);
        wakeLock.release();
    }

    class find_me extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            logger("find_me doInBackground");

            locationManager = (LocationManager)
                    getSystemService(Context.LOCATION_SERVICE);
            locationListenerGPS = new MyGPSLocationListener();
            locationListenerNetwork = new MyNetworkLocationListener();


            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    locationManager.requestLocationUpdates(LocationManager
                            .NETWORK_PROVIDER, 0, 0, locationListenerGPS);
                    locationManager.requestLocationUpdates(LocationManager
                            .GPS_PROVIDER, 0, 0, locationListenerNetwork);

                    Looper.loop();
                }
            });
            t.start();

            return null;
        }
    }


    private class MyNetworkLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            locationListener(loc, locationListenerNetwork);
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStatusChanged(String provider,
                                    int status, Bundle extras) {
            // TODO Auto-generated method stub
        }

    }

    private class MyGPSLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            locationListener(loc, locationListenerGPS);
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(locationListenerGPS);
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onStatusChanged(String provider,
                                    int status, Bundle extras) {
            // TODO Auto-generated method stub
        }

    }

    void locationListener(Location loc, LocationListener locationListener){
        Toast.makeText(getBaseContext(), "Location changed : Lat: " +
                        loc.getLatitude() + " Lng: " + loc.getLongitude(),
                Toast.LENGTH_SHORT).show();

        double longitude = loc.getLongitude();
        Log.v(TAG, "Longitude: " + longitude);
        double latitude = loc.getLatitude();
        Log.v(TAG, "Latitude: " + latitude);

        sendMyLocation(String.valueOf(longitude), String.valueOf(latitude), null);

        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        logger("MyLocationListener " + locationListener);
        locationManager.removeUpdates(locationListener);
    }

    void sendMyLocation(String longitude, String latitude, String cityName){
        if (isConnectingToInternet(getApplicationContext())) {

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

            nameValuePairs.add(new BasicNameValuePair("action", "coordinates"));
            nameValuePairs.add(new BasicNameValuePair("latitude", latitude));
            nameValuePairs.add(new BasicNameValuePair("longitude", longitude));
            nameValuePairs.add(new BasicNameValuePair("token", myToken));

            makeResponse(nameValuePairs);

            db_action(ACTION_SELECT, null, null);

            if(!(coordinates.size()==0)){
                for(String [] line: coordinates){
                    nameValuePairs.clear();

                    nameValuePairs.add(new BasicNameValuePair("action", "coordinates"));
                    nameValuePairs.add(new BasicNameValuePair("latitude", line[0]));
                    nameValuePairs.add(new BasicNameValuePair("longitude", line[1]));
                    nameValuePairs.add(new BasicNameValuePair("token", myToken));

                    makeResponse(nameValuePairs);
                }

                db_action(ACTION_DELETE, null, null);
            }

        }else{
            db_action(ACTION_INSERT, latitude, longitude);
        }
    }

    public static boolean isConnectingToInternet(Context _context) {
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

    void db_action(int action, String latitude, String longitude){

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

                do {
                    // получаем значения по номерам столбцов и пишем все в лог
                    Log.d(TAG,
                            "ID = " + c.getInt(idColIndex) +
                                    ", latitude = " + c.getString(latitudeColIndex) +
                                    ", longitude = " + c.getString(longitudeColIndex));
                    // переход на следующую строку
                    // а если следующей нет (текущая - последняя), то false - выходим из цикла
                    coordinates.add(new String[] {c.getString(latitudeColIndex), c.getString(longitudeColIndex)});
                } while (c.moveToNext());
            } else
                Log.d(TAG, "0 rows");
            c.close();
        }else if(action==ACTION_DELETE){
            Log.d(TAG, "--- Clear mytable: ---");
            // удаляем все записи
            int clearCount = db.delete(dbTableName, null, null);
            Log.d(TAG, "deleted rows count = " + clearCount);
        }

        dbHelper.close();
    }
}
