package com.namit.pw.blowfishsrnncloud;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button blowfishSrnnBtn, awsCloudDataBtn, aesBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        blowfishSrnnBtn = findViewById(R.id.blowfishSrnnBtn);
        awsCloudDataBtn = findViewById(R.id.awsCloudDataBtn);
        aesBtn = findViewById(R.id.aesEncryptBtn);

        blowfishSrnnBtn.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, BlowfishMainActivity.class));
        });

        awsCloudDataBtn.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, AwsCloudDataActivity.class));
        });

        aesBtn.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, BlowfishMainActivity.class));
        });
    }
}