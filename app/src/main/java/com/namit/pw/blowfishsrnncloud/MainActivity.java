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
            Intent i=new Intent(MainActivity.this, BlowfishMainActivity.class);
            // value in CAPITALS:
            i.putExtra("whichCipher", "BLOWFISH");
            startActivity(i);
        });

        awsCloudDataBtn.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, AwsCloudDataActivity.class));
        });

        aesBtn.setOnClickListener(view -> {
            Intent i=new Intent(MainActivity.this, BlowfishMainActivity.class);
            // value in CAPITALS:
            i.putExtra("whichCipher", "AES");
            startActivity(i);
        });
    }
}