package org.roman.loker;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
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
import java.util.List;

@TargetApi(16)
public class LookerActivity extends Activity implements View.OnClickListener{
    static String API_KEY ="AIzaSyCSf3zyqMPUTorDiwmFGRjJeA2AiYIJ6Yw";
    static String[] pushResult = new String[2];
    static String jSonResponse;
    static String tokenId = null;

    Button btnSendPush, btnGetCustomers, btnSetCustomer;
    EditText etMessage, etToken, etDevice, etTokenId;
    TextView tvResponse;
    Button btnSendReq;
    String downloadedString = null;

    MyTask mt;
    PushTask pt;
    ProgressBar pb;

    String TAG  = "LOOOKER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_looker);

        btnSendReq = (Button) findViewById(R.id.btnSendReq);
        btnSendPush = (Button) findViewById(R.id.btnSendPush);
        btnGetCustomers = (Button) findViewById(R.id.btnGetCustomers);
        btnSetCustomer = (Button) findViewById(R.id.btnSetCustomer);

        tvResponse = (TextView) findViewById(R.id.tvResponce);

        etMessage = (EditText) findViewById(R.id.message);
        etToken = (EditText) findViewById(R.id.etToken);
        etDevice = (EditText) findViewById(R.id.etDevice);
        etTokenId = (EditText) findViewById(R.id.tokenId);

        pb = (ProgressBar) findViewById(R.id.progress);
        pb.setVisibility(View.INVISIBLE);


        btnSendReq.setOnClickListener(this);
        btnSendPush.setOnClickListener(this);
        btnGetCustomers.setOnClickListener(this);
        btnSetCustomer.setOnClickListener(this);
        tvResponse.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnSendReq:
                Log.d(TAG, "btnSendReq pressed");

                List<NameValuePair> nameValuePairs1 = new ArrayList<NameValuePair>(2);
                nameValuePairs1.add(new BasicNameValuePair("name", "Roman"));

                mt = new MyTask();
                mt.execute(nameValuePairs1);
                break;
            case R.id.btnSendPush:
                Log.d(TAG, "btnSendPush" + " pressed");

                tokenId = etTokenId.getText().toString();
                pt = new PushTask();
                pt.execute(etMessage.getText().toString());


                break;
            case R.id.btnGetCustomers:
                List<NameValuePair> nameValuePairs2 = new ArrayList<NameValuePair>();
                nameValuePairs2.add(new BasicNameValuePair("action", "get"));

                mt = new MyTask();
                mt.execute(nameValuePairs2);
                break;
            case R.id.btnSetCustomer:
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
                nameValuePairs.add(new BasicNameValuePair("action", "register"));
                nameValuePairs.add(new BasicNameValuePair("device", etDevice.getText().toString()));
                nameValuePairs.add(new BasicNameValuePair("token", etToken.getText().toString()));

                mt = new MyTask();
                mt.execute(nameValuePairs);
                break;
            case R.id.tvResponce:
                tvResponse.setText("");
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
            tvResponse.setText("send request");
        }

        @Override
        protected void onPostExecute(List<NameValuePair> aVoid) {
            pb.setVisibility(View.INVISIBLE);
            tvResponse.setText(downloadedString);
            if(downloadedString.contains("token")){
                jSonResponse = downloadedString;
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


    static void sendPush(String message, String tokenId){
        String token = null;
        try{
            JSONObject jsonObject = new JSONObject(jSonResponse);
            token = jsonObject.getJSONObject("customer" + tokenId).getString("token");

        }catch (JSONException err){

        }catch (NullPointerException e){
            token = "c-lvW1rZH0Q:APA91bH9GMOYne2KZ0Et49lN4ABQNbWdB6RXMyCnpMWnWVFSTDcCBabBkYI2WtBfCPIUpLncpgQ2kpbcY_5ToXKikKfVd-xWZS_rD3MnFZclw0_BJHmSKkD4VItzdwBirvQ91lT-Le3O";
        }

        String[] args = new String[] {message, token};
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

    void sendRequest(List<NameValuePair>[]  nameValuePairs){
        String url = "http://task-master.zzz.com.ua/server.php";
        Log.d(TAG, "sendRequest");
        try {
            HttpClient httpclient = new DefaultHttpClient();
//            HttpGet httppost = new HttpGet(url + "?name=Roman");
            HttpPost httppost = new HttpPost(url);
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs[0]));


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
    }

}
