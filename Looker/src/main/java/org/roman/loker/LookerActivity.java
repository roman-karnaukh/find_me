package org.roman.loker;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@TargetApi(16)
public class LookerActivity extends Activity implements View.OnClickListener{

    static final int GET_COORDINATES = 1;
    static final int GET_CUSTOMERS = 2;
    static final int GET_LOCATION = 3;
    private static final String ONE_LINE = "one_line_";

    static String API_KEY ="AIzaSyCSf3zyqMPUTorDiwmFGRjJeA2AiYIJ6Yw";
    static String[] pushResult = new String[2];
    static String jSonForTokenExecute;
    static String tokenId = null;
    static int ACTION = 0;
    static  Map<String, String> locations = new HashMap<String, String>();

    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
    String[]  places  = { null };
    ArrayAdapter<String> adapter;
    String myToken;

    Button btnSendPush, btnGetCustomers, btnSetCustomer, btnGetCoordinates, btnGetLocation;
    EditText etMessage, etToken, etDevice, etTokenId;
    TextView tvResponse;
    Spinner spinner;
    CheckBox checkBox;

    String downloadedString = null;
    boolean isAuto = true;

    MyTask mt;
    PushTask pt;
    ProgressBar pb;


    static String TAG  = "LOOOKER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_looker);


        btnSendPush = (Button) findViewById(R.id.btnSendPush);
        btnGetCustomers = (Button) findViewById(R.id.btnGetCustomers);
        btnSetCustomer = (Button) findViewById(R.id.btnSetCustomer);
        btnGetCoordinates = (Button) findViewById(R.id.btnGetCoordinates);
        btnGetLocation = (Button) findViewById(R.id.btnGetLocation);
        btnGetLocation.setEnabled(false);

        tvResponse = (TextView) findViewById(R.id.tvResponce);

        etMessage = (EditText) findViewById(R.id.message);
        etToken = (EditText) findViewById(R.id.etToken);
        etDevice = (EditText) findViewById(R.id.etDevice);
        etTokenId = (EditText) findViewById(R.id.tokenId);

