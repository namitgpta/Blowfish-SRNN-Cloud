package com.namit.pw.blowfishsrnncloud;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.namit.pw.blowfishsrnncloud.adapter.AwsCloudDataRecyclerViewAdapter;
import com.namit.pw.blowfishsrnncloud.adapter.SampleRecycler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class AwsCloudDataActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AwsCloudDataRecyclerViewAdapter recyclerViewAdapter;
    private ProgressBar progressBar;

    ArrayList<String> timestampsArray, algorithmArray;
    ArrayList<Integer> idArray;
    ArrayList<byte[]> EncryptedFileBytesArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aws_cloud_data);

        timestampsArray = new ArrayList<>();
        idArray = new ArrayList<>();
        EncryptedFileBytesArray = new ArrayList<>();
        algorithmArray = new ArrayList<>();

        progressBar = findViewById(R.id.progressBarAwsCloudData);
        progressBar.setVisibility(View.VISIBLE);

        recyclerView = findViewById(R.id.recyclerView_awsCloudData);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setting adapter to sample Recycler for avoiding unnecessary errors in log
        recyclerView.setAdapter(new SampleRecycler());
        try {
            utilFun();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void utilFun() throws SQLException {
        new Thread(() -> {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                Connection connection = DriverManager.getConnection(AwsRdsData.url, AwsRdsData.username, AwsRdsData.password);
                Statement statement = connection.createStatement();

                ResultSet rs = statement.executeQuery("SELECT * FROM Blowfish" + " ORDER BY _id DESC");
                while (rs.next()) {
                    idArray.add(rs.getInt("_id"));
                    EncryptedFileBytesArray.add(rs.getBytes("encryptedFileBlob"));
                    timestampsArray.add(rs.getString("dateTimeStamp"));
                    algorithmArray.add(rs.getString("algorithm"));
                }
                connection.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                // after the job is finished:

                if (algorithmArray.isEmpty()) {
                    Toast.makeText(this, "Failed to fetch data from AWS Cloud !!!", Toast.LENGTH_LONG).show();
                } else {
                    // setup adapter
                    recyclerViewAdapter = new AwsCloudDataRecyclerViewAdapter(AwsCloudDataActivity.this, timestampsArray, idArray, EncryptedFileBytesArray, algorithmArray);
                    recyclerView.setAdapter(recyclerViewAdapter);
                }
                progressBar.setVisibility(View.INVISIBLE);
            });
        }).start();
    }
}