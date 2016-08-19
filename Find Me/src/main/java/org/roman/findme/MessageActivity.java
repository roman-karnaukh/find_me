package org.roman.findme;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MessageActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener {
    private static final String TAG = "FIND ME ACTIVITY";
    private String MESSAGE = null;
    static String API_KEY ="AIzaSyCSf3zyqMPUTorDiwmFGRjJeA2AiYIJ6Yw";
    static boolean active = false;
    static boolean HOLD_MESSAGE_LIST = false;
    public static Handler UIHandler;
    static LayoutInflater inflater;
    static MessageActivity messageActivity;


    Button btnSendMessage;
    TextView tvMessage;
    static EditText edMessage;

    static InputMethodManager imm;
    PushTask pushTask;

    static LinearLayout mainContainer, emptyElement;
    static ScrollView scrollView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        UIHandler = new Handler(Looper.getMainLooper());
        messageActivity = this;

        inflater = (LayoutInflater) messageActivity.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        try{
            Intent inputIntent = getIntent();
            MESSAGE = inputIntent.getExtras().getString("message");
        }catch (NullPointerException er){}


        btnSendMessage = (Button) findViewById(R.id.btnSendMessage);
        btnSendMessage.setOnClickListener(this);
        btnSendMessage.setOnLongClickListener(this);

        tvMessage = (TextView) findViewById(R.id.tvMessage);
        tvMessage.setText(MESSAGE);
        tvMessage.setOnLongClickListener(this);

        mainContainer =  (LinearLayout) messageActivity.findViewById(R.id.mainContainer);
        scrollView = (ScrollView) findViewById(R.id.scrollView);

        edMessage = (EditText) findViewById(R.id.edMessage);
        edMessage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                return false;
            }
        });


        emptyElement = (LinearLayout) findViewById(R.id.emptyElement);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnSendMessage:
                sendMessage(v);

                if(!HOLD_MESSAGE_LIST){
                    finish();
                }

                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(55);

        switch (v.getId()){
            case R.id.btnSendMessage:
                sendMessage(v);

                HOLD_MESSAGE_LIST = true;
                break;
            default:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("Message", ((TextView)v).getText().toString()));
                break;
        }


        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        active = false;
    }

    class PushTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            sendPush(params[0], MyGcmListenerService.senderToken);
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }


    private void sendPush(String message, String token){


        String[] array = {message, MyGcmListenerService.myToken};
        Gson gson = new Gson();
        message = gson.toJson(array);
        Log.d(TAG, message);

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
            Log.d(TAG, jGcmData.toString());

            // Read GCM response.
            InputStream inputStream = conn.getInputStream();
            String resp = IOUtils.toString(inputStream);
            Log.d(TAG, resp);

        } catch (IOException e) {
            System.out.println("Unable to send GCM message.");
            System.out.println("Please ensure that API_KEY has been replaced by the server " +
                    "API key, and that the device's registration token is correct (if specified).");
            e.printStackTrace();
        } catch (JSONException er){

        }
    }


    void closeSoftKeyboard(View v){
//        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    static void showSoftKeyboard(View v){
//        imm.showSoftInput(v, 0);
    }


    void sendMessage(View v){
        String message = edMessage.getText().toString();
        pushTask = new PushTask();
        pushTask.execute(message);

        addMyAnswer(message);
        edMessage.setText(null);
        closeSoftKeyboard(v);
    }

    public static void addSenderMessage(final String message){
        UIHandler.post(new Runnable() {
            @Override
            public void run() {

                LinearLayout senderContainer;
                TextView tvSenderMsg;


                View view = inflater.inflate(R.layout.sender_message_container, null);
                senderContainer = (LinearLayout)view.findViewById(R.id.senderMessageContainer);
                senderContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.weight = 1.0f;
                params.gravity = Gravity.START;
                params.bottomMargin = 10;
                senderContainer.setLayoutParams(params);

                tvSenderMsg = (TextView)inflater.inflate(R.layout.sender_tv, null).findViewById(R.id.tvSenderMessage);
                tvSenderMsg.setText(message);

                senderContainer.addView(tvSenderMsg);

                mainContainer.addView(senderContainer);

                mainContainer.removeView(emptyElement);
                mainContainer.addView(emptyElement);

                showSoftKeyboard(edMessage);

                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

    }

    void addMyAnswer(String message){
        LinearLayout receiverContainer;
        TextView tvSenderMsg;

        View view = inflater.inflate(R.layout.reciver_message_container, null);
        receiverContainer = (LinearLayout)view.findViewById(R.id.receiverContainer);

        tvSenderMsg = (TextView)inflater.inflate(R.layout.receiver_tv, null).findViewById(R.id.tvAnswerContainer);
        tvSenderMsg.setGravity(Gravity.BOTTOM | Gravity.END);
        tvSenderMsg.setText(message);
        receiverContainer.addView(tvSenderMsg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.weight = 1.0f;
        params.gravity = Gravity.END;
        params.bottomMargin = 10;
        receiverContainer.setLayoutParams(params);


        mainContainer.addView(receiverContainer);

        mainContainer.removeView(emptyElement);
        mainContainer.addView(emptyElement);

        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

}