        checkBox = (CheckBox) findViewById(R.id.checkBox);
        spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setPrompt("Choice coordinates you interested");
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {

                Object key = locations.keySet().toArray()[position];
                Object value = locations.get(key);
                String coordinates = parseCoordinates("{" + ONE_LINE + ":" + value + "}");

                if(!isAuto) {
                    Uri uri = Uri.parse(coordinates);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                }

                    isAuto = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });


        pb = (ProgressBar) findViewById(R.id.progress);
        pb.setVisibility(View.INVISIBLE);


        btnSendPush.setOnClickListener(this);
        btnGetCustomers.setOnClickListener(this);
        btnSetCustomer.setOnClickListener(this);
        btnGetCoordinates.setOnClickListener(this);
        btnGetLocation.setOnClickListener(this);
        tvResponse.setOnClickListener(this);

        pushEnabled(false);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                myToken = getMyToken();
            }
        });
        thread.start();

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.btnSendPush:
                Log.d(TAG, "btnSendPush" + " pressed");

                tokenId = etTokenId.getText().toString();
                pt = new PushTask();
                if(checkBox.isChecked()){
                    pt.execute("c");
                }else{
                    pt.execute(etMessage.getText().toString());
                }


                break;
            case R.id.btnGetCustomers:
                ACTION = GET_CUSTOMERS;

                nameValuePairs.clear();
                nameValuePairs.add(new BasicNameValuePair("action", "get"));

                mt = new MyTask();
                mt.execute(nameValuePairs);
                break;
            case R.id.btnSetCustomer:
                nameValuePairs.clear();
                nameValuePairs.add(new BasicNameValuePair("action", "register"));
                nameValuePairs.add(new BasicNameValuePair("device", etDevice.getText().toString()));
                nameValuePairs.add(new BasicNameValuePair("token", etToken.getText().toString()));

                mt = new MyTask();
                mt.execute(nameValuePairs);
                break;
            case R.id.tvResponce:
                tvResponse.setText("");
                break;
            case R.id.btnGetCoordinates:
                ACTION = GET_COORDINATES;
                isAuto = true;

                tokenId = etTokenId.getText().toString();

                nameValuePairs.clear();
                nameValuePairs.add(new BasicNameValuePair("action", "get_coordinates"));
                if(!tokenId.equals("")){
                    nameValuePairs.add(new BasicNameValuePair("token", getToken(tokenId)));
                }
                Log.d(TAG, "tokenId "  + tokenId);
                mt = new MyTask();
                mt.execute(nameValuePairs);

                btnGetLocation.setEnabled(true);

                break;
            case R.id.btnGetLocation:
                ACTION = GET_LOCATION;

                String coordinates = parseCoordinates(downloadedString);
                Uri uri = Uri.parse(coordinates);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);

                break;
        }

    }

    class MyTask extends AsyncTask<List<NameValuePair>, Void, List<NameValuePair>>{

        @Override
        protected List<NameValuePair>  doInBackground(List<NameValuePair>... params) {
            sendRequest(params);
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pb.setVisibility(View.VISIBLE);
            tvResponse.setText("sending request");
        }

        @Override
        protected void onPostExecute(List<NameValuePair> aVoid) {
            pb.setVisibility(View.INVISIBLE);
            tvResponse.setText(downloadedString);

            switch (ACTION){
                case GET_COORDINATES:
                    places = getPlaces(downloadedString);

                    adapter = new ArrayAdapter<String>(LookerActivity.this, R.layout.spinner_item, places);
//                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

                    spinner.setAdapter(adapter);
                    break;
                case GET_CUSTOMERS:
                    jSonForTokenExecute = downloadedString;
                    pushEnabled(true);
                    break;
                case GET_LOCATION:
                    break;
                default:
                    break;
            }

        }
    }

    class PushTask extends AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... params) {
            sendPush(params[0], tokenId);
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pb.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            pb.setVisibility(View.INVISIBLE);
            tvResponse.setText(pushResult[0] + "\n" + pushResult[1]);
        }
    }


    private void sendPush(String message, String tokenId){
        String token = getToken(tokenId);

        String[] array = {message, myToken};
        Gson gson = new Gson();
        message = gson.toJson(array);

        Log.d(TAG, message);

        String[] args = new String[] { message, token };
        try {
            // Prepare JSON containing the GCM message content. What to send and where to send.
            JSONObject jGcmData = new JSONObject();
            JSONObject jData = new JSONObject();
            jData.put("message", args[0].trim());
            // Where to send GCM message.
            if (args.length > 1 && args[1] != null) {
                jGcmData.put("to", args[1].trim());
            } else {
                jGcmData.put("to", "/topics/global");
            }
            // What to send in GCM message.
            jGcmData.put("data", jData);

            // Create connection to send GCM Message request.
            URL url = new URL("https://android.googleapis.com/gcm/send");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "key=" + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // Send GCM message content.
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(jGcmData.toString().getBytes());
                pushResult[0] = jGcmData.toString();

            // Read GCM response.
            InputStream inputStream = conn.getInputStream();
            String resp = IOUtils.toString(inputStream);
                pushResult[1] = resp;
            System.out.println("Check your device/emulator for notification or logcat for " +
                    "confirmation of the receipt of the GCM message.");
        } catch (IOException e) {
            System.out.println("Unable to send GCM message.");
            System.out.println("Please ensure that API_KEY has been replaced by the server " +
                    "API key, and that the device's registration token is correct (if specified).");
            e.printStackTrace();
        } catch (JSONException er){

        }
    }

    String sendRequest(List<NameValuePair>[]  nameValuePairs) {
        return sendRequest(nameValuePairs[0]);
    }

    String sendRequest(List<NameValuePair> nameValuePairs){
        String url = "http://task-master.zzz.com.ua/server.php";
        Log.d(TAG, "sendRequest");
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(url);
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));


            HttpResponse response = httpclient.execute(httppost);
            InputStream in = response.getEntity().getContent();

            StringBuilder stringbuilder = new StringBuilder();
            BufferedReader bfrd = new BufferedReader(new InputStreamReader(in),1024);
            String line;
            while((line = bfrd.readLine()) != null)
                stringbuilder.append(line);

            downloadedString = stringbuilder.toString();
            Log.d(TAG, downloadedString);


        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }
        return downloadedString;
    }

    static String getToken(String tokenId){
        String token = null;
        try{
            JSONObject jsonObject = new JSONObject(jSonForTokenExecute);
            token =  jsonObject.getJSONObject("customer" + tokenId).getString("token");

        }catch (JSONException err){

        }catch (NullPointerException e){
            token = "c-lvW1rZH0Q:APA91bH9GMOYne2KZ0Et49lN4ABQNbWdB6RXMyCnpMWnWVFSTDcCBabBkYI2WtBfCPIUpLncpgQ2kpbcY_5ToXKikKfVd-xWZS_rD3MnFZclw0_BJHmSKkD4VItzdwBirvQ91lT-Le3O";
        }

        Log.d(TAG, "getToken "  + token);
        return token;
    }

    private static String parseCoordinates(String input) {
        String coordinates = null;
        String latitude;
        String longitude;

        try{
            JSONObject jsonObject = new JSONObject(input);
            Log.d(TAG, String.valueOf(jsonObject.length()));


            if(!jsonObject.toString().contains(ONE_LINE)) {
                Iterator<?> keys = jsonObject.keys();

                JSONArray jsonArray = new JSONArray();

                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    jsonArray.put(jsonObject.get(key));

                }


                JSONObject jsonObject2 = (JSONObject) jsonArray.get(0);

                latitude = jsonObject2.get("latitude").toString();
                longitude = jsonObject2.get("longitude").toString();

            }else{
                jsonObject = jsonObject.getJSONObject(ONE_LINE);
                latitude = jsonObject.get("latitude").toString();
                longitude = jsonObject.get("longitude").toString();

            }

            coordinates = "geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude + "(Location)";

            Log.d(TAG, coordinates);
        }catch (JSONException err){
            err.printStackTrace();
            Log.d(TAG, err.getMessage() + " input: " + input);

        }catch (NullPointerException e){
            coordinates = "geo:55.754283,37.62002?q=55.754283,37.62002(Location)";
        }

        return coordinates;
    }

    private String[] getPlaces(String response){
        locations.clear();

        try{
            JSONObject jsonObject = new JSONObject(response);
            Iterator<?> keys = jsonObject.keys();

            while (keys.hasNext()){

                String key = (String)keys.next();
                JSONObject oneRecord = jsonObject.getJSONObject(key);

                key = oneRecord.getString("city_name") + " : " + oneRecord.getString("recg_date");

                locations.put(key, oneRecord.toString());

            }



        }catch (JSONException err){
            Log.d(TAG, err.getMessage());

        }catch (NullPointerException e){
        }


        Set<String> set = locations.keySet();

        return set.toArray(new String[set.size()]);
    }

    void pushEnabled(boolean value){
        etTokenId.setEnabled(value);
        etMessage.setEnabled(value);
        btnSendPush.setEnabled(value);
    }

    String getMyToken(){
        String device = Build.MANUFACTURER.toUpperCase() + "-"+ Build.MODEL + "_" +
                " " + Build.VERSION.SDK_INT;

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("action", "get_my_token"));
        nameValuePairs.add(new BasicNameValuePair("device", device));
        return sendRequest(nameValuePairs);
    }
}

