package com.tevin.mvoice;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import ai.picovoice.picovoice.*;
import ai.picovoice.porcupine.*;

//import com.chaquo.python.PyObject;
//import com.chaquo.python.Python;
//import com.chaquo.python.android.AndroidPlatform;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity
{
    // initializing constants
    private Button stopRecording;

    // testing textview
    private TextView test;

    // instance of Secrets class where secrete credentials are stored
    Secrets porcupineSecret;

    // Porcupine instance
    PorcupineManager porcupineManager;

    // for setting time that the recording was captured
    int current_time;

    /* create instances of the class to record audio in .wav format
    * These instances represent different categories of voice prints
    * e.g., raw, full duration voice prints will have the instance which directs to the path where they will be stored
    * */
    WavClass wavObj = new WavClass(Environment.getExternalStorageDirectory().getAbsolutePath() + "/MVoice/Raw Voice Prints");

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        // specify python file
        PyObject pythonFile = python.getModule("split_audio");

        // instance of secrets class that houses client secrets
        porcupineSecret = new Secrets();

        // instantiate the testing text view
        test = findViewById(R.id.test_textview);

        // initializing stop button
        stopRecording = findViewById(R.id.stopRecording);

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

        // stop recording when this button is clicked
        stopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // stop recording
                current_time = wavObj.stopRecording();

                // create a subdirectory with timestamp
                String raw_voice_prints_split_with_timestamp_subdir_status = create_child_subdirectories("Voice Prints Split", String.valueOf(current_time));
                Log.d("V.SplitStamp SubDir:", raw_voice_prints_split_with_timestamp_subdir_status);

                String result =  pythonFile.callAttr("split_audio", "/storage/emulated/0/MVoice/Raw Voice Prints", current_time).toString();
                test.setText(result);
                Toast.makeText(MainActivity.this, "Stopping...", Toast.LENGTH_SHORT).show();
            }
        });
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
//    private void endRecording()
//    {
//        try {
//            porcupineManager.stop();
//            porcupineManager.delete();
//        } catch (PorcupineException e) {
//            e.printStackTrace();
//        }
//        Log.d("VoiceIN", "Stopped!");
//        wavObj.stopRecording();
//        Toast.makeText(MainActivity.this, "Voice Input Stopped", Toast.LENGTH_SHORT).show();
//    }

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