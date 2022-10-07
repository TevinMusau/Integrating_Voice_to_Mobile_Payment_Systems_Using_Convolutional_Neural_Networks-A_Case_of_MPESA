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
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

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

    // path to save the audio recordings
    private String AudioSavePath = null;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WavClass wavObj = new WavClass(Environment.getExternalStorageDirectory().getAbsolutePath());

        startRecording = findViewById(R.id.startRecording);
        stopRecording = findViewById(R.id.stopRecording);

        startRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermissions())
                {
                    wavObj.startRecording();
                }
                else
                {
                    requestPermissions();
                }
            }
        });

        stopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wavObj.stopRecording();
                Toast.makeText(MainActivity.this, "Recording Stopped", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean checkPermissions(){
        int first = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int second = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        return first == PackageManager.PERMISSION_GRANTED && second == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions()
    {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }


    public void voiceInput(View view)
    {
        // Telling us voice input is beginning
        Toast.makeText(MainActivity.this, "Beginning Voice Input...", Toast.LENGTH_LONG).show();
    }
}