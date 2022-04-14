package com.namit.pw.blowfishsrnncloud;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.Key;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class BlowfishMainActivity extends AppCompatActivity {

    public String ALGO_STR = "Blowfish SRNN";

    private final String ALGORITHM = "Blowfish";
    private String keyString;

    Button chooseFileBtn, startEncryptBtn;
    TextView successFileImportTextView, successFileEncryptedTextView, encryptedKeySrnnTextView, timeTakenTextView;
    EditText keyEditText;
    boolean encryptionSuccessFlag, uploadToAwsSuccessful;
    AlertDialog loadingDialog;

    InputStream inputFileStream;
    byte[] inputFileBytes, outputBytes;

    long startTime, finishTime, timeTaken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blowfish_main);

        chooseFileBtn = findViewById(R.id.chooseFileBlowfishBtn);
        startEncryptBtn = findViewById(R.id.startEncryptBtnBlowfishEncrypt);
        successFileImportTextView = findViewById(R.id.successFileImportBlowfishEncryptTextView);
        successFileEncryptedTextView = findViewById(R.id.successFileEncryptedBlowfishEncryptTextView);
        keyEditText = findViewById(R.id.editTextKeyBlowfishEncrypt);
        encryptedKeySrnnTextView = findViewById(R.id.encryptedKeySrnnTextView);
        timeTakenTextView = findViewById(R.id.timeTakenTextViewBlowfishEncryption);

        // new Alert Loading Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setView(R.layout.loading_layout_dialog);
        loadingDialog = builder.create();

        encryptionSuccessFlag = false;

        successFileImportTextView.setVisibility(View.INVISIBLE);
        successFileEncryptedTextView.setVisibility(View.INVISIBLE);
        encryptedKeySrnnTextView.setVisibility(View.INVISIBLE);
        timeTakenTextView.setVisibility(View.INVISIBLE);

        chooseFileBtn.setOnClickListener(view -> fileChooser());
        startEncryptBtn.setOnClickListener(view -> {
            successFileEncryptedTextView.setVisibility(View.INVISIBLE);
            encryptedKeySrnnTextView.setVisibility(View.INVISIBLE);
            timeTakenTextView.setVisibility(View.INVISIBLE);
            loadingDialog.show();
            String keyToEncrypt = keyEditText.getText().toString().trim();
            if (keyToEncrypt.isEmpty()) {
                Toast.makeText(this, "Enter the key !!!", Toast.LENGTH_SHORT).show();
            } else {
                keyString = keyToEncrypt;
                startEncryptionBlowfish();
            }
        });
    }

    private void startEncryptionBlowfish() {
        startTime = System.currentTimeMillis();

        BigInteger n, u, p, q, ua, b;
        int j;
        p = new BigInteger("11");
        q = new BigInteger("7");
        n = p.multiply(q);
        u = new BigInteger("3");
        ua = u.pow(31);
        BigInteger[] ekey = new BigInteger[100];
        BigInteger[] enkey = new BigInteger[100];

        StringBuilder encryptedKeySrnn = new StringBuilder("{ ");
        try {
            encrypt();
            for (j = 0; j < keyString.length(); j++) {
                ekey[j] = BigInteger.valueOf((int) keyString.charAt(j));
            }
//            System.out.println("Your Encrypted key using SRNN is:");
            for (j = 0; j < keyString.length(); j++) {
                b = (ekey[j].multiply(ua)).pow(7);
                enkey[j] = b.mod(n);
                encryptedKeySrnn.append(enkey[j]);
                encryptedKeySrnn.append(" ");
            }
            finishTime = System.currentTimeMillis();
            timeTaken += finishTime - startTime;

            encryptedKeySrnn.append("}");
            encryptedKeySrnnTextView.setText(encryptedKeySrnn);
            encryptedKeySrnnTextView.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void encrypt() {
        doCrypto();
    }

    private void doCrypto() {
        if (inputFileBytes == null) {
            System.out.println("inputFileBytes are null");
            return;
        }
        long startThreadTime = System.currentTimeMillis();
        new Thread(() -> {
            try {
                Key secretKey = new SecretKeySpec(keyString.getBytes(), ALGORITHM);
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                outputBytes = cipher.doFinal(inputFileBytes);
                encryptionSuccessFlag = true;
            } catch (Exception e) {
                encryptionSuccessFlag = false;
                System.out.println("Error Exception raised line 152");
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                if (encryptionSuccessFlag) {
                    successFileEncryptedTextView.setVisibility(View.VISIBLE);
                    if (outputBytes == null) {
                        Toast.makeText(this, "Some Internal Error while encrypting the file!!!. Check logs for error", Toast.LENGTH_LONG).show();
                    } else {
                        timeTaken += System.currentTimeMillis() - startThreadTime;
                        String timeTakenStr = "Time Taken: ";
                        double timeTakenDouble = timeTaken / 1000.0;
                        timeTakenStr += timeTakenDouble;
                        timeTakenStr += " sec";
                        timeTakenTextView.setText(timeTakenStr);
                        timeTakenTextView.setVisibility(View.VISIBLE);
                        uploadEncryptedFileToAws();
                    }
                } else {
                    Toast.makeText(this, "Unable to Encrypt the file!!!. Check logs for error", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void fileChooser() {
//        inputFile = null;
        successFileImportTextView.setVisibility(View.INVISIBLE);
        Intent i = new Intent();
        i.setType("application/pdf");
        i.setAction(Intent.ACTION_GET_CONTENT);

        launchSomeActivity.launch(i);
    }

    ActivityResultLauncher<Intent> launchSomeActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    // your operation....
                    if (data != null && data.getData() != null) {
                        Uri fileUri = data.getData();
//                        InputStream iStream;
                        try {
                            inputFileStream = getContentResolver().openInputStream(fileUri);
                            inputFileBytes = getBytes(inputFileStream);
//                            String uriString = fileUri.toString();
//                            inputFile = new File(uriString);
                            System.out.println(fileUri.getPath());
                            System.out.println(fileUri);

                            successFileImportTextView.setVisibility(View.VISIBLE);
                        } catch (Exception e) {
                            Toast.makeText(this, "Error importing the file !!!", Toast.LENGTH_LONG).show();
                            e.printStackTrace();
                        }
                    }


                }
            });

    private void uploadEncryptedFileToAws() {
//        inputFileName = "namitFile";
        uploadToAwsSuccessful = true;
        new Thread(() -> {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                Connection connection = DriverManager.getConnection(AwsRdsData.url, AwsRdsData.username, AwsRdsData.password);

                PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO Blowfish" + "(encryptedFileBlob, algorithm) VALUES(?, ?)");
                preparedStatement.setBytes(1, outputBytes);
                preparedStatement.setString(2, ALGO_STR);
                preparedStatement.executeUpdate();
                connection.close();

            } catch (Exception e) {
                uploadToAwsSuccessful = false;
                e.printStackTrace();
            }
            runOnUiThread(() -> {
                // after the job is finished:
                if (!uploadToAwsSuccessful)
                    Toast.makeText(this, "Upload to AWS Failed !!!", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(this, "Upload to AWS Successful !!!", Toast.LENGTH_SHORT).show();
                loadingDialog.dismiss();
            });
        }).start();
    }

    // helper function for key file chooser
    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}