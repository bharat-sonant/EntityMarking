package com.example.entitymarking;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    SharedPreferences dbPathSP;
    String userId, assignedWard;
    DatabaseReference rootRef;
    String date,time,dt,city;
    CommonFunctions cmn = new CommonFunctions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        dbPathSP = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        dt = date+"/"+time;
        checkAlreadyLoggedIn();
//        mapIntent();
    }

    private void checkAlreadyLoggedIn() {
        userId = dbPathSP.getString("userId", null);
        city = dbPathSP.getString("storagePath", "");
        if (userId != null) {
            if (!userId.equals("0")) {
                checkInternet();
                return;
            }
        }
        setDatabase("Test");
    }

    @SuppressLint("StaticFieldLeak")
    private void checkInternet() {

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                return cmn.network(MainActivity.this);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    String ward  = dbPathSP.getString("ward","");
                    mapIntent();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Please Connect to internet").setCancelable(false)
                            .setPositiveButton("Ok", (dialog, id) -> {
                                Toast.makeText(MainActivity.this, "Turn On Internet", Toast.LENGTH_SHORT).show();
                                finish();
                                dialog.cancel();
                            })
                            .setNegativeButton("", (dialog, i) -> {
                                dialog.cancel();
                                finish();
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            }
        }.execute();
    }

    void setDatabase(String city) {
        String path = cmn.getDatabase(city);
        dbPathSP.edit().putString("dbPath", path).apply();
        dbPathSP.edit().putString("storagePath", city).apply();
        dbPathSP.edit().putString("prefix", "MNZ").apply();
        loginIntent();
    }

    private void checkAssignedWard() {
        try {
            rootRef.child("EntityMarkingData/MarkerAppAccess/" + userId + "/assignedWard/")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.getValue() != null) {
                                assignedWard = dbPathSP.getString("assignment", "null");
                                if (assignedWard != null) {
                                    if (!assignedWard.equals(String.valueOf(snapshot.getValue()))) {
                                        dbPathSP.edit().putString("assignment", String.valueOf(snapshot.getValue())).apply();
                                    }
                                    mapIntent();
                                }
                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setMessage("No work assigned").setCancelable(false)
                                        .setPositiveButton("ok", (dialog, id) -> {
                                            finish();
                                            dialog.cancel();
                                        })
                                        .setNegativeButton("", (dialog, i) -> finish());
                                AlertDialog alertDialog = builder.create();
                                alertDialog.show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        } catch (Exception e) {
            errorLog(e);
        }

    }

    private void errorLog(Exception e){

        Toast.makeText(MainActivity.this, "Please Contact to Admin", Toast.LENGTH_SHORT).show();
        HashMap<String, Object> hM = new HashMap<>();
        hM.put("ErrorDes",e.getMessage());
        hM.put("AndroidVersion", Build.VERSION.RELEASE);
        rootRef.child("ErrorLogs/EntityMarking/"+city+"/MainActivity/" +dt).updateChildren(hM);
    }
    private void loginIntent() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void mapIntent() {
        Intent intent = new Intent(MainActivity.this, MapActivity.class);
        startActivity(intent);
        finish();
    }

//    private void wardIntent() {
//        Intent intent = new Intent(MainActivity.this, SelectWardActivity.class);
//        startActivity(intent);
//        finish();
//    }
}