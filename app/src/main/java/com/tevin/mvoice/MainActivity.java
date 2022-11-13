package com.tevin.mvoice;

import static com.tevin.mvoice.Constants.BUSINESS_SHORT_CODE;
import static com.tevin.mvoice.Constants.CALLBACKURL;
import static com.tevin.mvoice.Constants.PARTYB;
import static com.tevin.mvoice.Constants.PASSKEY;
import static com.tevin.mvoice.Constants.PHONE_NUMBER;
import static com.tevin.mvoice.Constants.TRANSACTION_TYPE;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.tevin.mvoice.Services.DarajaApiClient;
import com.tevin.mvoice.model.AccessToken;
import com.tevin.mvoice.model.STKPush;

import ai.picovoice.porcupine.*;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity
{
    private DarajaApiClient mApiClient;

    // initializing constants
    private Button stopRecording;

    // testing textview
    private TextView test;

    // instance of Secrets class where secrete credentials are stored
    Secrets porcupineSecret;

    // instance of Secrets class where flask address is stored
    Secrets flaskAddress = new Secrets();

    // Porcupine instance
    PorcupineManager porcupineManager;

    // for setting time that the recording was captured
    int current_time;
    int time;

    /* create instances of the class to record audio in .wav format
    * These instances represent different categories of voice prints
    * e.g., raw, full duration voice prints will have the instance which directs to the path where they will be stored
    * */
    WavClass wavObj = new WavClass(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MVoice/Raw Voice Prints");

    private String POST = "POST";
    private String url = flaskAddress.getFlaskAddress();
    ArrayList<String> predictedWords = new ArrayList<>();

    private TextToSpeech text_to_speech;

    // client instance
    OkHttpClient client = new OkHttpClient();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // instance of Daraja Client Class
        mApiClient = new DarajaApiClient();

        // setting debug to True (for testing)
        mApiClient.setIsDebug(true);

        // get an access token from MPESA
        getAccessToken();

        // check if there is an instance of Python already running
        if (!Python.isStarted()){
            Python.start(new AndroidPlatform(this));
        }

        // create the main MVoice Dir
        String MVoice_directory_status = create_MVoice_Main_Directory();
        Log.d("MVoice Directory:", MVoice_directory_status);

        // create the raw voice prints subDir
        String raw_voice_prints_subdir_status = create_subdirectories("Raw Voice Prints");
        Log.d("RawVoicePrints SubDir:", raw_voice_prints_subdir_status);

        // create the raw voice prints split to 1 sec intervals subDir
        String raw_voice_prints_split_subdir_status = create_subdirectories("Voice Prints Split");
        Log.d("RawVoicePrints SubDir:", raw_voice_prints_split_subdir_status);

        // create python instance
        Python python = Python.getInstance();

        // specify python files
        PyObject pythonFile = python.getModule("split_audio");
        PyObject pythonFile_Concat = python.getModule("concatenate_audio");

        // instance of secrets class that houses client secrets
        porcupineSecret = new Secrets();

        // instantiate the testing text view
        test = findViewById(R.id.test_textview);

        // initializing stop button
        stopRecording = findViewById(R.id.stopRecording);

        // Function to call Porcupine
        listenForWakeWord();

        // stop recording when this button is clicked
        stopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // stop recording
                int time1 = stopRecord(pythonFile);
                Log.d("Tiiimmme1", String.valueOf(time1));

                try {
                    // send request to flask server and get response
                    sendRequest(time1, new ApiCallback() {
                        @Override
                        public void onOkHttpResponse(String data, int turn, int numberOfFiles) {
                            // Executes when we get response from Flask Server

                            /*
                            * The number of Files is our reference.
                            * We check if the ArrayList, which stores the predictions,
                            * has a length equal to the total number of files being 'predicted upon'.
                            * To ensure that on the last file, we use the full ArrayList,
                            * we take ArrayList length + 1, where the 1 accounts for the last file
                            *
                            * */
                            if ((predictedWords.size()+1 != numberOfFiles)){
                                // add to the ArrayList
                                predictedWords.add(data);

                                System.out.println("Added: "+predictedWords);
                                System.out.println(predictedWords.size());
                                System.out.println(predictedWords);
                            }
                            else {
                                // add the prediction on the last file
                                predictedWords.add(data);

                                System.out.println("Final: " +predictedWords);
                                System.out.println("ArrayList Size: "+predictedWords.size());

                                // to store the ArrayList as an array
                                String[] final_response_array = new String[predictedWords.size()+1];

                                // Join the ArrayList elements with a comma and a space and store as string
                                String responsePredictions = String.join(", ", predictedWords);
                                System.out.println("TO STRING: "+responsePredictions);

                                // Split at the point of comma and space and store the predictions in an array
                                final_response_array = responsePredictions.split(", ");

                                // sort the array in ascending order
                                Arrays.sort(final_response_array);

                                // we need to remove digits appended onto the predicted words
                                for (int i = 0; i < final_response_array.length; i++){
                                    String final_result = final_response_array[i].replaceAll("\\d", "");
                                    final_response_array[i] = final_result;
                                }

                                // convert the resulting sorted array that is free of digits into a string
                                String response_string = Arrays.toString(final_response_array);
                                String finalResponse_string = response_string;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        test.setText(finalResponse_string);
                                    }
                                });
                                response_string = finalResponse_string;
                                System.out.println(response_string);

                                // ---- DO TEXT TO SPEECH ----
                                // call TTS method
                                ttsInit(finalResponse_string);

                                // clear the Array List
                                predictedWords.clear();

                                /*
                                * The response is in form of words, i.e., one, two, three, etc
                                * We convert this to actual numbers, i.e., 123 would be equivalent to one, two, three
                                 */
                                String amount = wordsToNumbers(final_response_array);
                                Log.d("Amount", amount);

                                ttsInit("Your Amount is "+amount);
                                ttsInit("Please Enter Your MPESA PIN to complete the transaction");

                                // perform the STKPush
                                performSTKPush(amount);

                                // Allow user to confirm the response
                                // start Porcupine
//                                listenForWakeWord();


                            }
                        }

                        @Override
                        public void onOkHttpFailure(Exception exception) {
                            System.out.println("Error: "+ exception);
                        }
                    });
                    Toast.makeText(MainActivity.this, "Sending Request...", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (text_to_speech != null){
            // release resources held by TTS
            text_to_speech.stop();

            // stop TTS
            text_to_speech.shutdown();
        }
        super.onDestroy();
    }

    private void getAccessToken()
    {
        // if we got the access token set to true
        mApiClient.setGetAccessToken(true);
        mApiClient.mpesaService().getAccessToken().enqueue(new retrofit2.Callback<AccessToken>() {
            @Override
            public void onResponse(retrofit2.Call<AccessToken> call, retrofit2.Response<AccessToken> response) {
                if (response.isSuccessful()){
                    mApiClient.setAuthToken(response.body().accessToken);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<AccessToken> call, Throwable t) {
                Log.d("Auth Token","Didn't get Auth Token");
            }
        });
    }

    public void performSTKPush(String amount){
        // from Response Body
        String timestamp = Utils.getTimestamp();
        STKPush stkPush = new STKPush(
                BUSINESS_SHORT_CODE,
                Utils.getPassword(BUSINESS_SHORT_CODE, PASSKEY, timestamp),
                timestamp,
                TRANSACTION_TYPE,
                String.valueOf(amount),
                PHONE_NUMBER,
                PARTYB,
                PHONE_NUMBER,
                CALLBACKURL,
                "MVoice", // Account Reference
                "Testing" // Transaction Description
        );

        mApiClient.setGetAccessToken(false);

        // send a request to the API
        mApiClient.mpesaService().sendPush(stkPush).enqueue(new retrofit2.Callback<STKPush>() {
            @Override
            public void onResponse(retrofit2.Call<STKPush> call, retrofit2.Response<STKPush> response) {
                try{
                    if (response.isSuccessful()){
                        Log.d("STKPushAPI", "Post submitted to API");
                    }
                    else{
                        Log.d("STKPushError", response.errorBody().string());
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<STKPush> call, Throwable t) {
                Log.d("STKPushFail", String.valueOf(t));
            }
        });
    }

    // OkHttp Interface
    private interface ApiCallback{
        void onOkHttpResponse(String data, int turn, int numberOfFiles);
        void onOkHttpFailure(Exception exception);
    }

    private interface SVApiCallback{
        void onOkHttpResponseSV(String data);
        void onOkHttpFailureSV(Exception exception);
    }

    private void ttsInit(String text)
    {
        text_to_speech = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // check status of initialization
                if (status == TextToSpeech.SUCCESS) {
                    //set language to English
                    int result = text_to_speech.setLanguage(Locale.ENGLISH);

                    // check if the language is supported
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("text_to_speech", "Language not supported");
                    } else {
                        Log.d("text_to_speech: ","Good to Go!");

                        // speak out the response
                        text_to_speech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
                else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "TTS Initialization Failed!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private String wordsToNumbers(String [] array)
    {
        // the final string that holds the numbers
        StringBuilder result = new StringBuilder();
        Log.d("Array Len", String.valueOf(array.length));

        // loop through each of the values and append it to result
        for (int i = 0; i<array.length; i++){
            if (Objects.equals(array[i], "zero")){
                result.append("0");
            } else if (Objects.equals(array[i], "one")) {
                result.append("1");
            } else if (Objects.equals(array[i], "two")) {
                result.append("2");
            } else if (Objects.equals(array[i], "three")) {
                result.append("3");
            } else if (Objects.equals(array[i], "four")) {
                result.append("4");
            } else if (Objects.equals(array[i], "five")) {
                result.append("5");
            } else if (Objects.equals(array[i], "six")) {
                result.append("6");
            } else if (Objects.equals(array[i], "seven")) {
                result.append("7");
            } else if (Objects.equals(array[i], "eight")) {
                result.append("8");
            } else if (Objects.equals(array[i], "nine")) {
                result.append("9");
            } else {
            }
        }

        // check if the number generated starts with 0
        // if the number generated starts with zero(0) and it is not the only number present
        // then remove the zero and return the rest of the digits
        if (result.toString().startsWith("0")){
            if (result.length() != 1){
                result = new StringBuilder(result.toString().replace("0", ""));
            }
        }
        return result.toString();
    }

    private void sendRequest(int time, ApiCallback callback) throws IOException
    {
        int numberOfFiles = 0;

        // full url to the endpoint
        String fullURL = this.url+"/"+"predict";
        Log.d("URL:", fullURL);

        // count number of files under the current timestamp
        File dir = new File(Environment.getExternalStorageDirectory() + "/MVoice/Voice Prints Split/"+time);
        File[] files = dir.listFiles();

        if (files != null)
        {
            numberOfFiles = files.length;
            Log.d("NoOfFiles:", String.valueOf(numberOfFiles));

            for (int i = 1; i <= numberOfFiles; i++)
            {
                // get path to each 1 sec file
                String audio_path = "/storage/emulated/0/MVoice/Voice Prints Split/"+time+"/yeboo["+i+"]"+time+".wav";

                // convert audio file to byte stream
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(audio_path));
                int read;
                byte[] buff = new byte[1024];
                while ((read = in.read(buff)) > 0)
                {
                    out.write(buff, 0, read);
                }
                out.flush();
                byte[] audioBytes = out.toByteArray();

                // create a request body
                RequestBody postAudio = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", i+"yeboo", RequestBody.create(MediaType.parse("audio/wav"), audioBytes))
                        .build();

                // post the request and obtain a result once response is received
                postRequest(fullURL, postAudio, i, numberOfFiles, new ApiCallback() {
                    @Override
                    public void onOkHttpResponse(String data, int turn, int numberOfFiles) {
                        callback.onOkHttpResponse(data, turn, numberOfFiles);
                    }

                    @Override
                    public void onOkHttpFailure(Exception exception) {
                        callback.onOkHttpFailure(exception);
                    }
                });
            }
        }
    }

    private void postRequest(String url, RequestBody postBody, int turn, int numberOfFiles, ApiCallback callback)
    {
        // Posting the request
        Request request = new Request.Builder()
                .url(url)
                .post(postBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // cancel the call on fail and return exception
                callback.onOkHttpFailure(e);
                call.cancel();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
            {
                // get the predicted number from the model
                // for some reason it doesn't allow me to call for the response more than once and update a view
                Log.d("Success", "Response Received!");

                // get the response and store it
                String word = Objects.requireNonNull(response.body()).string();

                /*
                 * OkHttp requests are asynchronous and they do not work on the main thread
                 * Meaning, it doesn't block activity on the main thread
                 * Therefore, create an interface which will provide us the response when it is ready
                 *
                 * We track the file number and the number of files
                 */
                callback.onOkHttpResponse(word, turn, numberOfFiles);
            }
        });
    }

    private void sendRequestToSpeakerVerify(int time, SVApiCallback callback) throws IOException
    {
        // full url to the endpoint
        String fullURL = this.url+"/"+"verify_speaker";
        Log.d("URL:", fullURL);

        // check if file exists
        File file = new File(Environment.getExternalStorageDirectory() + "/MVoice/Voice Prints Split/concatenation"+time+".wav");

        if (file.exists()){
            String audio_path = "/storage/emulated/0/MVoice/Voice Prints Split/concatenation"+time+".wav";

            // convert audio file to byte stream
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(audio_path));
            int read;
            byte[] buff = new byte[1024];
            while ((read = in.read(buff)) > 0)
            {
                out.write(buff, 0, read);
            }
            out.flush();
            byte[] audioBytes = out.toByteArray();

            // create a request body
            RequestBody postAudio = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "concatenation.wav", RequestBody.create(MediaType.parse("audio/wav"), audioBytes))
                    .build();

            // post the request and obtain a result once response is received
            postRequestToSpeakerVerify(fullURL, postAudio, new SVApiCallback() {
                @Override
                public void onOkHttpResponseSV(String data) {
                    callback.onOkHttpResponseSV(data);
                }

                @Override
                public void onOkHttpFailureSV(Exception exception) {
                    callback.onOkHttpFailureSV(exception);
                }
            });
        }
        else{
            Log.d("Concat_File","Not Found");
            ttsInit("Something's not Right");
        }
    }

    private void postRequestToSpeakerVerify(String url, RequestBody postBody, SVApiCallback callback)
    {
        // Posting the request
        Request request = new Request.Builder()
                .url(url)
                .post(postBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // cancel the call on fail and return exception
                callback.onOkHttpFailureSV(e);
                call.cancel();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException
            {
                // get the predicted number from the model
                // for some reason it doesn't allow me to call for the response more than once and update a view
                Log.d("Success", "SV Response Received!");

                // get the response and store it
                String word = Objects.requireNonNull(response.body()).string();

                /*
                 * OkHttp requests are asynchronous and they do not work on the main thread
                 * Meaning, it doesn't block activity on the main thread
                 * Therefore, create an interface which will provide us the response when it is ready
                 *
                 * We track the file number and the number of files
                 */
                callback.onOkHttpResponseSV(word);
            }
        });
    }

    private int stopRecord(PyObject pythonFile) {
        // stop recording
        current_time = wavObj.stopRecording();

        // create a subdirectory with timestamp
        String raw_voice_prints_split_with_timestamp_subdir_status = create_child_subdirectories("Voice Prints Split", String.valueOf(current_time));
        Log.d("V.SplitStamp SubDir:", raw_voice_prints_split_with_timestamp_subdir_status);

        String result =  pythonFile.callAttr("split_audio", "/storage/emulated/0/MVoice/Raw Voice Prints", current_time).toString();
        test.setText(result);
        Toast.makeText(MainActivity.this, "Stopping...", Toast.LENGTH_SHORT).show();

        return current_time;
    }

    private void listenForWakeWord()
    {
        try {
            // start Porcupine Wake Word Builder
            porcupineManager = new PorcupineManager.Builder()
                    .setAccessKey(porcupineSecret.getPorcupineAccessKey())
                    .setKeyword(Porcupine.BuiltInKeyword.JARVIS)
                    .build(MainActivity.this, new PorcupineManagerCallback() {
                        @Override
                        public void invoke(int keywordIndex) {
                            if (keywordIndex == 0)
                            {
                                if (checkPermissions()) {
                                    // begin voice input
                                    beginRecording();
                                }
                                else {
                                    // request permissions for writing ext. storage and recording audio
                                    requestPermissions();
                                }
                            }
                            else
                            {
                                Log.d("Porcupine", "Invalid Keyword!!");
                                // end voice input
                                // endRecording();
                            }
                        }
                    });
        } catch (PorcupineException e) {
            Log.d("Error", "Something Went Wrong: " + e);
            e.printStackTrace();
        }

        // start the Porcupine Builder
        porcupineManager.start();
        Log.d("Porcupine", "Porcupine Started Successfully!");
    }

    private String create_child_subdirectories(String parent_directory, String child_directory)
    {
        // specifying dir name
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MVoice/" + parent_directory + "/" + child_directory);
        if (!dir.isDirectory()){
            // create the dir if it doesn't already exist
            if (dir.mkdir()) {
                return "Directory Created";
            }
            else{
                return "Unable to create Child Directory";
            }
        }
        else{
            return "Child Directory already exists";
        }
    }

    private String create_subdirectories(String directory_name)
    {
        // specifying dir name
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MVoice/" + directory_name);
        if (!dir.isDirectory()){
            // create the dir if it doesn't already exist
            if (dir.mkdir()) {
                return "Directory Created";
            }
            else{
                return "Unable to create MVoice Directory";
            }
        }
        else{
            return "MVoice Directory already exists";
        }
    }

    private String create_MVoice_Main_Directory()
    {
        // specifying dir name
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MVoice");
        if (!dir.isDirectory()){
            // create the dir if it doesn't already exist
            if (dir.mkdir()) {
                return "Directory Created";
            }
            else{
                return "Unable to create MVoice Directory";
            }
        }
        else{
            return "MVoice Directory already exists";
        }
    }

    private void beginRecording()
    {
        // Two processes, Porcupine and AudioRecorder, can't use the same microphone at the same time
        // Therefore, we must stop one of them
        try {
            // stopping Porcupine
            porcupineManager.stop();
            Log.d("Porcupine", "Porcupine Stopped!");

            // Clearing resources used by Porcupine
            porcupineManager.delete();
        } catch (PorcupineException e) {
            e.printStackTrace();
        }

        // Begin Voice input recording
        wavObj.startRecording();
        Log.d("AudioRecord","Voice Capture Started!");
        Toast.makeText(MainActivity.this, "Recording...", Toast.LENGTH_SHORT).show();
    }

    // check if permissions are granted for the app
    private boolean checkPermissions(){
        int first = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int second = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        return first == PackageManager.PERMISSION_GRANTED && second == PackageManager.PERMISSION_GRANTED;
    }

    // request permissions to record audio and write to ext. storage
    private void requestPermissions()
    {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }
}