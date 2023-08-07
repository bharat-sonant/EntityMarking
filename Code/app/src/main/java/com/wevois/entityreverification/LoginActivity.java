package com.wevois.entityreverification;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;


public class LoginActivity extends AppCompatActivity {

    private static final int REQUEST_READ_PHONE_STATE = 101;
    TextInputEditText userIdEt, userPassWordEt;
    TextInputLayout userLayout, passwordLayout;
    Button loginBtn;
    SharedPreferences preferences;
    String userId = "", password = "";
    boolean isPass = true;
    TextView txtCity;
    String city, date, time, dt;
    String type;
    String isActive = "1";
    boolean loginFlag;
    CommonFunctions common = new CommonFunctions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
//        setPageTitle();
        inIt();
    }

    private void inIt() {

        userIdEt = findViewById(R.id.username);
        userLayout = findViewById(R.id.userUsername);
        userPassWordEt = findViewById(R.id.password);
        passwordLayout = findViewById(R.id.userPassword);
        txtCity = findViewById(R.id.cityName);
        loginBtn = findViewById(R.id.login);
        preferences = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        date = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        dt = date + "/" + time;
        city = preferences.getString("storagePath", "");
        txtCity.setText("Marking Reverification ");
//        boolean isFastConnection = Connectivity.isConnectedFast(LoginActivity.this);
//        Log.e("isFastConnection",""+isFastConnection);
//        getSpecialUsers();

    }

    public void getSpecialUsers() {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        Log.e("storageReference", storageReference.toString());
        storageReference.child("Defaults/MobileUsers.json").getMetadata().addOnSuccessListener(storageMetadata -> {
            long fileCreationTime = storageMetadata.getCreationTimeMillis();
            long fileDownloadTime = preferences.getLong("SpecialUserDownloadTime", 0);
            if (fileDownloadTime != fileCreationTime) {
                storageReference.child("Defaults/MobileUsers.json").getBytes(10000000).addOnSuccessListener(taskSnapshot -> {
                    String specialUsersData = new String(taskSnapshot, StandardCharsets.UTF_8);
                    preferences.edit().putString("SpecialUsersList", specialUsersData).apply();
//                    preferences.edit().putLong("SpecialUserDownloadTime", fileCreationTime).apply();
                    doLogin();
                });
            }
        });
        storageReference.child("Defaults/MobileUsers.json").getMetadata().addOnFailureListener(e -> common.closeDialog(LoginActivity.this));
    }


    private void setPageTitle() {
        findViewById(R.id.screen_title).setOnLongClickListener(view -> {
//            preferences.edit().putString("dbPath", "https://malviyanagar.firebaseio.com/").apply();
//            preferences.edit().putString("dbPath", "https://dtdreengus.firebaseio.com/").apply();
            preferences.edit().putString("storagePath", "Malviyanagar").apply();
//            preferences.edit().putString("storagePath", "Reengus").apply();
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
                    type = Connectivity.getConnectionStrength(LoginActivity.this);
                    Log.e("type"," type "+ type);
                    if (type.equals("1")) {
                        return false;
                    } else if (type.equals("0")) {
                        return false;
//                        common.network(LoginActivity.this);
                    } else if (type.equals("Not Connected")) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {

                Log.e("Network connection"," available "+Connectivity.isNetworkAvailable(LoginActivity.this));
                if (Connectivity.isNetworkAvailable(LoginActivity.this)) {
                    if (editTextValidation()) {
//                        doLogin();
                        getSpecialUsers();
                    } else {
                        common.closeDialog(LoginActivity.this);
                    }
                } else {
                    common.showAlertBox("Please Connect to internet", "Ok", "", LoginActivity.this);
                    common.closeDialog(LoginActivity.this);
//                    if (type.equals("1")) {
//                        common.showAlertBox("Poor Network connection !", "Ok", "", LoginActivity.this);
//                    } else if (type.equals("0")) {
//                        common.showAlertBox("Network connection Not Available!", "Ok", "", LoginActivity.this);
//                    } else if (type.equals("Not Connected")) {
//                        common.showAlertBox("Please Connect to internet!", "Ok", "", LoginActivity.this);
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

    private void doLogin() {
        try {
//            Boolean isSpecialUser = false;
            common.setProgressDialog("Please Wait..", " ", this, this);
            JSONObject specialUsersData = new JSONObject(preferences.getString("SpecialUsersList", ""));
            Iterator<String> specialUsersKeys = specialUsersData.keys();
            while (specialUsersKeys.hasNext()) {
                String key = specialUsersKeys.next();
                try {
                    if (!key.equals("lastKey")) {
                        Log.e("key", key);
                        JSONObject values = specialUsersData.getJSONObject(key);
//                        Log.e("values", values.toString());
                        String name = values.getString("name");
                        String username = values.getString("userName");
                        String userpassword = values.getString("password");
                        if (values.has("isActive")) {
                            isActive = values.getString("isActive");
                        }
                        if (userId.equals(username)) {
                            loginFlag = true;
                            if (password.equals(userpassword)) {
                                if (isActive.equals("1")) {
                                    preferences.edit().putString("userId", userId).apply();
                                    preferences.edit().putString("userName", name).apply();
//                                preferences.edit().putString("assignment", "125-R1").apply();
                                    common.closeDialog(LoginActivity.this);
                                    startActivity(new Intent(LoginActivity.this, SelectWardActivity.class));
                                    finish();
                                    break;
                                } else {
                                    common.showAlertBox("आप Active User नहीं हैं कृपया Admin से संपर्क करें !", "OK", "", LoginActivity.this);
                                }
                            }

                        } else {
                            common.closeDialog(LoginActivity.this);
                            loginFlag = false;
//                            userPassWordEt.setError("Invalid user or password");
//                            userPassWordEt.requestFocus();
//                            break;
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (!loginFlag) {
                common.closeDialog(LoginActivity.this);
                userPassWordEt.setError("Invalid user or password");
                userPassWordEt.requestFocus();
//                Toast.makeText(this, "Invalid user or password", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

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