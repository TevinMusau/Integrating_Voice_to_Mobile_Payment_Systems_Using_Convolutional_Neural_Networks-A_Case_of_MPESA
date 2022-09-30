package com.tevin.mvoice;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void voiceInput(View view)
    {
        // Telling us voice input is beginning
        Toast.makeText(MainActivity.this, "Beginning Voice Input...", Toast.LENGTH_LONG).show();
    }
}