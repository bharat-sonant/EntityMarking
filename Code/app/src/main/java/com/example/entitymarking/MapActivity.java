package com.example.entitymarking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    String selectedWard = null, selectedCity, userId, date, cbText;
    int currentLineNumber = 0;                      // use + 1 to get currentLine;
    Spinner houseTypeSpinner;
    TextView currentLineTv, totalMarksTv, titleTv, rgHeadingTv;
    RadioButton isSurveyedTrue, isSurveyedFalse;
    Bitmap photo;
    GoogleMap mMap;
    LocationCallback locationCallback;
    LatLng lastKnownLatLngForWalkingMan = null, latLngToSave = null;
    DatabaseReference rootRef;
    SharedPreferences preferences;
    List<List<LatLng>> dbColl = new ArrayList<>();
    HashMap<String, Integer> houseDataHashMap;
    CommonFunctions common = new CommonFunctions();
    boolean isPass = true, captureClickControl = true, boolToInstantiateMovingMarker = true;
    CountDownTimer cdTimer;
    private Camera mCamera;
    private SurfaceView surfaceView;
    Camera.PictureCallback pictureCallback;
    ChildEventListener cELOnLine;
    ValueEventListener cELForAssignedWard;
    private static final int MAIN_LOC_REQUEST = 5000, GPS_CODE_FOR_ENTITY = 501, FOCUS_AREA_SIZE = 300, PERMISSION_CODE = 1000;

    private static final String TAG = MapActivity.class.getSimpleName();

    @SuppressLint("SimpleDateFormat")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_map);
            inIt();
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint({"ResourceType", "SimpleDateFormat"})
    private void inIt() {
        currentLineTv = findViewById(R.id.current_line_tv);
        rootRef = common.getDatabaseRef(this);
        houseTypeSpinner = findViewById(R.id.house_type_spinner);
        totalMarksTv = findViewById(R.id.total_marks_tv);
        rgHeadingTv = findViewById(R.id.radio_group_heading_tv);
        isSurveyedTrue = findViewById(R.id.is_surveyed_true_rb);
        isSurveyedFalse = findViewById(R.id.is_surveyed_false_rb);
        date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        preferences = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        selectedWard = preferences.getString("assignment", null);
        selectedCity = preferences.getString("storagePath", "");
        userId = preferences.getString("userId", "");
        cbText = preferences.getString("alreadyInstalledCheckBoxText", getResources().getString(R.string.already_installed_cb_text));
        rgHeadingTv.setText(cbText);
        setRB();
        if (selectedWard != null) {
            common.setProgressDialog("Please Wait", "", MapActivity.this, MapActivity.this);
            setPageTitle();
            runOnUiThread(this::fetchHouseTypesAndSetSpinner);
            fetchWardJson();
            assignedWardCEL();
        }
    }

    private void setRB() {
        isSurveyedFalse.setOnClickListener(view -> {
            isSurveyedFalse.setChecked(true);
            isSurveyedTrue.setChecked(false);
        });
        isSurveyedTrue.setOnClickListener(view -> {
            isSurveyedFalse.setChecked(false);
            isSurveyedTrue.setChecked(true);
        });
    }

    private void setBothRBUnchecked() {
        isSurveyedFalse.setChecked(false);
        isSurveyedTrue.setChecked(false);
    }

    private boolean checkWhichRBisChecked() {
        return isSurveyedTrue.isChecked();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mainCheckLocationForRealTimeRequest();
    }

    @SuppressLint("SetTextI18n")
    private void setPageTitle() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        titleTv = toolbar.findViewById(R.id.toolbar_title);
        titleTv.setText("Ward: " + selectedWard);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        toolbar.setNavigationOnClickListener(v -> MapActivity.this.onBackPressed());
    }

    private void fetchHouseTypesAndSetSpinner() {
        rootRef.child("Defaults/FinalHousesType").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    List<String> houseList = new ArrayList<>();
                    houseDataHashMap = new HashMap<>();
                    houseList.add("Select House Type");
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        if (dataSnapshot.hasChild("name")) {
                            String[] tempStr = String.valueOf(dataSnapshot.child("name").getValue()).split("\\(");
                            houseDataHashMap.put(String.valueOf(tempStr[0]), Integer.parseInt(Objects.requireNonNull(dataSnapshot.getKey())));
                            houseList.add(tempStr[0]);
                        }
                    }
                    ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(MapActivity.this, android.R.layout.simple_spinner_item, houseList) {
                        @Override
                        public boolean isEnabled(int position) {
                            return !(position == 0);
                        }

                        @Override
                        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                            View view = super.getDropDownView(position, convertView, parent);
                            TextView tv = (TextView) view;
                            tv.setTextColor(position == 0 ? Color.GRAY : Color.BLACK);
                            return view;
                        }

                    };
                    spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    houseTypeSpinner.setAdapter(spinnerArrayAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        houseTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (houseTypeSpinner.getSelectedItemId() != 0) {
                    onSaveClick(view);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void fetchWardJson() {
        try {
            File localFile = File.createTempFile("images", "jpg");
            FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + selectedCity + "/WardJson")
                    .child(selectedWard + ".json").getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(localFile)));
                    StringBuilder sb = new StringBuilder();
                    String str;
                    while ((str = br.readLine()) != null) {
                        sb.append(str);
                    }
                    prepareDB(new JSONObject(sb.toString()));

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }).addOnFailureListener(e -> {
                common.closeDialog(this);
                AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                builder.setMessage("No Data Found").setCancelable(false)
                        .setPositiveButton("Ok", (dialog, id) -> {
                            MapActivity.this.onBackPressed();
                            dialog.cancel();
                        })
                        .setNegativeButton("", (dialog, i) -> {
                            MapActivity.this.onBackPressed();
                            dialog.cancel();
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void prepareDB(JSONObject wardJSONObject) {
        Iterator<String> keys = wardJSONObject.keys();
        dbColl = new ArrayList<>();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                if (wardJSONObject.get(key) instanceof JSONObject) {
                    List<LatLng> tempList = new ArrayList<>();
                    JSONArray latLngPointJSONArray = wardJSONObject.getJSONObject(key).getJSONArray("points");
                    for (int a = 0; a < latLngPointJSONArray.length(); a++) {
                        try {
                            double lat = latLngPointJSONArray.getJSONArray(a).getDouble(0);
                            double lng = latLngPointJSONArray.getJSONArray(a).getDouble(1);
                            tempList.add(new LatLng(lat, lng));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    dbColl.add(tempList);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        celForLine();
        drawLine();
        fetchMarkerForLine(false);
        common.closeDialog(this);
    }

    private void celForLine() {
        cELOnLine = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.getValue() != null) {
                    if (Objects.equals(snapshot.getKey(), "marksCount")) {
                        totalMarksTv.setText(String.valueOf(snapshot.getValue()));
                    }
                    if (snapshot.hasChild("latLng")) {
                        String[] tempStr = String.valueOf(snapshot.child("latLng").getValue()).split(",");
                        double lat = Double.parseDouble(tempStr[0]);
                        double lng = Double.parseDouble(tempStr[1]);
                        addMarkerForEntity(new LatLng(lat, lng));
                    }
                }

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (Objects.equals(snapshot.getKey(), "marksCount")) {
                    totalMarksTv.setText(String.valueOf(snapshot.getValue()));
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
    }

    @SuppressLint("SetTextI18n")
    private void fetchMarkerForLine(boolean isCloseProgressDialog) {
        totalMarksTv.setText("" + 0);
        rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1)).addChildEventListener(cELOnLine);
        if (isCloseProgressDialog) {
            if (cELOnLine != null) {
                rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber)).removeEventListener(cELOnLine);
            }
            common.closeDialog(MapActivity.this);
        }
    }

    private void mainCheckLocationForRealTimeRequest() {
        if (common.locationPermission(MapActivity.this)) {
            LocationServices.getSettingsClient(this).checkLocationSettings(new LocationSettingsRequest.Builder()
                    .addLocationRequest(new LocationRequest().setInterval(5000).setFastestInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY))
                    .setAlwaysShow(true).setNeedBle(true).build()).addOnCompleteListener(task1 -> {
                try {
                    task1.getResult(ApiException.class);
                    if (task1.isSuccessful()) {
                        startCaptureLocForWalkingMan();
                    }
                } catch (ApiException e) {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(MapActivity.this, MAIN_LOC_REQUEST);
                        } catch (IntentSender.SendIntentException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    private void checkGpsForEntity() {
        LocationServices.getSettingsClient(this).checkLocationSettings(new LocationSettingsRequest.Builder()
                .addLocationRequest(new LocationRequest().setInterval(5000).setFastestInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY))
                .setAlwaysShow(true).setNeedBle(true).build()).addOnCompleteListener(task1 -> {
            try {
                task1.getResult(ApiException.class);
                if (task1.isSuccessful()) {
                    openCam();
                }
            } catch (ApiException e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MapActivity.this, GPS_CODE_FOR_ENTITY);
                    } catch (IntentSender.SendIntentException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    private void pickLocForEntity() {
        common.closeDialog(MapActivity.this);
        if (lastKnownLatLngForWalkingMan != null) {
            latLngToSave = lastKnownLatLngForWalkingMan;
            updateMarksCount();
        } else {
            common.showAlertBox("Please Refresh Location", "Ok", "", MapActivity.this);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void updateMarksCount() {
        View dialogLayout = MapActivity.this.getLayoutInflater().inflate(R.layout.custom_image_preview, null);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this).setView(dialogLayout).setCancelable(false);
        AlertDialog dialog = alertDialog.create();

        ImageView imageView = dialogLayout.findViewById(R.id.image_preview);
        imageView.setImageBitmap(photo);

        Button btn = dialogLayout.findViewById(R.id.proceed_preview_image_btn);
        btn.setOnClickListener(v -> {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    common.setProgressDialog("", "Saving", MapActivity.this, MapActivity.this);
                }

                @Override
                protected Boolean doInBackground(Void... p) {
                    return common.network(MapActivity.this);
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    common.closeDialog(MapActivity.this);
                    if (result) {
                        try {
                            saveMarkedLocationAndUploadPhoto();
                        } catch (Exception e) {
                            Log.d(TAG, "onPostExecute: " + e.toString());
                        }

                    } else {
                        isPass = true;
                        houseTypeSpinner.setSelection(0);
                        common.showAlertBox("Please Connect to internet", "Ok", "", MapActivity.this);
                    }
                }
            }.execute();
            dialog.dismiss();

        });
        Button closeBtn = dialogLayout.findViewById(R.id.close_preview_image_btn);
        closeBtn.setOnClickListener(v -> {
            houseTypeSpinner.setSelection(0);
            setBothRBUnchecked();
            dialog.dismiss();
        });
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        captureClickControl = true;
        houseTypeSpinner.setEnabled(true);
    }

    @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
    private void saveMarkedLocationAndUploadPhoto() {

        rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1)).child("marksCount")
                .runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        if (currentData.getValue() == null) {
                            currentData.setValue(1);
                        } else {
                            currentData.setValue((Long) currentData.getValue() + 1);
                        }
                        return Transaction.success(currentData);
                    }

                    @Override
                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                        if (error == null) {
                            assert currentData != null;
                            int MARKS_COUNT = Integer.parseInt(String.valueOf(currentData.getValue()));

                            runOnUiThread(() -> {
                                HashMap<String, Object> hM = new HashMap<>();
                                hM.put("latLng", latLngToSave.latitude + "," + latLngToSave.longitude);
                                hM.put("userId", userId);
                                hM.put("alreadyInstalled", checkWhichRBisChecked());
                                hM.put("image", MARKS_COUNT + ".jpg");
                                hM.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
                                hM.put("houseType", houseDataHashMap.get(houseTypeSpinner.getSelectedItem()));

                                rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1) + "/" + MARKS_COUNT).setValue(hM)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/DateWise/" + date + "/" + userId)
                                                        .runTransaction(new Transaction.Handler() {
                                                            @NonNull
                                                            @Override
                                                            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                                                if (currentData.getValue() == null) {
                                                                    currentData.setValue(1);
                                                                } else {
                                                                    currentData.setValue((Long) currentData.getValue() + 1);
                                                                }
                                                                return Transaction.success(currentData);
                                                            }

                                                            @Override
                                                            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {

                                                            }
                                                        });

                                                rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/DateWise/" + date + "/totalCount")
                                                        .runTransaction(new Transaction.Handler() {
                                                            @NonNull
                                                            @Override
                                                            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                                                if (currentData.getValue() == null) {
                                                                    currentData.setValue(1);
                                                                } else {
                                                                    currentData.setValue((Long) currentData.getValue() + 1);
                                                                }
                                                                return Transaction.success(currentData);
                                                            }

                                                            @Override
                                                            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                                                            }
                                                        });

                                                rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/EmployeeWise/" + userId + "/" + selectedWard)
                                                        .runTransaction(new Transaction.Handler() {
                                                            @NonNull
                                                            @Override
                                                            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                                                if (currentData.getValue() == null) {
                                                                    currentData.setValue(1);
                                                                } else {
                                                                    currentData.setValue((Long) currentData.getValue() + 1);
                                                                }
                                                                return Transaction.success(currentData);
                                                            }

                                                            @Override
                                                            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                                                            }
                                                        });

                                                rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/EmployeeWise/" + userId + "/total")
                                                        .runTransaction(new Transaction.Handler() {
                                                            @NonNull
                                                            @Override
                                                            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                                                if (currentData.getValue() == null) {
                                                                    currentData.setValue(1);
                                                                } else {
                                                                    currentData.setValue((Long) currentData.getValue() + 1);
                                                                }
                                                                return Transaction.success(currentData);
                                                            }

                                                            @Override
                                                            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                                                            }
                                                        });

                                                rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/DateWise/" + date + "/" + selectedWard)
                                                        .runTransaction(new Transaction.Handler() {
                                                            @NonNull
                                                            @Override
                                                            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                                                if (currentData.getValue() == null) {
                                                                    currentData.setValue(1);
                                                                } else {
                                                                    currentData.setValue((Long) currentData.getValue() + 1);
                                                                }
                                                                return Transaction.success(currentData);
                                                            }

                                                            @Override
                                                            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                                                            }
                                                        });

                                                rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/DateWise/" + date + "/totalCount")
                                                        .runTransaction(new Transaction.Handler() {
                                                            @NonNull
                                                            @Override
                                                            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                                                if (currentData.getValue() == null) {
                                                                    currentData.setValue(1);
                                                                } else {
                                                                    currentData.setValue((Long) currentData.getValue() + 1);
                                                                }
                                                                return Transaction.success(currentData);
                                                            }

                                                            @Override
                                                            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                                                            }
                                                        });

                                                rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/WardWise/" + selectedWard + "/total")
                                                        .runTransaction(new Transaction.Handler() {
                                                            @NonNull
                                                            @Override
                                                            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                                                if (currentData.getValue() == null) {
                                                                    currentData.setValue(1);
                                                                } else {
                                                                    currentData.setValue((Long) currentData.getValue() + 1);
                                                                }
                                                                return Transaction.success(currentData);
                                                            }

                                                            @Override
                                                            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                                                            }
                                                        });

                                                if (checkWhichRBisChecked()) {
                                                    rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/WardWise/" + selectedWard + "/alreadyInstalled")
                                                            .runTransaction(new Transaction.Handler() {
                                                                @NonNull
                                                                @Override
                                                                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                                                    if (currentData.getValue() == null) {
                                                                        currentData.setValue(1);
                                                                    } else {
                                                                        currentData.setValue((Long) currentData.getValue() + 1);
                                                                    }
                                                                    return Transaction.success(currentData);
                                                                }

                                                                @Override
                                                                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                                                                }
                                                            });
                                                }

                                                houseTypeSpinner.setSelection(0);
                                                setBothRBUnchecked();


                                            } else {
                                                common.closeDialog(MapActivity.this);
                                                common.showAlertBox("Please Retry", "Ok", "", MapActivity.this);
                                            }
                                        });
                            });

                            ByteArrayOutputStream toUpload = new ByteArrayOutputStream();
                            photo.compress(Bitmap.CompressFormat.JPEG, 80, toUpload);
                            FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + selectedCity)
                                    .child("/MarkingSurveyImages/" + selectedWard + "/" + (currentLineNumber + 1) + "/" + MARKS_COUNT + ".jpg")
                                    .putBytes(toUpload.toByteArray())
                                    .addOnSuccessListener((UploadTask.TaskSnapshot taskSnapshot) -> {
                                        if (taskSnapshot.getTask().isSuccessful()) {
                                            common.closeDialog(MapActivity.this);
                                        }
                                    }).addOnFailureListener(e -> common.closeDialog(MapActivity.this));

                        } else {
                            houseTypeSpinner.setSelection(0);
                            common.closeDialog(MapActivity.this);
                            common.showAlertBox("Please Connect to internet", "Ok", "", MapActivity.this);
                        }
                    }
                });
    }

    private void startCaptureLocForWalkingMan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult.getLocations().size() > 0) {
                    int latestLocationIndex = locationResult.getLocations().size() - 1;

                    lastKnownLatLngForWalkingMan = new LatLng(locationResult.getLocations().get(latestLocationIndex).getLatitude(),
                            locationResult.getLocations().get(latestLocationIndex).getLongitude());

                    if (boolToInstantiateMovingMarker) {
                        boolToInstantiateMovingMarker = false;
                        common.setMovingMarker(mMap, lastKnownLatLngForWalkingMan, MapActivity.this);
                    }

                    if (cdTimer == null) {
                        timerForWalkingMan();
                    }

                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    builder.include(lastKnownLatLngForWalkingMan);
                    if (dbColl.size() > 0) {
                        for (LatLng ll : dbColl.get(currentLineNumber)) {
                            builder.include(ll);
                        }
                    }
                    LatLngBounds bounds = builder.build();
                    int padding = 200;
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                    mMap.animateCamera(cu);
                }
            }
        };
        LocationRequest locationRequest = new LocationRequest().setInterval(1000).setFastestInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void timerForWalkingMan() {
        cdTimer = new CountDownTimer(2000, 1000) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                common.currentLocationShow(lastKnownLatLngForWalkingMan);
                timerForWalkingMan();
            }
        }.start();
    }

    @SuppressLint("SetTextI18n")
    private void drawLine() {
        mMap.clear();
        boolToInstantiateMovingMarker = true;
        currentLineTv.setText("" + (currentLineNumber + 1) + " / " + dbColl.size());
        drawAllLine();
        mMap.addPolyline(new PolylineOptions().addAll(dbColl.get(currentLineNumber))
                .startCap(new RoundCap())
                .endCap(new RoundCap())
                .color(0xff000000)
                .jointType(JointType.ROUND)
                .width(8));


    }

    private void drawAllLine() {
        for (int i = 0; i < dbColl.size(); i++) {
            if (currentLineNumber != i) {
                mMap.addPolyline(new PolylineOptions().addAll(dbColl.get(i))
                        .startCap(new RoundCap())
                        .endCap(new RoundCap())
                        .color(Color.parseColor("#5abcff"))
                        .jointType(JointType.ROUND)
                        .width(8));
            }
        }
    }

    private void openCam() {
        new Handler().post(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (MapActivity.this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_CODE);
                } else {
                    showAlertDialog();
                }
            }
        });
    }

    private void focusOnTouch(MotionEvent event) throws Exception {
        if (mCamera != null) {
            try {

                Camera.Parameters parameters = mCamera.getParameters();
                if (parameters.getMaxNumMeteringAreas() > 0) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    Rect rect = calculateFocusArea(event.getX(), event.getY());
                    List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();
                    meteringAreas.add(new Camera.Area(rect, 800));
                    parameters.setFocusAreas(meteringAreas);
                    mCamera.setParameters(parameters);
                }
                mCamera.autoFocus((success, camera) -> {

                });

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private Rect calculateFocusArea(float x, float y) {
        int left = clamp(Float.valueOf((x / surfaceView.getWidth()) * 2000 - 1000).intValue());
        int top = clamp(Float.valueOf((y / surfaceView.getHeight()) * 2000 - 1000).intValue());

        return new Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }

    private int clamp(int touchCoordinateInCameraReper) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper) + MapActivity.FOCUS_AREA_SIZE / 2 > 1000) {
            if (touchCoordinateInCameraReper > 0) {
                result = 1000 - MapActivity.FOCUS_AREA_SIZE / 2;
            } else {
                result = -1000 + MapActivity.FOCUS_AREA_SIZE / 2;
            }
        } else {
            result = touchCoordinateInCameraReper - MapActivity.FOCUS_AREA_SIZE / 2;
        }
        return result;
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void showAlertDialog() {
        View dialogLayout = MapActivity.this.getLayoutInflater().inflate(R.layout.custom_camera_alertbox, null);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this).setView(dialogLayout).setCancelable(false);
        AlertDialog dialog = alertDialog.create();
        surfaceView = (SurfaceView) dialogLayout.findViewById(R.id.surfaceViews);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_HARDWARE);
        SurfaceHolder.Callback surfaceViewCallBack = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                try {
                    mCamera = Camera.open();
                    Camera.Parameters parameters = mCamera.getParameters();
                    List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
                    parameters.setPictureSize(sizes.get(0).width, sizes.get(0).height);
                    mCamera.setParameters(parameters);
                    setCameraDisplayOrientation(MapActivity.this, 0, mCamera);
                    mCamera.setPreviewDisplay(surfaceHolder);
                    mCamera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        };
        surfaceHolder.addCallback(surfaceViewCallBack);

        try {
            Button btn = dialogLayout.findViewById(R.id.capture_image_btn);
            btn.setOnClickListener(v -> {
                common.setProgressDialog("", "Please Wait", MapActivity.this, MapActivity.this);
                isPass = true;
                houseTypeSpinner.setEnabled(false);
                if (captureClickControl) {
                    captureClickControl = false;
                    mCamera.takePicture(null, null, null, pictureCallback);
                }
            });
            Button closeBtn = dialogLayout.findViewById(R.id.close_image_btn);
            closeBtn.setOnClickListener(v -> {
                houseTypeSpinner.setSelection(0);
                setBothRBUnchecked();
                dialog.cancel();
                isPass = true;
            });
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();

            pictureCallback = (bytes, camera) -> {
                Matrix matrix = new Matrix();
                matrix.postRotate(90F);
                photo = Bitmap.createBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length),
                        0, 0, BitmapFactory.decodeByteArray(bytes, 0, bytes.length).getWidth(),
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.length).getHeight(), matrix, true);
                if (photo != null) {
                    pickLocForEntity();
                } else {
                    common.closeDialog(MapActivity.this);
                    Toast.makeText(this, "Please Retry", Toast.LENGTH_SHORT).show();
                }

                camera.stopPreview();
                if (camera != null) {
                    camera.release();
                    mCamera = null;
                }
                dialog.cancel();
            };

            surfaceView.setOnTouchListener((view, motionEvent) -> {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    try {
                        focusOnTouch(motionEvent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void addMarkerForEntity(LatLng latLng) {
        mMap.addMarker(new MarkerOptions().position(latLng).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.gharicon)));
    }

    @SuppressLint("StaticFieldLeak")
    public void onNextClick(View view) {
        if ((currentLineNumber + 1) >= 0 && (currentLineNumber + 1) < dbColl.size()) {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    common.setProgressDialog("Please wait...", " ", MapActivity.this, MapActivity.this);
                }

                @Override
                protected Boolean doInBackground(Void... p) {
                    return common.network(MapActivity.this);
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    if (result) {
                        currentLineNumber++;
                        drawLine();
                        fetchMarkerForLine(true);
                    } else {
                        common.closeDialog(MapActivity.this);
                        common.showAlertBox("Please Connect to internet", "Ok", "", MapActivity.this);
                    }
                }
            }.execute();
        } else {
            common.showAlertBox("Line does not exist", "Ok", "", MapActivity.this);
        }
    }

    public void onSaveClick(View view) {
        if (isPass) {
            isPass = false;
            if (isSurveyedTrue.isChecked() || isSurveyedFalse.isChecked()) {
                checkGpsForEntity();
            } else {
                isPass = true;
                setBothRBUnchecked();
                houseTypeSpinner.setSelection(0);
                common.showAlertBox("Please Select yes or no option", "ok", "", MapActivity.this);
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void onPrevClick(View view) {
        if ((currentLineNumber - 1) >= 0 && (currentLineNumber - 1) < dbColl.size()) {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    common.setProgressDialog("Please wait...", " ", MapActivity.this, MapActivity.this);
                }

                @Override
                protected Boolean doInBackground(Void... p) {
                    return common.network(MapActivity.this);
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    if (result) {
                        currentLineNumber--;
                        drawLine();
                        fetchMarkerForLine(true);
                    } else {
                        common.closeDialog(MapActivity.this);
                        common.showAlertBox("Please Connect to internet", "Ok", "", MapActivity.this);
                    }
                }
            }.execute();
        } else {
            common.showAlertBox("Line does not exist", "Ok", "", MapActivity.this);
        }
    }

    @Override
    protected void onPause() {
        try {
            super.onPause();
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListeners();
    }

    private void removeListeners() {
        if (locationCallback != null) {
            LocationServices.getFusedLocationProviderClient(MapActivity.this).removeLocationUpdates(locationCallback);
        }
        if (cELOnLine != null) {
            rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1)).removeEventListener(cELOnLine);
        }
        if (cELForAssignedWard != null) {
            rootRef.child("EntityMarkingData/MarkerAppAccess/" + userId + "/assignedWard").removeEventListener(cELForAssignedWard);
        }
    }

    @Override
    @SuppressLint("StaticFieldLeak")
    public void onBackPressed() {
        super.onBackPressed();
        removeListeners();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.logOut) {
            preferences.edit().clear().apply();
            startActivity(new Intent(MapActivity.this, MainActivity.class));
            finish();
        } else if (item.getItemId() == R.id.move_to_line) {
            dialogForMoveToLine();
        }
        return super.onOptionsItemSelected(item);

    }

    @SuppressLint("StaticFieldLeak")
    private void dialogForMoveToLine() {
        try {
            View dialogLayout = MapActivity.this.getLayoutInflater().inflate(R.layout.move_to_line_view, null);
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this).setView(dialogLayout).setCancelable(false);
            AlertDialog dialog = alertDialog.create();
            EditText lineNumberEt = dialogLayout.findViewById(R.id.move_to_line_et);
            Button btn = dialogLayout.findViewById(R.id.move_to_line_confirm);
            btn.setOnClickListener(v -> {
                try {
                    if (lineNumberEt != null && lineNumberEt.getText().toString().trim().length() > 0) {
                        int lineNumber = Integer.parseInt(lineNumberEt.getText().toString());
                        if ((currentLineNumber + 1) != lineNumber) {
                            if ((lineNumber - 1) >= 0 && lineNumber <= dbColl.size()) {
                                new AsyncTask<Void, Void, Boolean>() {
                                    @Override
                                    protected void onPreExecute() {
                                        super.onPreExecute();
                                        common.setProgressDialog("Please wait...", " ", MapActivity.this, MapActivity.this);
                                    }

                                    @Override
                                    protected Boolean doInBackground(Void... p) {
                                        return common.network(MapActivity.this);
                                    }

                                    @Override
                                    protected void onPostExecute(Boolean result) {
                                        if (result) {
                                            dialog.cancel();
                                            currentLineNumber = lineNumber - 1;
                                            drawLine();
                                            fetchMarkerForLine(true);
                                        } else {
                                            common.closeDialog(MapActivity.this);
                                            common.showAlertBox("Please Connect to internet", "Ok", "", MapActivity.this);
                                        }
                                    }
                                }.execute();
                            } else {
                                common.showAlertBox("Please Enter a valid line number", "Ok", "", MapActivity.this);
                                lineNumberEt.setText("");
                            }
                        } else {
                            common.showAlertBox("Already on same line", "Ok", "", MapActivity.this);
                            lineNumberEt.setText("");
                        }
                    } else {
                        common.showAlertBox("Please Enter a valid line number", "Ok", "", MapActivity.this);
                        assert lineNumberEt != null;
                        lineNumberEt.setText("");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    common.showAlertBox("Please Enter a valid line number", "Ok", "", MapActivity.this);
                    assert lineNumberEt != null;
                    lineNumberEt.setText("");
                }
            });
            Button closeBtn = dialogLayout.findViewById(R.id.move_to_line_cancel);
            closeBtn.setOnClickListener(v -> dialog.cancel());
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        try {
            if (requestCode == 500 && grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCaptureLocForWalkingMan();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }

            if (requestCode == PERMISSION_CODE) {
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    showAlertDialog();
                } else {
                    houseTypeSpinner.setSelection(0);
                    setBothRBUnchecked();
                    isPass = true;
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == GPS_CODE_FOR_ENTITY) {
                if (resultCode == RESULT_OK) {
                    openCam();
                } else {
                    isPass = true;
                    houseTypeSpinner.setSelection(0);
                    setBothRBUnchecked();
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == MAIN_LOC_REQUEST) {
                if (resultCode == RESULT_OK) {
                    startCaptureLocForWalkingMan();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    mainCheckLocationForRealTimeRequest();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    @SuppressLint({"CommitPrefEdits", "SetTextI18n"})
    private void assignedWardCEL() {
        cELForAssignedWard = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!String.valueOf(snapshot.getValue()).equals(selectedWard)) {
                    try {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                        builder.setMessage("आपका असाइन किया गया वार्ड बदल गया है").setCancelable(false)
                                .setPositiveButton("Ok", (dialog, id) -> {
                                    mMap.clear();
                                    preferences.edit().putString("assignment", String.valueOf(snapshot.getValue())).apply();
                                    selectedWard = String.valueOf(snapshot.getValue());
                                    currentLineNumber = 0;
                                    titleTv.setText("Ward: " + selectedWard);
                                    fetchWardJson();
                                    mainCheckLocationForRealTimeRequest();
                                    dialog.cancel();
                                })
                                .setNegativeButton("", (dialog, i) -> dialog.cancel());
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        rootRef.child("EntityMarkingData/MarkerAppAccess/" + userId + "/assignedWard/").addValueEventListener(cELForAssignedWard);
    }

}