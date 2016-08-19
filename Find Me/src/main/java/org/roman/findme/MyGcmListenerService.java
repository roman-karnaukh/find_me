/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.roman.findme;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmListenerService;
import com.google.gson.Gson;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.roman.findme.RegistrationIntentService.makeResponse;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";
    public static String senderToken = null;
    SharedPreferences sharedPreferences;
    public static String myToken = null;
    find_me myCoordinatesTask;

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        sharedPreferences = getSharedPreferences(RegistrationIntentService.APP_PREFERENCES, Context.MODE_PRIVATE);
        myToken = sharedPreferences.getString("my_token", null);

        String message = data.getString("message");
        assert message != null;

        Gson gson = new Gson();
        String[] response = gson.fromJson(message, String[].class);

        message = response[0];
        senderToken = response[1].replaceAll("\\s+","");

        Log.d(TAG, data.toString());
        Log.d(TAG, message);
        Log.d(TAG, senderToken);

        if (from.startsWith("/topics/")) {
            // message received from some topic.
        } else {
            // normal downstream message.
        }


        if (message.equals("c")) {
            getCoordinates();
        } else {
            sendNotification(message);
        }

    }



    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message) {
        Intent intent = new Intent(this, MessageActivity.class);
        intent.putExtra("message", message);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setContentTitle("Message")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());

        if(sharedPreferences.getBoolean("display_message", true)){
            displayMessage(message);
        }
    }

    void displayMessage(String message){
       if(MessageActivity.active){
           MessageActivity.addSenderMessage(message);
       }else{
           Intent intent = new Intent(this, MessageActivity.class);
           intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
           intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
           intent.putExtra("message", message);
           startActivity(intent);
       }

    }

    /*****************Location****************/
    private LocationManager locationManager = null;
    private LocationListener locationListenerNetwork = null;
    private LocationListener locationListenerGPS = null;


    void getCoordinates() {

        myCoordinatesTask = new find_me();
        myCoordinatesTask.execute();
    }

    void stopSendCoordinates() {

    }

    class find_me extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            locationManager = (LocationManager)
                    getSystemService(Context.LOCATION_SERVICE);
            locationListenerGPS = new MyGPSLocationListener();
            locationListenerNetwork = new MyNetworkLocationListener();


            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    if (ActivityCompat.checkSelfPermission(MyGcmListenerService.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MyGcmListenerService.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    locationManager.requestLocationUpdates(LocationManager
                            .NETWORK_PROVIDER, 0, 0, locationListenerGPS);
                    locationManager.requestLocationUpdates(LocationManager
                            .GPS_PROVIDER, 0, 0, locationListenerNetwork);

//                   locationManager.removeUpdates(locationListener);
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
            Toast.makeText(getBaseContext(), "Location changed : Lat: " +
                            loc.getLatitude() + " Lng: " + loc.getLongitude(),
                    Toast.LENGTH_SHORT).show();

            double longitude = loc.getLongitude();
            Log.v(TAG, "Longitude: " + longitude);
            double latitude = loc.getLatitude();
            Log.v(TAG, "Latitude: " + latitude);

    /*----------to get City-Name from coordinates ------------- */
            String cityName = null;
            Geocoder gcd = new Geocoder(getBaseContext(),
                    Locale.ENGLISH);
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(loc.getLatitude(), loc
                        .getLongitude(), 1);
                if (addresses.size() > 0)
                    System.out.println(addresses.get(0).getLocality());
                cityName = addresses.get(0).getLocality();
            } catch (IOException e) {
                e.printStackTrace();
            }

            sendMyLocation(longitude, latitude, cityName);
            if (ActivityCompat.checkSelfPermission(MyGcmListenerService.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MyGcmListenerService.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(locationListenerNetwork);
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

        void sendMyLocation(double longitude, double latitude, String cityName){

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("action", "coordinates"));
            nameValuePairs.add(new BasicNameValuePair("latitude", String.valueOf(latitude)));
            nameValuePairs.add(new BasicNameValuePair("longitude", String.valueOf(longitude)));
            nameValuePairs.add(new BasicNameValuePair("cityName", cityName));
            nameValuePairs.add(new BasicNameValuePair("token", myToken));

            makeResponse(nameValuePairs);
        }
    }

    private class MyGPSLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            Toast.makeText(getBaseContext(), "Location changed : Lat: " +
                            loc.getLatitude() + " Lng: " + loc.getLongitude(),
                    Toast.LENGTH_SHORT).show();

            double longitude = loc.getLongitude();
            Log.v(TAG, "Longitude: " + longitude);
            double latitude = loc.getLatitude();
            Log.v(TAG, "Latitude: " + latitude);

    /*----------to get City-Name from coordinates ------------- */
            String cityName = null;
            Geocoder gcd = new Geocoder(getBaseContext(),
                    Locale.ENGLISH);
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(loc.getLatitude(), loc
                        .getLongitude(), 1);
                if (addresses.size() > 0)
                    System.out.println(addresses.get(0).getLocality());
                cityName = addresses.get(0).getLocality();
            } catch (IOException e) {
                e.printStackTrace();
            }

            sendMyLocation(longitude, latitude, cityName);
            if (ActivityCompat.checkSelfPermission(MyGcmListenerService.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MyGcmListenerService.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(locationListenerGPS);
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

        void sendMyLocation(double longitude, double latitude, String cityName){

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("action", "coordinates"));
            nameValuePairs.add(new BasicNameValuePair("latitude", String.valueOf(latitude)));
            nameValuePairs.add(new BasicNameValuePair("longitude", String.valueOf(longitude)));
            nameValuePairs.add(new BasicNameValuePair("cityName", cityName));
            nameValuePairs.add(new BasicNameValuePair("token", myToken));

            makeResponse(nameValuePairs);
        }
    }
}
