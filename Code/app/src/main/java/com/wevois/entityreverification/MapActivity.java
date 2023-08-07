package com.wevois.entityreverification;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;

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
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.Glide;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    String selectedWard = null, selectedCity, userId, date, cbText;
    String serialNo;
    String city;
    String lineno, wardno;
    ImageView imageViewForRejectedMarker;
    int currentLineNumber = 0;                      // use + 1 to get currentLine;
    Spinner houseTypeSpinner, entityVerifieSpinner, preEntitySpinner;
    TextView currentLineTv, totalMarksTv, titleTv, dateTimeTv;
    Bitmap photo;
    GoogleMap mMap;
    EditText edtTHouse;
    RadioButton isSurveyedTrue, isSurveyedFalse;
    LocationCallback locationCallback;
    LatLng lastKnownLatLngForWalkingMan = null;
    DatabaseReference rootRef;
    SharedPreferences preferences;
    List<List<LatLng>> dbColl = new ArrayList<>();
    List<List<LatLng>> wardBoundaryColl = new ArrayList<>();
    List<String> houseList;
    TextView tvPrefix;
    EditText editCardNo;
    ImageView imgEntity;
    HashMap<String, Integer> houseDataHashMap;
    CommonFunctions common = new CommonFunctions();
    CountDownTimer cdTimer;
    private Camera mCamera;
    private SurfaceView surfaceView;
    Camera.PictureCallback pictureCallback;
    ChildEventListener cELOnLine;
    ChildEventListener cELOnLineCount;
    ChildEventListener cELOnLineCardNotFound;
    ValueEventListener cELForAssignedWard;
    AlertDialog dialogForModification;
    LinearLayout spinnerlayout, cardnolayout;
    String htype, entityCount;
    Button btnCardDetail;
    ImageView btnTakePic;
    AlertDialog dialogVerifier, dialogEntityDetails;
    JSONObject paramObject = new JSONObject();
    String result = null;
    int resCode;
    InputStream in;
    String error = "";
    private long mLastClickTime = 0;
    HashMap<LatLng, MarkersDataModel> mDMMap = new HashMap<>();
    boolean isPass = true,
            captureClickControl = true,
            boolToInstantiateMovingMarker = true,
            enableZoom = true,
            isEdit = false;
    private static final int MAIN_LOC_REQUEST = 5000,
            GPS_CODE_FOR_ENTITY = 501,
            GPS_CODE_FOR_MODIFICATION = 7777,
            FOCUS_AREA_SIZE = 300,
            PERMISSION_CODE = 1000;

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
    @RequiresApi(api = Build.VERSION_CODES.P)
    private void inIt() {

        currentLineTv = findViewById(R.id.current_line_tv);
        rootRef = common.getDatabaseRef(this);
        totalMarksTv = findViewById(R.id.total_marks_tv);
        dateTimeTv = findViewById(R.id.date_and_time_tv);
        date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        preferences = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        selectedWard = preferences.getString("ward", null);
        selectedCity = preferences.getString("city", "");
        userId = preferences.getString("userId", "");
        btnTakePic = findViewById(R.id.img_entity);
        Log.e("cityyyyy", selectedCity);
        if (selectedCity.equals("Malviyanagar")) {
            city = "Jaipur-Malviyanagar";
        } else if (selectedCity.equals("Murlipura")) {
            city = "Jaipur-Murlipura";
        }
//        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
//        mapFragment.getMapAsync(this);
        if (selectedWard != null) {
            common.setProgressDialog("Please Wait.....", "", MapActivity.this, MapActivity.this);
            setPageTitle();
//            fetchWardJson();
//            getWardBoundarys();
//            assignedWardCEL();
            lastScanTimeVEL();
            checkVersionForTheApplication();
        }


        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                openCam();
            }
        });

        if (currentLineNumber == 0) {
            findViewById(R.id.pre_line_btn).setVisibility(View.GONE);
        } else {
            findViewById(R.id.pre_line_btn).setVisibility(View.VISIBLE);
        }
    }

    private void getHouseDetails(String ward, String SerialNo) {

        common.setProgressDialog("Please Wait....", "", this, this);
        Log.e("path", rootRef.child("ImportedData/" + ward + "/" + SerialNo) + "");
        rootRef.child("ImportedData/" + ward + "/" + SerialNo).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {

                    if (dataSnapshot.hasChild("houseType")) {

                        common.closeDialog(MapActivity.this);
                        String type = dataSnapshot.child("entityType").getValue().toString();
                        htype = dataSnapshot.child("houseType").getValue().toString();

                        if (dataSnapshot.hasChild("lineNo")) {
                            lineno = dataSnapshot.child("lineNo").getValue().toString();
                        } else {
                            common.showAlertBox("Line no not available please contact to admin!", "OK", "", MapActivity.this);
                        }
                        preferences.edit().putString("htype", htype).apply();
                        preferences.edit().putString("cardId", SerialNo).apply();
                        preferences.edit().putString("ward", ward).apply();
                        preferences.edit().putString("line", lineno).apply();

                        View dialogLayout = MapActivity.this.getLayoutInflater().inflate(R.layout.card_detail_layout, null);
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this).setView(dialogLayout).setCancelable(false);
                        dialogVerifier = alertDialog.create();
                        preEntitySpinner = dialogLayout.findViewById(R.id.entity_type);
                        TextView tvWard = dialogLayout.findViewById(R.id.tv_ward_no);
                        TextView tvCard = dialogLayout.findViewById(R.id.tv_card_no);
                        tvWard.setText("Ward: " + preferences.getString("ward", ""));
                        tvCard.setText("Card Number: " + serialNo);
                        entityVerifieSpinner = dialogLayout.findViewById(R.id.entity_type_verifier);
                        edtTHouse = dialogLayout.findViewById(R.id.etEntityCount);
                        if (htype.equals("19") || htype.equals("20")) {
                            entityCount = dataSnapshot.child("servingCount").getValue().toString();
                            edtTHouse.setVisibility(View.VISIBLE);
                            edtTHouse.setText(entityCount);
                        }
                        dialogLayout.findViewById(R.id.card_detail_close).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialogVerifier.dismiss();
                            }
                        });
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
                        preEntitySpinner.setAdapter(spinnerArrayAdapter);
                        preEntitySpinner.setEnabled(false);
                        if (!htype.isEmpty()) {
                            preEntitySpinner.setSelection(Integer.parseInt(htype));
                        }else {
                            common.showAlertBox("House Type नहीं मिला, कृपया Admin से संपर्क करें!", "OK", "", MapActivity.this);
                        }

                        entityVerifieSpinner.setAdapter(spinnerArrayAdapter);
                        if (!htype.isEmpty()) {
                            entityVerifieSpinner.setSelection(Integer.parseInt(htype));
                        }else {
                            common.showAlertBox("House Type नहीं मिला, कृपया Admin से संपर्क करें!", "OK", "", MapActivity.this);
                        }
                        entityVerifieSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                                if (entityVerifieSpinner.getSelectedItemId() != 0) {
                                    Log.e("houseTypeSpinner", "" + houseTypeSpinner.getSelectedItemId());
                                    long selectedItemId = entityVerifieSpinner.getSelectedItemId();
                                    if (selectedItemId == 19 || selectedItemId == 20) {
                                        edtTHouse.setVisibility(View.VISIBLE);
                                    } else {
                                        edtTHouse.setVisibility(View.GONE);
                                    }

                                }
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> adapterView) {

                            }
                        });

                        Button btn = dialogLayout.findViewById(R.id.btnVerified);
                        btn.setOnClickListener(v -> {
//                            imgEntity.setImageBitmap(photo);
//                            dialog.dismiss();
                            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                                return;
                            }
                            mLastClickTime = SystemClock.elapsedRealtime();
                            if (edtTHouse.getVisibility() == View.VISIBLE) {
                                if (!edtTHouse.getText().toString().isEmpty()) {
                                    int count = Integer.parseInt(edtTHouse.getText().toString());
                                    if (count > 0) {
                                        checkGpsForEntity("CardFound");
                                    } else {
                                        Toast.makeText(MapActivity.this, "Enter valid entity count", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(MapActivity.this, "Enter entity count", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                checkGpsForEntity("CardFound");
                            }


                        });
//                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.GRAY));
                        if (!htype.isEmpty())
                            dialogVerifier.show();
                    } else {
                        common.closeDialog(MapActivity.this);
                        common.showAlertBox("House Type नहीं मिला, कृपया Admin से संपर्क करें!", "OK", "", MapActivity.this);
//                        Toast.makeText(MapActivity.this, "House Type not fond please contact to admin!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    common.closeDialog(MapActivity.this);
//                    Toast.makeText(MapActivity.this, "Card detail not found", Toast.LENGTH_SHORT).show();
                    View dialogLayout = MapActivity.this.getLayoutInflater().inflate(R.layout.card_detail_not_found_layout, null);
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this).setView(dialogLayout).setCancelable(false);
                    dialogVerifier = alertDialog.create();
                    preEntitySpinner = dialogLayout.findViewById(R.id.entity_type);
                    TextView tvWard = dialogLayout.findViewById(R.id.tv_ward_no);
                    TextView tvCard = dialogLayout.findViewById(R.id.tv_card_no);
                    tvWard.setText("Ward: " + preferences.getString("ward", ""));
                    tvCard.setText("Card Number: " + serialNo);
                    edtTHouse = dialogLayout.findViewById(R.id.etEntityCount);
                    dialogLayout.findViewById(R.id.card_detail_not_found_close).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialogVerifier.dismiss();
                        }
                    });
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
                    preEntitySpinner.setAdapter(spinnerArrayAdapter);
                    preEntitySpinner.setEnabled(true);
                    preEntitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            if (preEntitySpinner.getSelectedItemId() != 0) {
                                Log.e("houseTypeSpinner", "" + houseTypeSpinner.getSelectedItemId());
                                long selectedItemId = preEntitySpinner.getSelectedItemId();
                                if (selectedItemId == 19 || selectedItemId == 20) {
                                    edtTHouse.setVisibility(View.VISIBLE);
                                } else {
                                    edtTHouse.setVisibility(View.GONE);
                                }

                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });

                    Button btn = dialogLayout.findViewById(R.id.btnVerified);
                    btn.setOnClickListener(v -> {
//                            imgEntity.setImageBitmap(photo);
//                            dialog.dismiss();
                        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                            return;
                        }
                        mLastClickTime = SystemClock.elapsedRealtime();
                        if (preEntitySpinner.getSelectedItemPosition() != 0) {
                            if (edtTHouse.getVisibility() == View.VISIBLE) {
                                if (!edtTHouse.getText().toString().isEmpty()) {
                                    int count = Integer.parseInt(edtTHouse.getText().toString());
                                    if (count > 0) {
                                        checkGpsForEntity("CardDetailNotFound");
                                    } else {
                                        Toast.makeText(MapActivity.this, "Enter valid entity count", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(MapActivity.this, "Enter entity count", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                checkGpsForEntity("CardDetailNotFound");
                            }
                        } else {
                            Toast.makeText(MapActivity.this, "Select entity type", Toast.LENGTH_SHORT).show();
                        }

                    });
//                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.GRAY));
                    dialogVerifier.show();

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.e("onMapReady", "onMapReady");
        mMap = googleMap;
        mMap.setOnMarkerClickListener(marker -> {
            try {
                if (mDMMap.containsKey(marker.getPosition())) {
                    MarkersDataModel mDM = mDMMap.get(marker.getPosition());
                    assert mDM != null;
                    if (mDM.isStatus()) {
//                        dialogForRejectedMarker(mDM, marker);
                        return false;
                    }
                }
                Toast.makeText(MapActivity.this, "Card Not Rejected", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        });
        mainCheckLocationForRealTimeRequest();
        getWardBoundarys();
    }

    private void setRB() {
        isSurveyedFalse.setOnClickListener(view -> {
            isSurveyedFalse.setChecked(true);
            isSurveyedTrue.setChecked(false);
            spinnerlayout.setVisibility(View.VISIBLE);
            cardnolayout.setVisibility(View.GONE);
        });
        isSurveyedTrue.setOnClickListener(view -> {
            isSurveyedFalse.setChecked(false);
            isSurveyedTrue.setChecked(true);
            spinnerlayout.setVisibility(View.GONE);
            cardnolayout.setVisibility(View.VISIBLE);
            tvPrefix.setText(preferences.getString("prefix", ""));
        });
    }

    private void setBothRBUnchecked() {
        isSurveyedFalse.setChecked(false);
        isSurveyedTrue.setChecked(false);
    }

    private boolean checkWhichRBisChecked() {
        return isSurveyedTrue.isChecked();
    }

    @SuppressLint("SetTextI18n")
    private void setPageTitle() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        titleTv = toolbar.findViewById(R.id.toolbar_title);
        titleTv.setText("Ward : " + selectedWard);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
        toolbar.setNavigationOnClickListener(v -> MapActivity.this.onBackPressed());
    }

    private void fHouseTypeFromSto() {

        Log.e("path", "" + common.getDatabaseStoragePath(MapActivity.this));
        common.getDatabaseStoragePath(MapActivity.this).child("/Default/FinalHousesType.json")
                .getMetadata().addOnSuccessListener(storageMetadata -> {
                    long serverUpdation = storageMetadata.getCreationTimeMillis();
                    long localUpdation = common.getDatabaseSp(MapActivity.this).getLong("houseTypeLastUpdate", 0);
                    if (serverUpdation != localUpdation) {
                        common.getDatabaseSp(MapActivity.this).edit().putLong("houseTypeLastUpdate", serverUpdation).apply();
                        try {
                            File local = File.createTempFile("temp", "txt");
                            common.getDatabaseStoragePath(MapActivity.this)
                                    .child("/Default/FinalHousesType.json")
                                    .getFile(local).addOnCompleteListener(task -> {
                                        try {
                                            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(local)));
                                            StringBuilder sb = new StringBuilder();
                                            String str;
                                            while ((str = br.readLine()) != null) {
                                                sb.append(str);
                                            }
                                            common.getDatabaseSp(MapActivity.this).edit().putString("houseType", sb.toString().trim()).apply();
                                            parseSpinnerData();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e("exce", e.getMessage());
                        }

                    } else {
                        parseSpinnerData();
                    }
                });
    }

    private void parseSpinnerData() {
        try {
            JSONArray arr = new JSONArray(common.getDatabaseSp(MapActivity.this).getString("houseType", ""));
            houseList = new ArrayList<>();
            houseDataHashMap = new HashMap<>();
            houseList.add("Select House Type");
            for (int i = 0; i < arr.length(); i++) {
                if (!arr.get(i).toString().equalsIgnoreCase("null")) {
                    JSONObject o = arr.getJSONObject(i);
                    String[] tempStr = String.valueOf(o.get("name")).split("\\(");
                    houseDataHashMap.put(String.valueOf(tempStr[0]), i);
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
            houseTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    if (houseTypeSpinner.getSelectedItemId() != 0) {
                        Log.e("houseTypeSpinner", "" + houseTypeSpinner.getSelectedItemId());
                        long selectedItemId = houseTypeSpinner.getSelectedItemId();
                        if (selectedItemId == 19 || selectedItemId == 20) {
                            edtTHouse.setVisibility(View.VISIBLE);
                            onSaveClick(view);
                        } else {
                            edtTHouse.setVisibility(View.GONE);
                            onSaveClick(view);
                        }

                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void fetchWardJson() {

        getMapUpdate();
    }

    @SuppressLint("StaticFieldLeak")
    @RequiresApi(api = Build.VERSION_CODES.P)
    public void getMapUpdate() {


        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
//                common.setProgressDialog("", "Please Wait..", MapActivity.this, MapActivity.this);
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                return common.network(MapActivity.this);
            }

            @Override
            protected void onPostExecute(Boolean result) {
//                common.closeDialog(MapActivity.this);
                if (result) {

                    try {

                        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                        StrictMode.setThreadPolicy(policy);
                        String urlString = "https://firebasestorage.googleapis.com/v0/b/dtdnavigator.appspot.com/o/" + city + "%2FWardLinesHouseJson%2F" + selectedWard + "%2FmapUpdateHistoryJson.json?alt=media";
                        Log.e("EntityRV", " url " + urlString);
                        HttpURLConnection urlConnection = null;
                        URL url = new URL(urlString);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setRequestMethod("GET");
                        urlConnection.setReadTimeout(600000 /* milliseconds */);
                        urlConnection.setConnectTimeout(600000 /* milliseconds */);
//                        urlConnection.setDoOutput(true);
                        urlConnection.connect();

                        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                        StringBuilder sb = new StringBuilder();

                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line + "\n");
                        }
                        br.close();
                        String jsonString = sb.toString();
//            JSONObject object = new JSONObject(jsonString);
//            JSONArray array = object.getJSONArray("points");
//            System.out.println("map json: " + jsonString.toString());
                        checkDate(jsonString);
                        prepareDB(jsonString);

                    } catch (SocketTimeoutException ss) {
                        // show message to the user
                        common.showAlertBox("Network connection slow!", "OK", "", MapActivity.this);
                    } catch (IOException e) {
                        Log.e("Exce", e.getMessage() + "");
                    }

                } else {
                    isPass = true;
                    houseTypeSpinner.setSelection(0);
                    common.showAlertBox("Please Connect to internet", "Ok", "", MapActivity.this);
                }
            }
        }.execute();

    }

    @SuppressLint("StaticFieldLeak")
    @RequiresApi(api = Build.VERSION_CODES.P)
    public void getWardBoundarys() {

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                common.setProgressDialog("", "Please Wait..", MapActivity.this, MapActivity.this);
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                return common.network(MapActivity.this);
            }

            @Override
            protected void onPostExecute(Boolean result) {
//                common.closeDialog(MapActivity.this);
                if (result) {

                    try {

                        Log.e("city name", "name  " + city);
                        String urlString = "https://firebasestorage.googleapis.com/v0/b/dtdnavigator.appspot.com/o/" + city + "%2FWardBoundryJson%2F" + selectedWard + ".json?alt=media";
                        Log.e("EntityRV", " url " + urlString);
                        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                        StrictMode.setThreadPolicy(policy);

                        HttpURLConnection urlConnection = null;
                        URL url = new URL(urlString);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setRequestMethod("GET");
                        urlConnection.setReadTimeout(500000 /* milliseconds */);
                        urlConnection.setConnectTimeout(500000 /* milliseconds */);
//                        urlConnection.setDoOutput(true);
                        urlConnection.connect();

                        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                        StringBuilder sb = new StringBuilder();

                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line + "\n");
                        }
                        br.close();
                        String jsonString = sb.toString();
                        JSONObject object = new JSONObject(jsonString);
                        JSONArray array = object.getJSONArray("points");
//                      System.out.println("JSON: " + array.toString());
                        prepareWardBoundary(jsonString);

                    } catch (SocketTimeoutException ss) {
                        // show message to the user
                        common.showAlertBox("Network connection slow!", "OK", "", MapActivity.this);
                    } catch (IOException | JSONException e) {
                        Log.e("Exce", e.getMessage() + "");
                    }
                } else {
                    isPass = true;
                    houseTypeSpinner.setSelection(0);
                    common.showAlertBox("Please Connect to internet", "Ok", "", MapActivity.this);
                }
            }
        }.execute();


//        return new JSONObject(jsonString);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void checkDate(String result) {

        try {
            JSONArray jsonArray = new JSONArray(result);
            for (int i = jsonArray.length() - 1; i >= 0; i--) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    Date date1 = format.parse(format.format(new Date()));
                    Date date2 = format.parse(jsonArray.getString(i));
                    if (date1.after(date2)) {
                        preferences.edit().putString("commonReferenceDate", String.valueOf(jsonArray.getString(i))).apply();
                        fileMetaDownload(String.valueOf(jsonArray.getString(i)));
                        break;
                    } else if (date1.equals(date2)) {
                        preferences.edit().putString("commonReferenceDate", String.valueOf(jsonArray.getString(i))).apply();
                        fileMetaDownload(String.valueOf(jsonArray.getString(i)));
                        break;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @SuppressLint("StaticFieldLeak")
    @RequiresApi(api = Build.VERSION_CODES.P)
    private void fileMetaDownload(String dates) {

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
//                common.setProgressDialog("", "Please Wait..", MapActivity.this, MapActivity.this);
            }

            @Override
            protected Boolean doInBackground(Void... p) {
                return common.network(MapActivity.this);
            }

            @Override
            protected void onPostExecute(Boolean result) {
//                common.closeDialog(MapActivity.this);
                if (result) {
                    try {

                        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                        StrictMode.setThreadPolicy(policy);
                        String urlString = "https://firebasestorage.googleapis.com/v0/b/dtdnavigator.appspot.com/o/" + city + "%2FWardLinesHouseJson%2F" + selectedWard + "%2F" + dates + ".json?alt=media";
                        Log.e("EntityRV", " url " + urlString);
                        HttpURLConnection urlConnection = null;
                        URL url = new URL(urlString);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setRequestMethod("GET");
                        urlConnection.setReadTimeout(500000 /* milliseconds */);
                        urlConnection.setConnectTimeout(500000 /* milliseconds */);
//                        urlConnection.setDoOutput(true);
                        urlConnection.connect();

                        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                        StringBuilder sb = new StringBuilder();

                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line + "\n");
                        }
                        br.close();
                        String jsonString = sb.toString();
                        prepareDB(jsonString);

                    } catch (SocketTimeoutException ss) {
                        // show message to the user
                        common.showAlertBox("Network connection slow!", "OK", "", MapActivity.this);
                    } catch (IOException e) {
                        Log.e("Exception", e.getMessage() + "");
                    }
                } else {
                    isPass = true;
                    houseTypeSpinner.setSelection(0);
                    common.showAlertBox("Please Connect to internet", "Ok", "", MapActivity.this);
                }
            }
        }.execute();


    }

    private void prepareDB(String result) {

        try {
            JSONObject wardJSONObject = new JSONObject(result);
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
            celForLineCardNotFound();
            celForLineCardDetailNotFound();
            celCount();
            drawLine();
            fetchMarkerForLine(false);
            Log.e("EntityRV","close dialog");
//            common.closeDialog(this);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void prepareWardBoundary(String result) {

        try {
            JSONObject wardJSONObject = new JSONObject(result);
            Log.e("wardJSONObject size", wardJSONObject.length() + "");
            wardBoundaryColl = new ArrayList<>();
            try {

                List<LatLng> tempList = new ArrayList<>();
                JSONArray latLngPointJSONArray = wardJSONObject.getJSONArray("points");
                for (int a = 0; a < latLngPointJSONArray.length(); a++) {
                    try {
                        String lat = latLngPointJSONArray.getJSONArray(a).getString(0);
                        String lng = latLngPointJSONArray.getJSONArray(a).getString(1);
                        Log.e("lat ", lat + "");
                        Log.e("lat ", lng + "");
                        tempList.add(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                wardBoundaryColl.add(tempList);

            } catch (JSONException e) {
                e.printStackTrace();
            }

//            celForLine();
//            drawLine();
//            fetchMarkerForLine(false);
//            common.closeDialog(this);
            fetchWardJson();

            celForLine();
            celForLineCardNotFound();
            celForLineCardDetailNotFound();
            celCount();
            drawLine();
            fetchMarkerForLine(false);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    private void celCount() {

        cELOnLineCount = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.getValue() != null) {
                    Log.e("house count", snapshot.getValue().toString());
                    totalMarksTv.setText(String.valueOf(snapshot.getValue()));
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.getValue() != null) {
                    Log.e("house count", snapshot.getValue().toString());
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

    private void celForLine() {

//        Log.e("EntityReverification","line no "+currentLineNumber);
//        common.setProgressDialog("Please Wait....", "", this, this);
        rootRef.child("EntityReverification/" + selectedWard + "/" + "CardFound").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
//                    Log.e("EntityReverification", "line nooo " + currentLineNumber);
//                    Log.e("EntityReverification", "line nooo " + snapshot.toString());
                    if (Objects.equals(snapshot.getKey(), "marksCount")) {
//                        totalMarksTv.setText(String.valueOf(snapshot.getValue()));
                    }
                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {
//                        Log.e("EntityReverification", "snapshot1 " + snapshot1.getValue().toString());
//                        Log.e("EntityReverification", "snapshot1 " + snapshot1.getKey());
                        if (snapshot1.hasChild("lineNo")) {
                            String line = String.valueOf(snapshot1.child("lineNo").getValue());
                            if (snapshot1.hasChild("latLng")) {
                                String[] tempStr = String.valueOf(snapshot1.child("latLng").getValue()).split(",");
                                double lat = Double.parseDouble(tempStr[0]);
                                double lng = Double.parseDouble(tempStr[1]);
//                                Log.e("EntityReverification", "snapshot1 " + lat + " " + lng);
//                                if (isRejectedMarker(snapshot1)) {
//                                    mDMMap.put(new LatLng(lat, lng), new MarkersDataModel(Boolean.parseBoolean(snapshot1.child("alreadyInstalled").getValue().toString()),
//                                            (Boolean) isRejectedMarker(snapshot1),
//                                            String.valueOf(snapshot1.child("date").getValue()),
//                                            (String) snapshot1.child("image").getValue(),
//                                            Integer.parseInt(String.valueOf(snapshot1.child("houseType").getValue())),
//                                            Integer.parseInt(String.valueOf(snapshot1.getKey()))));
//
//                                    mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.rejected_marker_icon)));
//                                } else {
                                String currentLine = String.valueOf((currentLineNumber + 1));
                                if (line.equals(currentLine)) {
//                                    Log.e("EntityReverification", "snapshot1 line" + line);
                                    mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.gharicon)));
                                }
//                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

//        cELOnLine = new ChildEventListener() {
//            @Override
//            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
//                if (snapshot.getValue() != null) {
//                    Log.e("EntityReverification","line nooo "+currentLineNumber);
//                    Log.e("EntityReverification","line nooo "+snapshot.getChildren().toString());
//                    if (Objects.equals(snapshot.getKey(), "marksCount")) {
////                        totalMarksTv.setText(String.valueOf(snapshot.getValue()));
//                    }
//                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {
////                        Log.e("EntityReverification","snapshot1 "+snapshot1.getValue().toString());
////                        Log.e("EntityReverification","snapshot1 "+snapshot1.getKey());
//                        if (Objects.equals(snapshot1.getKey(), "lineNo")) {
//                            int line = Integer.parseInt(String.valueOf(snapshot1.child(snapshot1.getKey()).getValue()));
//                            Log.e("EntityReverification", "snapshot1" + line);
//                            if (snapshot1.hasChild("latLng")) {
//                                String[] tempStr = String.valueOf(snapshot1.child("latLng").getValue()).split(",");
//                                double lat = Double.parseDouble(tempStr[0]);
//                                double lng = Double.parseDouble(tempStr[1]);
//                                Log.e("EntityReverification","snapshot1 "+lat+" "+lng);
////                                if (isRejectedMarker(snapshot1)) {
////                                    mDMMap.put(new LatLng(lat, lng), new MarkersDataModel(Boolean.parseBoolean(snapshot1.child("alreadyInstalled").getValue().toString()),
////                                            (Boolean) isRejectedMarker(snapshot1),
////                                            String.valueOf(snapshot1.child("date").getValue()),
////                                            (String) snapshot1.child("image").getValue(),
////                                            Integer.parseInt(String.valueOf(snapshot1.child("houseType").getValue())),
////                                            Integer.parseInt(String.valueOf(snapshot1.getKey()))));
////
////                                    mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.rejected_marker_icon)));
////                                } else {
//                                    mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.gharicon)));
////                                }
//                            }
//                        }
//                    }
//                }
//
//            }
//
//            @Override
//            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
//                if (Objects.equals(snapshot.getKey(), "marksCount")) {
////                    totalMarksTv.setText(String.valueOf(snapshot.getValue()));
//                }
//            }
//
//            @Override
//            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
//            }
//
//            @Override
//            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//            }
//        };

    }


    private void celForLineCardNotFound() {

        rootRef.child("EntityReverification/" + selectedWard + "/" + "CardNotFound").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    if (Objects.equals(snapshot.getKey(), "marksCount")) {
//                        totalMarksTv.setText(String.valueOf(snapshot.getValue()));
                    }
                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                        Log.e("EntityReverification", "snapshot1 " + snapshot1.getValue().toString());
                        Log.e("EntityReverification", "snapshot1 " + snapshot1.getKey());
                        if (snapshot1.hasChild("lineNo")) {
                            String line = String.valueOf(snapshot1.child("lineNo").getValue());
                            Log.e("EntityReverification", "snapshot1" + line);
                            if (snapshot1.hasChild("latLng")) {
                                String[] tempStr = String.valueOf(snapshot1.child("latLng").getValue()).split(",");
                                double lat = Double.parseDouble(tempStr[0]);
                                double lng = Double.parseDouble(tempStr[1]);

//                            if (isRejectedMarker(snapshot)) {
//                                mDMMap.put(new LatLng(lat, lng), new MarkersDataModel(Boolean.parseBoolean(snapshot.child("alreadyInstalled").getValue().toString()),
//                                        (Boolean) isRejectedMarker(snapshot),
//                                        String.valueOf(snapshot.child("date").getValue()),
//                                        (String) snapshot.child("image").getValue(),
//                                        Integer.parseInt(String.valueOf(snapshot.child("houseType").getValue())),
//                                        Integer.parseInt(String.valueOf(snapshot.getKey()))));
//
//                                mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.rejected_marker_icon)));
//                            } else {
                                String currentLine = String.valueOf((currentLineNumber + 1));
                                if (line.equals(currentLine))
                                    mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.rejected_marker_icon)));
//                            }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

//        cELOnLineCardNotFound = new ChildEventListener() {
//            @Override
//            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
//                if (snapshot.getValue() != null) {
//                    if (Objects.equals(snapshot.getKey(), "marksCount")) {
////                        totalMarksTv.setText(String.valueOf(snapshot.getValue()));
//                    }
//                    if (snapshot.hasChild("latLng")) {
//                        String[] tempStr = String.valueOf(snapshot.child("latLng").getValue()).split(",");
//                        double lat = Double.parseDouble(tempStr[0]);
//                        double lng = Double.parseDouble(tempStr[1]);
//
//                        if (isRejectedMarker(snapshot)) {
//                            mDMMap.put(new LatLng(lat, lng), new MarkersDataModel(Boolean.parseBoolean(snapshot.child("alreadyInstalled").getValue().toString()),
//                                    (Boolean) isRejectedMarker(snapshot),
//                                    String.valueOf(snapshot.child("date").getValue()),
//                                    (String) snapshot.child("image").getValue(),
//                                    Integer.parseInt(String.valueOf(snapshot.child("houseType").getValue())),
//                                    Integer.parseInt(String.valueOf(snapshot.getKey()))));
//
//                            mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.rejected_marker_icon)));
//                        } else {
//                            mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.rejected_marker_icon)));
//                        }
//                    }
//                }
//
//            }
//
//            @Override
//            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
//                if (Objects.equals(snapshot.getKey(), "marksCount")) {
////                    totalMarksTv.setText(String.valueOf(snapshot.getValue()));
//                }
//
//            }
//
//            @Override
//            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
//            }
//
//            @Override
//            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//            }
//        };

    }

    private void celForLineCardDetailNotFound() {

        rootRef.child("EntityReverification/" + selectedWard + "/" + "CardDetailNotFound").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                common.closeDialog(MapActivity.this);
                if (snapshot.getValue() != null) {
                    if (Objects.equals(snapshot.getKey(), "marksCount")) {
//                        totalMarksTv.setText(String.valueOf(snapshot.getValue()));
                    }
                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                        Log.e("EntityReverification", "snapshot1 " + snapshot1.getValue().toString());
                        Log.e("EntityReverification", "snapshot1 " + snapshot1.getKey());
                        if (snapshot1.hasChild("lineNo")) {
                            String line = String.valueOf(snapshot1.child("lineNo").getValue());
                            Log.e("EntityReverification", "snapshot1" + line);
                            if (snapshot1.hasChild("latLng")) {
                                String[] tempStr = String.valueOf(snapshot1.child("latLng").getValue()).split(",");
                                double lat = Double.parseDouble(tempStr[0]);
                                double lng = Double.parseDouble(tempStr[1]);

//                            if (isRejectedMarker(snapshot)) {
//                                mDMMap.put(new LatLng(lat, lng), new MarkersDataModel(Boolean.parseBoolean(snapshot.child("alreadyInstalled").getValue().toString()),
//                                        (Boolean) isRejectedMarker(snapshot),
//                                        String.valueOf(snapshot.child("date").getValue()),
//                                        (String) snapshot.child("image").getValue(),
//                                        Integer.parseInt(String.valueOf(snapshot.child("houseType").getValue())),
//                                        Integer.parseInt(String.valueOf(snapshot.getKey()))));
//
//                                mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.rejected_marker_icon)));
//                            } else {
                                String currentLine = String.valueOf((currentLineNumber + 1));
                                if (line.equals(currentLine))
                                    mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.rejected_marker_icon)));
//                            }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

//        cELOnLineCardNotFound = new ChildEventListener() {
//            @Override
//            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
//                if (snapshot.getValue() != null) {
//                    if (Objects.equals(snapshot.getKey(), "marksCount")) {
////                        totalMarksTv.setText(String.valueOf(snapshot.getValue()));
//                    }
//                    if (snapshot.hasChild("latLng")) {
//                        String[] tempStr = String.valueOf(snapshot.child("latLng").getValue()).split(",");
//                        double lat = Double.parseDouble(tempStr[0]);
//                        double lng = Double.parseDouble(tempStr[1]);
//
//                        if (isRejectedMarker(snapshot)) {
//                            mDMMap.put(new LatLng(lat, lng), new MarkersDataModel(Boolean.parseBoolean(snapshot.child("alreadyInstalled").getValue().toString()),
//                                    (Boolean) isRejectedMarker(snapshot),
//                                    String.valueOf(snapshot.child("date").getValue()),
//                                    (String) snapshot.child("image").getValue(),
//                                    Integer.parseInt(String.valueOf(snapshot.child("houseType").getValue())),
//                                    Integer.parseInt(String.valueOf(snapshot.getKey()))));
//
//                            mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.rejected_marker_icon)));
//                        } else {
//                            mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.rejected_marker_icon)));
//                        }
//                    }
//                }
//
//            }
//
//            @Override
//            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
//                if (Objects.equals(snapshot.getKey(), "marksCount")) {
////                    totalMarksTv.setText(String.valueOf(snapshot.getValue()));
//                }
//
//            }
//
//            @Override
//            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
//            }
//
//            @Override
//            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//            }
//        };

    }

    private boolean isRejectedMarker(DataSnapshot snapshot) {
        if (snapshot.hasChild("status")) {
            return String.valueOf(snapshot.child("status").getValue()).equals("Reject");
        }
        return false;
    }

    @SuppressLint("SetTextI18n")
    private void fetchMarkerForLine(boolean isCloseProgressDialog) {

        totalMarksTv.setText("" + 0);
        mDMMap = new HashMap<>();
        enableZoom = true;
//        rootRef.child("EntityReverification/" + selectedWard + "/" + "CardFound").addChildEventListener(cELOnLine);
        rootRef.child("EntityReverification/" + selectedWard).addChildEventListener(cELOnLineCount);
        celForLineCardDetailNotFound();
        celForLine();
        celForLineCardNotFound();
//        rootRef.child("EntityReverification/" + selectedWard + "/" + "CardNotFound").addChildEventListener(cELOnLineCardNotFound);
        if (isCloseProgressDialog) {
            if (cELOnLine != null) {
                rootRef.child("EntityReverification/" + selectedWard + "/" + "CardFound").removeEventListener(cELOnLine);
            }
            if (cELOnLineCardNotFound != null) {
                rootRef.child("EntityReverification/" + selectedWard + "/" + "CardNotFound").removeEventListener(cELOnLineCardNotFound);
            }
//            common.closeDialog(MapActivity.this);
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

    private void checkGpsForEntity(String type) {

        LocationServices.getSettingsClient(this).checkLocationSettings(new LocationSettingsRequest.Builder()
                .addLocationRequest(new LocationRequest().setInterval(5000).setFastestInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY))
                .setAlwaysShow(true).setNeedBle(true).build()).addOnCompleteListener(task1 -> {
            try {
                task1.getResult(ApiException.class);
                if (task1.isSuccessful()) {
//                    openCam();
                    pickLocForEntity(type);
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

    private void pickLocForEntity(String type) {

        common.closeDialog(MapActivity.this);
        if (lastKnownLatLngForWalkingMan != null) {
//            updateMarksCount();
            saveMarkedLocationAndUploadPhoto(type);
        } else {
            common.showAlertBox("Please Refresh Location", "Ok", "", MapActivity.this);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void updateMarksCount() {

        common.closeDialog(MapActivity.this);
        View dialogLayout = MapActivity.this.getLayoutInflater().inflate(R.layout.custom_image_preview, null);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this).setView(dialogLayout).setCancelable(false);
        AlertDialog dialog = alertDialog.create();

        ImageView imageView = dialogLayout.findViewById(R.id.image_preview);
        imageView.setImageBitmap(photo);

        Button btn = dialogLayout.findViewById(R.id.proceed_preview_image_btn);
        btn.setOnClickListener(v -> {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();
            showCardDetailAlert();
            dialog.dismiss();


        });
        Button closeBtn = dialogLayout.findViewById(R.id.close_preview_image_btn);
        closeBtn.setOnClickListener(v -> {
//            houseTypeSpinner.setSelection(0);
            dialog.dismiss();
        });
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();

        captureClickControl = true;
//        houseTypeSpinner.setEnabled(true);
    }

    @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
    private void saveMarkedLocationAndUploadPhoto(String type) {

        common.closeDialog(MapActivity.this);
        common.setProgressDialog("", "Saving data", MapActivity.this, MapActivity.this);

        try {
            if (type.equals("CardFound")) {
                HashMap<String, Object> hM = new HashMap<>();
                hM.put("latLng", lastKnownLatLngForWalkingMan.latitude + "," + lastKnownLatLngForWalkingMan.longitude);
                hM.put("lineNo", (currentLineNumber + 1));
                hM.put("verifiedBy", userId);
                hM.put("image", serialNo + ".jpg");
                hM.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
                hM.put("preEntityType", houseDataHashMap.get(preEntitySpinner.getSelectedItem()));
                hM.put("entityType", houseDataHashMap.get(entityVerifieSpinner.getSelectedItem()));
                hM.put("entityCount", edtTHouse.getText().toString());
                rootRef.child("EntityReverification/" + selectedWard + "/" + type + "/" + serialNo).updateChildren(hM).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        dialogEntityDetails.dismiss();
                        dialogVerifier.dismiss();
                    }
                });
                common.increaseCountByOne(rootRef.child("EntityReverification/" + selectedWard).child("marksCount"));
                ByteArrayOutputStream toUpload = new ByteArrayOutputStream();
                Bitmap.createScaledBitmap(photo, 400, 600, false)
                        .compress(Bitmap.CompressFormat.JPEG, 80, toUpload);
                FirebaseStorage.getInstance().getReferenceFromUrl("gs://entity-verification.appspot.com/" + selectedCity)
                        .child("/EntityReverificationImages/" + selectedWard + "/" + type + "/" + serialNo + ".jpg")
                        .putBytes(toUpload.toByteArray())
                        .addOnSuccessListener((UploadTask.TaskSnapshot taskSnapshot) -> {
                            if (taskSnapshot.getTask().isSuccessful()) {
                                photo = null;
                                enableZoom = true;
                                common.closeDialog(MapActivity.this);
                                common.showAlertBox("Entity verification complete!", "OK", "", MapActivity.this);
                                fetchMarkerForLine(false);
//                                Toast.makeText(MapActivity.this, "Entity verification complete!", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(e -> common.closeDialog(MapActivity.this));
            } else if (type.equals("CardDetailNotFound")) {
                HashMap<String, Object> hM = new HashMap<>();
                hM.put("latLng", lastKnownLatLngForWalkingMan.latitude + "," + lastKnownLatLngForWalkingMan.longitude);
                hM.put("lineNo", (currentLineNumber + 1));
                hM.put("verifiedBy", userId);
                hM.put("image", serialNo + ".jpg");
                hM.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
                hM.put("entityType", houseDataHashMap.get(preEntitySpinner.getSelectedItem()));
                hM.put("entityCount", edtTHouse.getText().toString());
                rootRef.child("EntityReverification/" + selectedWard + "/" + type + "/" + serialNo).updateChildren(hM).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        dialogEntityDetails.dismiss();
                        dialogVerifier.dismiss();
                    }
                });
                common.increaseCountByOne(rootRef.child("EntityReverification/" + selectedWard).child("marksCount"));
                ByteArrayOutputStream toUpload = new ByteArrayOutputStream();
                Bitmap.createScaledBitmap(photo, 400, 600, false)
                        .compress(Bitmap.CompressFormat.JPEG, 80, toUpload);
                FirebaseStorage.getInstance().getReferenceFromUrl("gs://entity-verification.appspot.com/" + selectedCity)
                        .child("/EntityReverificationImages/" + selectedWard + "/" + type + "/" + serialNo + ".jpg")
                        .putBytes(toUpload.toByteArray())
                        .addOnSuccessListener((UploadTask.TaskSnapshot taskSnapshot) -> {
                            if (taskSnapshot.getTask().isSuccessful()) {
                                photo = null;
                                enableZoom = true;
                                common.closeDialog(MapActivity.this);
                                common.showAlertBox("Entity verification complete!", "OK", "", MapActivity.this);
                                fetchMarkerForLine(false);
//                                Toast.makeText(MapActivity.this, "Entity verification complete!", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(e -> common.closeDialog(MapActivity.this));
            } else {

                Log.e("Entity", type);
                rootRef.child("EntityReverification/" + selectedWard + "/" + type).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int count = 1;
                        if (!snapshot.exists()) {
                            count = 1;
                        } else {
                            for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                Log.e("snapshot", dataSnapshot.getKey() + dataSnapshot.getChildrenCount());
                                count = count + 1;
                            }
                        }

                        HashMap<String, Object> hM = new HashMap<>();
                        hM.put("latLng", lastKnownLatLngForWalkingMan.latitude + "," + lastKnownLatLngForWalkingMan.longitude);
                        hM.put("lineNo", (currentLineNumber + 1));
                        hM.put("image", count + ".jpg");
                        hM.put("verifiedBy", userId);
                        hM.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
                        hM.put("entityType", houseDataHashMap.get(houseTypeSpinner.getSelectedItem()));
                        hM.put("entityCount", edtTHouse.getText().toString());
                        rootRef.child("EntityReverification/" + selectedWard + "/" + type + "/" + count).updateChildren(hM).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                dialogEntityDetails.dismiss();
                            }
                        });
                        common.increaseCountByOne(rootRef.child("EntityReverification/" + selectedWard).child("marksCount"));
                        ByteArrayOutputStream toUpload = new ByteArrayOutputStream();
                        Bitmap.createScaledBitmap(photo, 400, 600, false)
                                .compress(Bitmap.CompressFormat.JPEG, 80, toUpload);
                        FirebaseStorage.getInstance().getReferenceFromUrl("gs://entity-verification.appspot.com/" + selectedCity)
                                .child("/EntityReverificationImages/" + selectedWard + "/" + type + "/" + count + ".jpg")
                                .putBytes(toUpload.toByteArray())
                                .addOnSuccessListener((UploadTask.TaskSnapshot taskSnapshot) -> {
                                    if (taskSnapshot.getTask().isSuccessful()) {
                                        photo = null;
                                        enableZoom = true;
                                        common.closeDialog(MapActivity.this);
                                        common.showAlertBox("Entity verification complete!", "OK", "", MapActivity.this);
                                        fetchMarkerForLine(false);
//                                        Toast.makeText(MapActivity.this, "Entity verification complete!", Toast.LENGTH_SHORT).show();
                                    }
                                }).addOnFailureListener(e -> common.closeDialog(MapActivity.this));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            }

        } catch (Exception e) {
            photo = null;
            enableZoom = true;
            common.closeDialog(MapActivity.this);
            e.printStackTrace();
        }

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
//                    Log.e("Lat ",lastKnownLatLngForWalkingMan.latitude+" Lng "+lastKnownLatLngForWalkingMan.longitude);

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
                    if (enableZoom) {
                        enableZoom = false;
                        LatLngBounds bounds = builder.build();
                        int padding = 200;
                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                        mMap.animateCamera(cu);
                    }
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
        try {
            drawAllLine();
            drawBoundary();
            Log.e("dbCall", dbColl.size() + "");
            mMap.addPolyline(new PolylineOptions().addAll(dbColl.get(currentLineNumber))
                    .endCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.upper60), 30))
                    .startCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.start50), 30))
                    .color(0xff000000)
                    .jointType(JointType.ROUND)
                    .width(8));
        } catch (Exception e) {
            Log.e("exception type", e.getMessage());
        }
        common.closeDialog(MapActivity.this);
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

    private void drawBoundary() {
//        mMap.clear();
//        boolToInstantiateMovingMarker = true;
        Log.e("wardBoundaryColl", wardBoundaryColl.size() + "");
        for (int i = 0; i < wardBoundaryColl.size(); i++) {
//            if (currentLineNumber != i) {
//            Log.e("wardBoundaryColl enter", wardBoundaryColl.get(i) + "");
            mMap.addPolyline(new PolylineOptions().addAll(wardBoundaryColl.get(i))
                    .startCap(new RoundCap())
                    .endCap(new RoundCap())
                    .color(Color.parseColor("#000000"))
                    .jointType(JointType.ROUND)
                    .width(6));
//            }
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
                    List<Camera.Area> meteringAreas = new ArrayList<>();
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

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
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
        if (info.facing == CAMERA_FACING_FRONT) {
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
        surfaceView = dialogLayout.findViewById(R.id.surfaceViews);
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
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                common.setProgressDialog("", "Please Wait", MapActivity.this, MapActivity.this);
                isPass = true;
//                houseTypeSpinner.setEnabled(false);
                if (captureClickControl) {
                    captureClickControl = false;
                    mCamera.takePicture(null, null, null, pictureCallback);
                }
            });
            Button closeBtn = dialogLayout.findViewById(R.id.close_image_btn);
            closeBtn.setOnClickListener(v -> {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                isEdit = false;
//                houseTypeSpinner.setSelection(0);
                dialog.cancel();
                isPass = true;
            });
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();

            pictureCallback = (bytes, camera) -> {
                Matrix matrix = new Matrix();
                matrix.postRotate(90F);
                Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                photo = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);

                if (photo != null) {
                    if (isEdit) {
                        isPass = true;
                        isEdit = false;
                        captureClickControl = true;
                        houseTypeSpinner.setEnabled(true);
                        common.closeDialog(MapActivity.this);
                        imageViewForRejectedMarker.setImageBitmap(photo);
                    } else {
                        updateMarksCount();

                    }
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
                        findViewById(R.id.pre_line_btn).setVisibility(View.VISIBLE);
                        drawLine();
                        fetchMarkerForLine(true);
                    } else {
                        common.closeDialog(MapActivity.this);
                        common.showAlertBox("Please Connect to internet", "Ok", "", MapActivity.this);
                    }
                }
            }.execute();
        } else {
            common.showAlertBox("ईश वार्ड में " + dbColl.size() + " लाइन ही है आप आगे नहीं बढ़ सकते हो", "Ok", "", MapActivity.this);
        }
    }

    public void onSaveClick(View view) {
        Log.e("pass", isPass + "");
        if (isPass) {
            isPass = false;
            if (isSurveyedTrue.isChecked() || isSurveyedFalse.isChecked()) {
                if (edtTHouse.getVisibility() == View.VISIBLE) {
                    if (!edtTHouse.getText().toString().equals("")) {
                        int length = Integer.parseInt(edtTHouse.getText().toString());

                    } else {
//                        houseTypeSpinner.setSelection(0);
                        isPass = true;
//                        common.showAlertBox("Please Enter Total Number of Houses greater than one", "ok", "", MapActivity.this);
                    }
                } else {
                    checkGpsForEntity("CardNotFound");
                }
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
                        Log.e("line noooo", currentLineNumber + "");
                        drawLine();
                        fetchMarkerForLine(true);
                        if (currentLineNumber == 0) {
                            findViewById(R.id.pre_line_btn).setVisibility(View.GONE);
                        }
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
            rootRef.child("EntityReverification/" + selectedWard + "/" + "CardFound").removeEventListener(cELOnLine);
        }
        if (cELForAssignedWard != null) {
            rootRef.child("EntityReverification/" + userId + "/assignedWard").removeEventListener(cELForAssignedWard);
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
                                            if (currentLineNumber == 0) {
                                                findViewById(R.id.pre_line_btn).setVisibility(View.GONE);
                                            } else {
                                                findViewById(R.id.pre_line_btn).setVisibility(View.VISIBLE);
                                            }
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

    @SuppressLint("StaticFieldLeak")
    private void dialogForRejectedMarker(MarkersDataModel mdm, Marker marker) {
        try {
            if (dialogForModification != null) {
                dialogForModification.dismiss();
            }
            View diaLayout = MapActivity.this.getLayoutInflater().inflate(R.layout.form_dialog_for_rejected_marker, null);
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this).setView(diaLayout).setCancelable(false);
            dialogForModification = alertDialog.create();
            Spinner spinner = diaLayout.findViewById(R.id.house_type_spinner_for_rejected_marker);
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
            spinner.setAdapter(spinnerArrayAdapter);
            spinner.setSelection(mdm.getHouseType());
            TextView textView = diaLayout.findViewById(R.id.radio_group_heading_tv_for_rejected_marker);
            RadioButton yesRb = diaLayout.findViewById((R.id.is_surveyed_true_rb_for_rejected_marker));
            RadioButton noRb = diaLayout.findViewById((R.id.is_surveyed_false_rb_for_rejected_marker));
            imageViewForRejectedMarker = diaLayout.findViewById(R.id.rejected_marker_image_preview);
            Button imageButton = diaLayout.findViewById(R.id.re_click_picture);
            textView.setText(cbText);

            yesRb.setOnClickListener(view -> {
                yesRb.setChecked(true);
                noRb.setChecked(false);
            });

            noRb.setOnClickListener(view -> {
                yesRb.setChecked(false);
                noRb.setChecked(true);
            });

            if ((mdm.isAlreadyInstalled())) {
                yesRb.setChecked(true);
            } else {
                noRb.setChecked(true);
            }

            imageButton.setOnClickListener(view -> {
                if (isPass) {
                    isPass = false;
                    isEdit = true;
                    openCam();
                }
            });

            try {
                CircularProgressDrawable circularProgressDrawable = new CircularProgressDrawable(MapActivity.this);
                circularProgressDrawable.setStrokeWidth(5f);
                circularProgressDrawable.setCenterRadius(30f);
                circularProgressDrawable.start();
                FirebaseStorage.getInstance()
                        .getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + selectedCity + "/MarkingSurveyImages/" + selectedWard + "/" + (currentLineNumber + 1) + "/" + mdm.getImageName())
                        .getDownloadUrl()
                        .addOnSuccessListener(uri -> Glide.with(MapActivity.this)
                                .load(uri)
                                .placeholder(circularProgressDrawable)
                                .into(imageViewForRejectedMarker))
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                try {
                                    Toast.makeText(MapActivity.this, "Image Not Available ", Toast.LENGTH_SHORT).show();
                                    imageViewForRejectedMarker.setImageResource(R.drawable.img_not_available);
                                } catch (Exception exc) {
                                    exc.printStackTrace();
                                }

                            }
                        });

            } catch (Exception e) {
                e.printStackTrace();
            }

            Button btn = diaLayout.findViewById(R.id.form_dialog_confirm);
            btn.setOnClickListener(v -> {
                LocationServices.getSettingsClient(this).checkLocationSettings(new LocationSettingsRequest.Builder()
                        .addLocationRequest(new LocationRequest().setInterval(5000).setFastestInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY))
                        .setAlwaysShow(true).setNeedBle(true).build()).addOnCompleteListener(task1 -> {
                    try {
                        task1.getResult(ApiException.class);
                        if (task1.isSuccessful()) {
                            if (lastKnownLatLngForWalkingMan != null) {
                                if (photo != null) {
                                    common.setProgressDialog("", "Please Wait", MapActivity.this, MapActivity.this);
                                    HashMap<String, Object> mapTemp = new HashMap<>();
                                    mapTemp.put("latLng", lastKnownLatLngForWalkingMan.latitude + "," + lastKnownLatLngForWalkingMan.longitude);
                                    mapTemp.put("modifiedBy", userId);
                                    mapTemp.put("alreadyInstalled", yesRb.isChecked());
                                    mapTemp.put("image", mdm.getImageName());
                                    mapTemp.put("modifiedDate", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
                                    mapTemp.put("status", "Re-marked");
                                    mapTemp.put("houseType", houseDataHashMap.get(spinner.getSelectedItem()));
                                    mapTemp.put("totalHouses", edtTHouse.getText().toString());
                                    rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1) + "/" + mdm.getMarkerNumber()).updateChildren(mapTemp);

                                    int temp = Boolean.compare(mdm.isAlreadyInstalled(), yesRb.isChecked());
                                    if (temp == 0) {
                                    } else if (temp > 0) {
                                        common.decCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/WardWise/" + selectedWard + "/alreadyInstalled"));
                                        common.decCountByOne(rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1)).child("alreadyInstalledCount"));
                                    } else {
                                        common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1)).child("alreadyInstalledCount"));
                                        common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/WardWise/" + selectedWard + "/alreadyInstalled"));
                                    }

                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/DateWise/" + date + "/" + userId + "/modified"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/EmployeeWise/" + userId + "/" + selectedWard + "/modified"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/DateWise/" + date + "/" + selectedWard + "/modified"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/WardWise/" + selectedWard + "/modified"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/EmployeeWise/" + userId + "/totalModified"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/DateWise/" + date + "/totalModified"));
                                    common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/DateWise/" + date + "/totalModified"));

                                    try {
                                        ByteArrayOutputStream toUpload = new ByteArrayOutputStream();
                                        Bitmap.createScaledBitmap(photo, 400, 600, false)
                                                .compress(Bitmap.CompressFormat.JPEG, 80, toUpload);
                                        FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + selectedCity)
                                                .child("/MarkingSurveyImages/" + selectedWard + "/" + (currentLineNumber + 1) + "/" + mdm.getImageName())
                                                .putBytes(toUpload.toByteArray())
                                                .addOnSuccessListener((UploadTask.TaskSnapshot taskSnapshot) -> {
                                                    if (taskSnapshot.getTask().isSuccessful()) {
                                                        dialogForModification.dismiss();
                                                        photo = null;
                                                        enableZoom = true;
                                                        common.closeDialog(MapActivity.this);
                                                        marker.setIcon(common.BitmapFromVector(MapActivity.this, R.drawable.gharicon));
                                                        marker.setPosition(lastKnownLatLngForWalkingMan);
                                                        mDMMap.remove(marker.getPosition());
                                                    }
                                                }).addOnFailureListener(e -> common.closeDialog(MapActivity.this));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        dialogForModification.dismiss();
                                        photo = null;
                                        enableZoom = true;
                                        common.closeDialog(MapActivity.this);
                                        marker.setIcon(common.BitmapFromVector(MapActivity.this, R.drawable.gharicon));
                                        marker.setPosition(lastKnownLatLngForWalkingMan);
                                        mDMMap.remove(marker.getPosition());
                                    }
                                } else {
                                    Toast.makeText(MapActivity.this, "Please Click New Image", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    } catch (ApiException e) {
                        if (e instanceof ResolvableApiException) {
                            try {
                                ResolvableApiException resolvable = (ResolvableApiException) e;
                                resolvable.startResolutionForResult(MapActivity.this, GPS_CODE_FOR_MODIFICATION);
                            } catch (IntentSender.SendIntentException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                });
            });
            Button closeBtn = diaLayout.findViewById(R.id.form_dialog_cancel);
            closeBtn.setOnClickListener(v -> dialogForModification.cancel());
            dialogForModification.show();
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
                    Toast.makeText(this, "Permission denieddd", Toast.LENGTH_SHORT).show();
                    startCaptureLocForWalkingMan();
                }
            }

            if (requestCode == PERMISSION_CODE) {
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    showAlertDialog();
                } else {
                    houseTypeSpinner.setSelection(0);
                    isPass = true;
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    mainCheckLocationForRealTimeRequest();
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
                    Toast.makeText(this, "Permission deniede", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == MAIN_LOC_REQUEST) {
                if (resultCode == RESULT_OK) {
                    startCaptureLocForWalkingMan();
                } else {
                    Toast.makeText(this, "Permission deniedi", Toast.LENGTH_SHORT).show();
                    mainCheckLocationForRealTimeRequest();
                }
            } else if (requestCode == GPS_CODE_FOR_MODIFICATION) {
                if (resultCode == RESULT_OK) {

                } else {
                    Toast.makeText(this, "Permission deniedw", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
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
                                    common.getDatabaseSp(MapActivity.this).edit().remove("wardJSONLastUpdate").apply();
                                    common.getDatabaseSp(MapActivity.this).edit().remove("wardJSON").apply();
                                    fetchWardJson();
                                    mainCheckLocationForRealTimeRequest();
                                    dialog.cancel();
                                })
                                .setNegativeButton("", (dialog, i) -> dialog.cancel());
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("exce", e.getMessage());
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        rootRef.child("EntityMarkingData/MarkerAppAccess/" + userId + "/assignedWard/").addValueEventListener(cELForAssignedWard);
    }

    private void lastScanTimeVEL() {

        rootRef.child("EntityMarkingData/LastScanTime/Surveyor/" + userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    dateTimeTv.setText(snapshot.getValue().toString());
                } else {
                    dateTimeTv.setText("---");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void checkVersionForTheApplication() {
        rootRef.child("Settings/LatestVersions/EntityReverification").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    try {
                        String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                        if (!version.equalsIgnoreCase(snapshot.getValue().toString())) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                            builder.setMessage("Version Expired").setCancelable(false)
                                    .setPositiveButton("Ok", (dialog, id) -> {
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
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        Log.e("exce", e.getMessage());
                    }

                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
                    builder.setMessage("Version Expired").setCancelable(false)
                            .setPositiveButton("Ok", (dialog, id) -> {
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showCardDetailAlert() {

        View dialogLayout = MapActivity.this.getLayoutInflater().inflate(R.layout.entity_info_layout, null);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this).setView(dialogLayout).setCancelable(false);
        dialogEntityDetails = alertDialog.create();
        cbText = preferences.getString("alreadyInstalledCbHeading", getResources().getString(R.string.card_allready_install));
        TextView rgHeadingTv = dialogLayout.findViewById(R.id.radio_group_heading_tv);
        isSurveyedTrue = dialogLayout.findViewById(R.id.is_surveyed_true_rb);
        isSurveyedFalse = dialogLayout.findViewById(R.id.is_surveyed_false_rb);
        houseTypeSpinner = dialogLayout.findViewById(R.id.house_type_spinner);
        spinnerlayout = dialogLayout.findViewById(R.id.entityLayout);
        cardnolayout = dialogLayout.findViewById(R.id.cardNolayout);
        edtTHouse = dialogLayout.findViewById(R.id.etTotalHouse);
        tvPrefix = dialogLayout.findViewById(R.id.tvPrefix);
        editCardNo = dialogLayout.findViewById(R.id.serialNumber);
        imgEntity = dialogLayout.findViewById(R.id.imgEntity);
        imgEntity.setImageBitmap(photo);
        rgHeadingTv.setText(cbText);
        setRB();
        fHouseTypeFromSto();
        btnCardDetail = dialogLayout.findViewById(R.id.btnSubmit);
        btnCardDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                String prefix = tvPrefix.getText().toString();
                String cardNo = editCardNo.getText().toString();
                serialNo = prefix + cardNo;
                if (!cardNo.isEmpty() && cardNo.length() == 6) {

                    getHouseDetails(selectedWard, serialNo);

                } else {

                    common.showAlertBox("कृपया वैध कार्ड नंबर दर्ज करें ", "ok", "", MapActivity.this);
                }
            }
        });

        dialogLayout.findViewById(R.id.imgCloseDialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogEntityDetails.dismiss();
            }
        });

        edtTHouse.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    Log.e("TAG", "Enter pressed");
                    if (!edtTHouse.getText().toString().equals("")) {
                        int no = Integer.parseInt(edtTHouse.getText().toString());
                        if (no > 1) {
                            checkGpsForEntity("CardNotFound");
                        } else {
//                                    houseTypeSpinner.setSelection(0);
                            isPass = true;
                            common.showAlertBox("Please Enter Total Number of Houses greater than one", "ok", "", MapActivity.this);
                        }
                    }
                }
                return false;
            }
        });

        dialogEntityDetails.show();
    }

}