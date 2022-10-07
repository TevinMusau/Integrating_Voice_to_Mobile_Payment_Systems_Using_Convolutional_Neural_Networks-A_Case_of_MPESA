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
    private ImageButton startRecording;
    private MediaRecorder mediaRecorder;

    Secrets picovoiceSecret;

    PorcupineManager porcupineManager;

    // create an instance of the class to record audio in .wav format
    WavClass wavObj = new WavClass(Environment.getExternalStorageDirectory().getAbsolutePath());

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        picovoiceSecret = new Secrets();

        File file = new File("java/com/tevin/mvoice/Secrets.java");
        if ((file.exists()))
        {
            Log.d("Yes", "It's there");
        }

        try {
            porcupineManager = new PorcupineManager.Builder()
                    .setAccessKey(picovoiceSecret.getPicovoiceAccessKey())
                    .setKeywordPaths(new String[]{"assets/start.ppn", "assets/transact.ppn"})
                    .build(MainActivity.this, new PorcupineManagerCallback() {
                        @Override
                        public void invoke(int keywordIndex) {
                            if (keywordIndex == 0)
                            {
                                Log.d("Start", "We Begin");
                                beginRecording();
                            }
                            else
                            {
                                Log.d("End", "We are Done");
                                endRecording();
                            }
                        }
                    });
        } catch (PorcupineException e) {
            Log.d("No", "Smth Went Wrong");
            e.printStackTrace();
        }

        porcupineManager.start();
        Log.d("Good", "Porcupine Started!");

        // initializing buttons
//        startRecording = findViewById(R.id.startRecording);
//        stopRecording = findViewById(R.id.stopRecording);
//
//        startRecording.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (checkPermissions())
//                {
//                    // start voice input
//                    wavObj.startRecording();
//                }
//                else
//                {
//                    // request for permissions to record audio and write to ext. storage
//                    requestPermissions();
//                }
//            }
//        });
//
//        stopRecording.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // stop recording
//                wavObj.stopRecording();
//                Toast.makeText(MainActivity.this, "Recording Stopped", Toast.LENGTH_SHORT).show();
//            }
//        });
    }
    private void endRecording()
    {
        try {
            porcupineManager.stop();
        } catch (PorcupineException e) {
            e.printStackTrace();
        }
        Log.d("VoiceIN", "Stopped!");
        wavObj.stopRecording();
        Toast.makeText(MainActivity.this, "Voice Input Stopped", Toast.LENGTH_SHORT).show();
    }

    private void beginRecording()
    {
        wavObj.startRecording();
        Log.d("VoiceIN","Started!");
        Toast.makeText(MainActivity.this, "Started Voice Input", Toast.LENGTH_SHORT).show();
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