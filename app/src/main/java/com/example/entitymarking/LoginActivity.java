package com.example.entitymarking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;


public class LoginActivity extends AppCompatActivity {

    private static final int REQUEST_READ_PHONE_STATE = 101;
    TextInputEditText userIdEt, userPassWordEt;
    TextInputLayout userLayout, passwordLayout;
    Button loginBtn;
    SharedPreferences preferences;
    String userId = "", password = "";
    boolean isPass = true;
    DatabaseReference rootRef;
    TextView txtCity;
    String city,date,time,dt;
    String type;
    CommonFunctions common = new CommonFunctions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
//        FirebaseApp.initializeApp(this);
        setPageTitle();
        inIt();
    }

    private void inIt() {

        userIdEt = findViewById(R.id.username);
        userLayout = findViewById(R.id.userUsername);
        userPassWordEt = findViewById(R.id.password);
        passwordLayout = findViewById(R.id.userPassword);
        txtCity = findViewById(R.id.cityName);
        loginBtn = findViewById(R.id.login);
        rootRef = common.getDatabaseRef(LoginActivity.this);
        preferences = getSharedPreferences("LoginDetails", MODE_PRIVATE);
//        common.mFetchAlreadyInstalledCBHeading(LoginActivity.this);
        date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        dt = date+"/"+time;
        city = preferences.getString("storagePath", "");
        txtCity.setText("Marking " + preferences.getString("storagePath", ""));
//        boolean isFastConnection = Connectivity.isConnectedFast(LoginActivity.this);
//        Log.e("isFastConnection",""+isFastConnection);
        /*int permissionCheck = ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.ACCESS_WIFI_STATE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(LoginActivity.this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, REQUEST_READ_PHONE_STATE);
        } else {
            type = Connectivity.getConnectionStrength(LoginActivity.this);
            Log.e("type",type);
            if (type.equals("1")){
                common.showAlertBox("Poor Network connection !", "Ok", "", LoginActivity.this);
            }else if (type.equals("0")){
                common.showAlertBox("Network connection Not Available!", "Ok", "", LoginActivity.this);
            }else if (type.equals("3")){
                common.showAlertBox("Network connection Fair!", "Ok", "", LoginActivity.this);
            }else if (type.equals("4")){
                common.showAlertBox("Network connection Good!", "Ok", "", LoginActivity.this);
            }else if (type.equals("5")){
                common.showAlertBox("Network connection Strong!", "Ok", "", LoginActivity.this);
            }else if (type.equals("Not Connected")){
                common.showAlertBox("Please Connect to internet!", "Ok", "", LoginActivity.this);
            }
        }*/

    }



    private void setPageTitle() {
        findViewById(R.id.screen_title).setOnLongClickListener(view -> {
            preferences.edit().putString("dbPath", "https://dtdnavigatortesting.firebaseio.com/").apply();
//            preferences.edit().putString("dbPath", "https://dtdreengus.firebaseio.com/").apply();
            preferences.edit().putString("storagePath", "Test").apply();
//            preferences.edit().putString("storagePath", "Reengus").apply();
            rootRef = common.getDatabaseRef(LoginActivity.this);
            common.showAlertBox("Testing Mode Enabled", "Ok", "", LoginActivity.this);
            return false;
        });
    }

    @SuppressLint("StaticFieldLeak")
    public void onLoginClick(View view) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                common.setProgressDialog("Please wait...", " ", LoginActivity.this, LoginActivity.this);
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                int permissionCheck = ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.ACCESS_WIFI_STATE);
                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(LoginActivity.this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, REQUEST_READ_PHONE_STATE);
                } else {
                    return Connectivity.isNetworkAvailable(LoginActivity.this);
//                    Log.e("type",type);
//                    if (type.equals("1")){
//                        return false;
//                    }else if (type.equals("0")){
//                        return false;
////                        common.network(LoginActivity.this);
//                    }else if (type.equals("Not Connected")){
//                        return false;
//                    }
                }

                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    if (editTextValidation()) {
                        checkUserIdAndPassword();
                    }else {
                        common.closeDialog(LoginActivity.this);
                    }
                } else {
//                    common.showAlertBox("Please Connect to internet", "Ok", "", LoginActivity.this);
                    common.closeDialog(LoginActivity.this);
//                    if (type.equals("1")){
//                        common.showAlertBox("Poor Network connection !", "Ok", "", LoginActivity.this);
//                    }else if (type.equals("0")){
//                        common.showAlertBox("Network connection Not Available!", "Ok", "", LoginActivity.this);
//                    }else if (type.equals("Not Connected")){
                        common.showAlertBox("Please Connect to internet!", "Ok", "", LoginActivity.this);
//                    }
                }

            }
        }.execute();
    }


    private boolean editTextValidation() {

        userId = String.valueOf(userIdEt.getText()).trim();
        password = String.valueOf(userPassWordEt.getText()).trim();
        if (userId.length() > 0) {
            if (password.length() > 0) {
                return true;
            } else {
                common.showAlertBox("Please Enter password", "ok", "", this);
                return false;
            }
        } else {
            common.showAlertBox("Please Enter Username", "ok", "", this);
            return false;
        }
    }

    private void checkUserIdAndPassword() {
        if (isPass) {
            isPass = false;
            common.setProgressDialog("Please Wait...", "", LoginActivity.this, LoginActivity.this);
            rootRef.child("EntityMarkingData/MarkerAppAccess/").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.getValue() != null) {
                        try {
                            String userid = userId;
                            String assignWard = String.valueOf(snapshot.child("assignedWard").getValue());
//                            Toast.makeText(LoginActivity.this, "userid "+userid+" ward "+assignWard, Toast.LENGTH_SHORT).show();
                            if (snapshot.hasChild("assignedWard")) {
                                if (String.valueOf(snapshot.child("password").getValue()).equals(password)) {
//                                    Log.e("isAtive",snapshot.child("isActive").getValue().toString());
                                    if (Boolean.parseBoolean(String.valueOf(snapshot.child("isActive").getValue()))) {
                                        preferences.edit().putString("userId", userId).apply();
                                        preferences.edit().putString("assignment", String.valueOf(snapshot.child("assignedWard").getValue())).apply();
//                                        Toast.makeText(LoginActivity.this, "user active "+snapshot.child("isActive").getValue().toString(), Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(LoginActivity.this, MapActivity.class);
                                        common.closeDialog(LoginActivity.this);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        isPass = true;
                                        common.closeDialog(LoginActivity.this);
                                        common.showAlertBox("InActive user", "ok", "", LoginActivity.this);
                                    }
                                } else {
                                    isPass = true;
                                    common.closeDialog(LoginActivity.this);
                                    common.showAlertBox("Incorrect Password", "ok", "", LoginActivity.this);
                                }
                            } else {
                                isPass = true;
                                common.closeDialog(LoginActivity.this);
                                common.showAlertBox("No Work Assigned", "ok", "", LoginActivity.this);
                            }
                        } catch (Exception e) {
                            Log.e("exception e", e.getMessage());
                            errorLog(e);
                        }
                    } else {
                        isPass = true;
                        common.closeDialog(LoginActivity.this);
                        common.showAlertBox("Incorrect Username", "ok", "", LoginActivity.this);
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    common.closeDialog(LoginActivity.this);
                    Toast.makeText(LoginActivity.this, "Please Contact to Admin", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void errorLog(Exception e){
        Toast.makeText(LoginActivity.this, "Please Contact to Admin", Toast.LENGTH_SHORT).show();
        HashMap<String, Object> hM = new HashMap<>();
        hM.put("ErrorDes",e.getMessage());
        hM.put("AndroidVersion", Build.VERSION.RELEASE);
        rootRef.child("ErrorLogs/EntityMarking/"+city+"/LoginPage/" +dt).updateChildren(hM);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_READ_PHONE_STATE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //TODO
                }
                break;

            default:
                break;
        }
    }

}