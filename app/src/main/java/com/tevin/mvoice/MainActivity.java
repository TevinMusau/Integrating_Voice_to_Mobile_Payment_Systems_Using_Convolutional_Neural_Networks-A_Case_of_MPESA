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
import android.widget.Toast;
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

    // instance of Secrets class where secrete credentials are stored
    Secrets porcupineSecret;

    // Porcupine instance
    PorcupineManager porcupineManager;

    // create an instance of the class to record audio in .wav format
    WavClass wavObj = new WavClass(Environment.getExternalStorageDirectory().getAbsolutePath());

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        porcupineSecret = new Secrets();

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
                wavObj.stopRecording();
                Toast.makeText(MainActivity.this, "Stopping...", Toast.LENGTH_SHORT).show();
            }
        });
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