package com.example.entitymarking;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.view.inputmethod.InputMethodManager;
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
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
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
import java.nio.charset.StandardCharsets;
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
    Spinner houseTypeSpinner, roadTypeSp;
    TextView currentLineTv, totalMarksTv, titleTv, dateTimeTv;
    Bitmap photo;
    GoogleMap mMap;
    int htypee;
    EditText edtTHouse, edtRoadWidth, edtOwner, edtPerson;
    Spinner entityVerifieSpinner, preEntitySpinner;
    LocationCallback locationCallback;
    LatLng lastKnownLatLngForWalkingMan = null;
    DatabaseReference rootRef;
    SharedPreferences preferences;
    List<List<LatLng>> dbColl = new ArrayList<>();
    List<List<LatLng>> wardBoundaryColl = new ArrayList<>();
    List<String> houseList, roadTypeList;
    TextView tvPrefix, tvRoadPreview;
    ImageView imgEntity;
    HashMap<String, Integer> houseDataHashMap;
    CommonFunctions common = new CommonFunctions();
    CountDownTimer cdTimer;
    private Camera mCamera;
    private SurfaceView surfaceView;
    Camera.PictureCallback pictureCallback;
    ChildEventListener cELOnLine;
    ChildEventListener cELOnLineCount;
    ValueEventListener cELForAssignedWard;
    AlertDialog dialogForModification;
    LinearLayout spinnerlayout, cardnolayout;
    String htype, entityCount;
    ImageView btnTakePic, viewDetails;
    AlertDialog dialogVerifier, dialogEntityDetails;
    JSONObject paramObject = new JSONObject();
    String result = null;
    int resCode, noPerson;
    String ownerName;
    InputStream in;
    String error = "";
    private long mLastClickTime = 0;
    HashMap<LatLng, MarkersDataModel> mDMMap = new HashMap<>();
    AlertDialog dialogRoadDetails;
    String roadType;
    String roadData;
    String roadWidth, imgName, flag = "first";
    ImageView roadImg;

    LinearLayout bottomLayout, bottomRoadLayout;
    boolean isPass = true,
            captureClickControl = true,
            boolToInstantiateMovingMarker = true,
            enableZoom = true,
            isEdit = false;
    private static final int MAIN_LOC_REQUEST = 5000,
            GPS_CODE_FOR_ENTITY = 501,
            GPS_CODE_FOR_MODIFICATION = 7777,
            FOCUS_AREA_SIZE = 300,
            PERMISSION_CODE = 1000,
            CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 2000;
    ;

    @RequiresApi(api = Build.VERSION_CODES.P)
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

    @RequiresApi(api = Build.VERSION_CODES.P)
    public void inIt() {

        currentLineTv = findViewById(R.id.current_line_tv);
        rootRef = common.getDatabaseRef(this);
        houseTypeSpinner = findViewById(R.id.house_type_spinner);
        totalMarksTv = findViewById(R.id.total_marks_tv);
        dateTimeTv = findViewById(R.id.date_and_time_tv);
        bottomLayout = findViewById(R.id.bottomLayout);
        bottomRoadLayout = findViewById(R.id.bottomRoadLayout);
        viewDetails = findViewById(R.id.view_details);
        tvRoadPreview = findViewById(R.id.tv_roadPreview);

        date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        preferences = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        selectedWard = preferences.getString("assignment", null);
        selectedCity = preferences.getString("storagePath", "");
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
            fHouseTypeFromSto();
            fetchWardJson();
            getWardBoundary();
            assignedWardCEL();
            getWardRoadDetail();
            lastScanTimeVEL();
            checkVersionForTheApplication();

        }



        findViewById(R.id.img_road).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCam("road_img");
            }
        });

        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                openCam("entity_img");
            }
        });

        if (currentLineNumber == 0) {
            findViewById(R.id.pre_line_btn).setVisibility(View.GONE);
        } else {
            findViewById(R.id.pre_line_btn).setVisibility(View.VISIBLE);
        }

        viewDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                View dialogLayout = MapActivity.this.getLayoutInflater().inflate(R.layout.road_info_layout, null);
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this).setView(dialogLayout).setCancelable(false);
                dialogRoadDetails = alertDialog.create();
                Spinner sp_roadType = dialogLayout.findViewById(R.id.road_type_spinner);
                edtRoadWidth = dialogLayout.findViewById(R.id.edtRoadWidth);
                roadImg = dialogLayout.findViewById(R.id.imgRoad);
                Button btnUpdate = dialogLayout.findViewById(R.id.btnUpdate);
                btnUpdate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                            return;
                        }
                        mLastClickTime = SystemClock.elapsedRealtime();
                        if (sp_roadType.getSelectedItemId() != 0) {
                            Log.e("houseTypeSpinner", "" + sp_roadType.getSelectedItemId());
                            long selectedItemId = sp_roadType.getSelectedItemId();
                            if (!edtRoadWidth.getText().toString().trim().isEmpty()) {

                                String roadType = roadTypeList.get(sp_roadType.getSelectedItemPosition());
                                String roadWidth = edtRoadWidth.getText().toString().trim();
                                HashMap<String, Object> hM = new HashMap<>();
                                hM.put("latLng", lastKnownLatLngForWalkingMan.latitude + "," + lastKnownLatLngForWalkingMan.longitude);
                                hM.put("roadType", roadType);
                                hM.put("roadWidth", roadWidth);
                                hM.put("image", imgName);
                                common.setProgressDialog("", "Saving data", MapActivity.this, MapActivity.this);
                                rootRef.child("EntityMarkingData/WardRoadDetail/" + selectedWard + "/" + (currentLineNumber + 1)).updateChildren(hM).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);

                                        // Set the message show for the Alert time
                                        builder.setMessage("Road Details submit successfully.");

                                        // Set Alert Title
                                        builder.setTitle("Road Detail Alert !");

                                        // Set Cancelable false for when the user clicks on the outside the Dialog Box then it will remain show
                                        builder.setCancelable(false);

                                        // Set the Negative button with No name Lambda OnClickListener method is use of DialogInterface interface.
                                        builder.setNegativeButton("OK", (DialogInterface.OnClickListener) (dialog, which) -> {
                                            // If user click no then dialog box is canceled.
                                            dialog.cancel();
                                            dialogRoadDetails.dismiss();
                                            getWardRoadDetail();
                                        });

                                        // Create the Alert dialog
                                        AlertDialog alertDialog = builder.create();
                                        // Show the Alert Dialog box
                                        alertDialog.show();

                                    }
                                });
                            } else {
                                edtRoadWidth.setError("Enter road width(In Feet)");
                                edtRoadWidth.requestFocus();
                            }

                        } else {
                            Toast.makeText(MapActivity.this, "Please Select road type", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                roadTypeList = new ArrayList<>();
                roadTypeList.add("Select road type");
                roadTypeList.add("Single");
                roadTypeList.add("Double");
                ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(MapActivity.this, android.R.layout.simple_spinner_item, roadTypeList) {
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
                sp_roadType.setAdapter(spinnerArrayAdapter);
                sp_roadType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        if (sp_roadType.getSelectedItemId() != 0) {
                            Log.e("houseTypeSpinner", "" + sp_roadType.getSelectedItemId());
                            long selectedItemId = sp_roadType.getSelectedItemId();
                            if (flag.equals("first")) {
                                btnUpdate.setVisibility(View.GONE);
                                flag = "sec";
                            } else {
                                btnUpdate.setVisibility(View.VISIBLE);
                            }


                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });
                edtRoadWidth.setText(roadWidth);
                for (int i = 0; i < roadTypeList.size(); i++) {
                    if (roadTypeList.get(i).equals(roadType)) {
                        sp_roadType.setSelection(i);
                        flag = "first";
                        break;
                    }
                }

                String url = "https://firebasestorage.googleapis.com/v0/b/dtdnavigator.appspot.com/o/" + selectedCity + "%2FWardRoadImages%2F" + selectedWard + "%2F" + (currentLineNumber + 1) + "%2F" + imgName + "?alt=media&token=822bc92c-1aa6-4ebf-8a7b-307bb56fd445";
                Log.e("url", url);
                Glide.with(MapActivity.this).load(url).into(roadImg);
                dialogLayout.findViewById(R.id.imgCloseDialog).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        viewDetails.setEnabled(true);
                        viewDetails.setClickable(true);
                        dialogRoadDetails.dismiss();
                    }
                });
                edtRoadWidth.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        btnUpdate.setVisibility(View.VISIBLE);
                    }
                });
                dialogRoadDetails.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialogRoadDetails.show();
            }
        });
    }

    public void onPathClick(View view){

        try {
            // Log the current path and line number
            Log.e("MV"," Path " + dbColl.get(currentLineNumber) + " " + currentLineNumber);

            // Get the list of LatLng for the current line number
            List<LatLng> latLngList = dbColl.get(currentLineNumber);

            // Ensure the list is not empty
            if (latLngList == null || latLngList.isEmpty()) {
                Log.e("Error", "LatLng list is null or empty");
                return;
            }

            // Get the first LatLng from the list
            LatLng latLng = latLngList.get(0);
            String lat = String.valueOf(latLng.latitude);
            String lon = String.valueOf(latLng.longitude);

            // Log the latitude and longitude
            Log.e("LatLng", " Lat " + lat + " Lng " + lon);

            // Create the URI for Google Maps navigation
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lon);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            // Start the activity
            startActivity(mapIntent);
        } catch (Exception e) {
            // Log any exceptions
            Log.e("Exception", e.getMessage(), e);
        }


    }

    private void getWardRoadDetail() {

        Log.e("pradeep", "getWardRoadDetail");
        common.setProgressDialog("Please Wait..", "load data", MapActivity.this, MapActivity.this);
        rootRef.child("EntityMarkingData/WardRoadDetail/" + selectedWard + "/" + (currentLineNumber + 1)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                Log.e("pradeep"," RoadType "+snapshot.toString());
                common.closeDialog(MapActivity.this);
                if (snapshot.getValue() != null) {
                    roadData = "true";
                    if (snapshot.hasChild("roadType")) {
                        roadType = snapshot.child("roadType").getValue().toString();
                        Log.e("pradeep", " RoadType " + roadType);
                    }
                    if (snapshot.hasChild("roadWidth")) {
                        roadWidth = snapshot.child("roadWidth").getValue().toString();
                    }
                    if (snapshot.hasChild("image")) {
                        imgName = snapshot.child("image").getValue().toString();
                    }

                } else {
                    roadData = "false";
                    Log.e("pradeep", " RoadType " + roadData);
                }

                if (roadData.equals("true")) {
                    bottomLayout.setVisibility(View.VISIBLE);
                    bottomRoadLayout.setVisibility(View.GONE);
                    viewDetails.setVisibility(View.VISIBLE);
                    tvRoadPreview.setText("Road Preview");
                } else {
                    bottomLayout.setVisibility(View.GONE);
                    bottomRoadLayout.setVisibility(View.VISIBLE);
                    viewDetails.setVisibility(View.GONE);
                    tvRoadPreview.setText("Road Preview Not Available");
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
                        dialogForRejectedMarker(mDM, marker);
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
        getWardBoundary();
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

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        Log.e("path", "" + storageReference);
        storageReference.child(preferences.getString("storagePath", "") + "/Defaults/FinalHousesType.json")
                .getMetadata().addOnSuccessListener(storageMetadata -> {
                    long serverUpdation = storageMetadata.getCreationTimeMillis();
                    long localUpdation = common.getDatabaseSp(MapActivity.this).getLong("houseTypeLastUpdate", 0);
                    if (serverUpdation != localUpdation) {
                        common.getDatabaseSp(MapActivity.this).edit().putLong("houseTypeLastUpdate", serverUpdation).apply();
                        try {
                            File local = File.createTempFile("temp", "txt");
                            common.getDatabaseStoragePath(MapActivity.this)
                                    .child("/Defaults/FinalHousesType.json")
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
                        }

                    } else {
                        parseSpinnerData();
                    }
                });
    }

    private void parseSpinnerData() {

        Log.e("HouseType ", common.getDatabaseSp(MapActivity.this).getString("houseType", ""));
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
                        getEntityDetailsAlert(selectedItemId);

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

    public void addDetails(long hid) {
        if (hid == 1 || hid == 19) {

            edtPerson.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                        Log.e("TAG", "Enter pressed");
                        if (!edtPerson.getText().toString().equals("")) {
                            noPerson = Integer.parseInt(edtPerson.getText().toString());
                            ownerName = edtOwner.getText().toString();
                            if (!ownerName.equals("")) {
                                if (noPerson > 1) {
                                    checkGpsForEntity("CardNotFound");
                                } else {
//                                    houseTypeSpinner.setSelection(0);
                                    isPass = true;
                                    common.showAlertBox("Please Enter Total Number of Person greater than one", "ok", "", MapActivity.this);
                                }
                            } else {
                                isPass = true;
                                common.showAlertBox("Please Enter Owner Name", "ok", "", MapActivity.this);
                            }
                        }
                    }
                    return false;
                }
            });

        } else if (hid == 20) {

            edtTHouse.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                        Log.e("TAG", "Enter pressed");
                        if (!edtOwner.getText().toString().equals("")) {
                            ownerName = edtOwner.getText().toString();
                            if (!ownerName.equals("")) {
                                checkGpsForEntity("CardNotFound");
                            } else {
//                                    houseTypeSpinner.setSelection(0);
                                isPass = true;
                                common.showAlertBox("Please Enter Owner Name", "ok", "", MapActivity.this);
                            }
                        } else {
                            isPass = true;
                            common.showAlertBox("Please Enter Owner Name", "ok", "", MapActivity.this);
                        }
                    }
                    return false;
                }
            });
        } else {
            edtOwner.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                        Log.e("TAG", "Enter pressed");
                        if (!edtOwner.getText().toString().equals("")) {
                            ownerName = edtOwner.getText().toString();
                            if (!ownerName.equals("")) {
                                checkGpsForEntity("CardNotFound");
                            } else {
//                                    houseTypeSpinner.setSelection(0);
                                isPass = true;
                                common.showAlertBox("Please Enter Owner Name", "ok", "", MapActivity.this);
                            }
                        } else {
                            isPass = true;
                            common.showAlertBox("Please Enter Owner Name", "ok", "", MapActivity.this);
                        }
                    }
                    return false;
                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void fetchWardJson() {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        storageReference.child(preferences.getString("storagePath", "") + "/WardLinesHouseJson/" + selectedWard + "/mapUpdateHistoryJson.json").getMetadata().addOnSuccessListener(storageMetadata -> {
            long fileCreationTime = storageMetadata.getCreationTimeMillis();
            long fileDownloadTime = preferences.getLong(preferences.getString("storagePath", "") + "" + selectedWard + "mapUpdateHistoryJsonDownloadTime", 0);
            if (fileDownloadTime != fileCreationTime) {
                storageReference.child(preferences.getString("storagePath", "") + "/WardLinesHouseJson/" + selectedWard + "/mapUpdateHistoryJson.json").getBytes(10000000).addOnSuccessListener(taskSnapshot -> {
                    try {
                        String str = new String(taskSnapshot, StandardCharsets.UTF_8);
                        preferences.edit().putString(preferences.getString("storagePath", "") + selectedWard + "mapUpdateHistoryJson", str).apply();
                        preferences.edit().putLong(preferences.getString("storagePath", "") + "" + selectedWard + "mapUpdateHistoryJsonDownloadTime", fileCreationTime).apply();
                        checkDate(selectedWard);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                checkDate(selectedWard);
            }
        });
    }


    @RequiresApi(api = Build.VERSION_CODES.P)
    public void getWardBoundary() {

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        Log.e("Select ward ", selectedWard);
        storageReference.child(preferences.getString("storagePath", "") + "/WardBoundryJson/" + selectedWard + ".json").getMetadata().addOnSuccessListener(storageMetadata -> {
            long fileCreationTime = storageMetadata.getCreationTimeMillis();
            long fileDownloadTime = preferences.getLong(preferences.getString("storagePath", "") + "/WardBoundryJson/" + selectedWard + ".json", 0);
            if (fileDownloadTime != fileCreationTime) {
                storageReference.child(preferences.getString("storagePath", "") + "/WardBoundryJson/" + selectedWard + ".json").getBytes(10000000).addOnSuccessListener(taskSnapshot -> {
                    try {
                        String str = new String(taskSnapshot, StandardCharsets.UTF_8);
                        Log.e("String value ", str);
                        preferences.edit().putString("BoundaryJson", str).apply();
                        preferences.edit().putLong("BoundaryJsonDownloadTime", fileCreationTime).apply();
//                        checkDate(selectedWard);
                        prepareWardBoundary();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
//                checkDate(selectedWard);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void checkDate(String wardNo) {

        try {
            JSONArray jsonArray = new JSONArray(preferences.getString(preferences.getString("storagePath", "") + wardNo + "mapUpdateHistoryJson", ""));
            for (int i = jsonArray.length() - 1; i >= 0; i--) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    Date date1 = format.parse(format.format(new Date()));
                    Date date2 = format.parse(jsonArray.getString(i));
                    if (date1.after(date2)) {
                        preferences.edit().putString("commonReferenceDate", String.valueOf(jsonArray.getString(i))).apply();
                        fileMetaDownload(String.valueOf(jsonArray.getString(i)), wardNo);
                        break;
                    } else if (date1.equals(date2)) {
                        preferences.edit().putString("commonReferenceDate", String.valueOf(jsonArray.getString(i))).apply();
                        fileMetaDownload(String.valueOf(jsonArray.getString(i)), wardNo);
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

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void fileMetaDownload(String dates, String wardNo) {

        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        storageReference.child(preferences.getString("storagePath", "") + "/WardLinesHouseJson/" + wardNo + "/" + dates + ".json").getMetadata().addOnSuccessListener(storageMetadata -> {
            long fileCreationTime = storageMetadata.getCreationTimeMillis();
            long fileDownloadTime = preferences.getLong(preferences.getString("storagePath", "") + wardNo + dates + "DownloadTime", 0);
            if (fileDownloadTime != fileCreationTime) {
                storageReference.child(preferences.getString("storagePath", "") + "/WardLinesHouseJson/" + wardNo + "/" + dates + ".json").getBytes(10000000).addOnSuccessListener(taskSnapshot -> {
                    try {
                        String str = new String(taskSnapshot, StandardCharsets.UTF_8);
                        common.getDatabaseSp(MapActivity.this).edit().putString("wardJSON", str).apply();
                        preferences.edit().putLong(preferences.getString("storagePath", "") + wardNo + dates + "DownloadTime", fileCreationTime).apply();
                        prepareDB();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).addOnFailureListener(Ex -> {
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
            } else {
                prepareDB();
            }
        });
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
                        prepareDB();

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

    private void prepareDB() {

        try {
            JSONObject wardJSONObject = new JSONObject(common.getDatabaseSp(MapActivity.this).getString("wardJSON", ""));
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
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void prepareWardBoundary() {

        try {
            JSONObject wardJSONObject = new JSONObject(preferences.getString("BoundaryJson", ""));
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

            celForLine();
            drawLine();
            fetchMarkerForLine(false);
            common.closeDialog(this);

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
//        rootRef.child("EntityReverification/" + selectedWard + "/" + "CardFound").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.getValue() != null) {
////                    Log.e("EntityReverification", "line nooo " + currentLineNumber);
////                    Log.e("EntityReverification", "line nooo " + snapshot.toString());
//                    if (Objects.equals(snapshot.getKey(), "marksCount")) {
////                        totalMarksTv.setText(String.valueOf(snapshot.getValue()));
//                    }
//                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {
////                        Log.e("EntityReverification", "snapshot1 " + snapshot1.getValue().toString());
////                        Log.e("EntityReverification", "snapshot1 " + snapshot1.getKey());
//                        if (snapshot1.hasChild("lineNo")) {
//                            String line = String.valueOf(snapshot1.child("lineNo").getValue());
//                            if (snapshot1.hasChild("latLng")) {
//                                String[] tempStr = String.valueOf(snapshot1.child("latLng").getValue()).split(",");
//                                double lat = Double.parseDouble(tempStr[0]);
//                                double lng = Double.parseDouble(tempStr[1]);
////                                Log.e("EntityReverification", "snapshot1 " + lat + " " + lng);
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
//                                String currentLine = String.valueOf((currentLineNumber + 1));
//                                if (line.equals(currentLine)) {
////                                    Log.e("EntityReverification", "snapshot1 line" + line);
//                                    mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.gharicon)));
//                                }
////                                }
//                            }
//                        }
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });

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

                        if (isRejectedMarker(snapshot)) {
                            mDMMap.put(new LatLng(lat, lng), new MarkersDataModel(true,
                                    (Boolean) isRejectedMarker(snapshot),
                                    String.valueOf(snapshot.child("date").getValue()),
                                    (String) snapshot.child("image").getValue(),
                                    Integer.parseInt(String.valueOf(snapshot.child("houseType").getValue())),
                                    Integer.parseInt(String.valueOf(snapshot.getKey()))));

                            mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.rejected_marker_icon)));
                        } else {
                            mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.gharicon)));
                        }
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

//        rootRef.child("EntityReverification/" + selectedWard + "/" + "CardDetailNotFound").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
////                common.closeDialog(MapActivity.this);
//                if (snapshot.getValue() != null) {
//                    if (Objects.equals(snapshot.getKey(), "marksCount")) {
////                        totalMarksTv.setText(String.valueOf(snapshot.getValue()));
//                    }
//                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {
//                        Log.e("EntityReverification", "snapshot1 " + snapshot1.getValue().toString());
//                        Log.e("EntityReverification", "snapshot1 " + snapshot1.getKey());
//                        if (snapshot1.hasChild("lineNo")) {
//                            String line = String.valueOf(snapshot1.child("lineNo").getValue());
//                            Log.e("EntityReverification", "snapshot1" + line);
//                            if (snapshot1.hasChild("latLng")) {
//                                String[] tempStr = String.valueOf(snapshot1.child("latLng").getValue()).split(",");
//                                double lat = Double.parseDouble(tempStr[0]);
//                                double lng = Double.parseDouble(tempStr[1]);
//
////                            if (isRejectedMarker(snapshot)) {
////                                mDMMap.put(new LatLng(lat, lng), new MarkersDataModel(Boolean.parseBoolean(snapshot.child("alreadyInstalled").getValue().toString()),
////                                        (Boolean) isRejectedMarker(snapshot),
////                                        String.valueOf(snapshot.child("date").getValue()),
////                                        (String) snapshot.child("image").getValue(),
////                                        Integer.parseInt(String.valueOf(snapshot.child("houseType").getValue())),
////                                        Integer.parseInt(String.valueOf(snapshot.getKey()))));
////
////                                mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.rejected_marker_icon)));
////                            } else {
//                                String currentLine = String.valueOf((currentLineNumber + 1));
//                                if (line.equals(currentLine))
//                                    mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).icon(common.BitmapFromVector(getApplicationContext(), R.drawable.rejected_marker_icon)));
////                            }
//                            }
//                        }
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });

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
        Log.e("Marking Value", "" + snapshot.toString());
        if (snapshot.hasChild("status")) {
            return snapshot.child("status").getValue().equals("Reject");
        }
        return false;
    }

    @SuppressLint("SetTextI18n")
    private void fetchMarkerForLine(boolean isCloseProgressDialog) {

        totalMarksTv.setText("" + 0);
        mDMMap = new HashMap<>();
        enableZoom = true;
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

    private void checkGpsForEntity(String type) {

        LocationServices.getSettingsClient(this).checkLocationSettings(new LocationSettingsRequest.Builder()
                .addLocationRequest(new LocationRequest().setInterval(5000).setFastestInterval(1000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY))
                .setAlwaysShow(true).setNeedBle(true).build()).addOnCompleteListener(task1 -> {
            try {
                task1.getResult(ApiException.class);
                if (task1.isSuccessful()) {
                    openCam("entity_img");
//                    openCam();
//                    pickLocForEntity(type);
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
            updateMarksCount(type);
//            updateMarksCount();
//            saveMarkedLocationAndUploadPhoto(type);
        } else {
            common.showAlertBox("Please Refresh Location", "Ok", "", MapActivity.this);
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void updateMarksCount(String type) {

        common.closeDialog(MapActivity.this);
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
                    common.setProgressDialog("", "checking internet", MapActivity.this, MapActivity.this);
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
                            if (type.equals("road_img")) {
                                showRoadDetailAlert();
                            } else {
                                dialog.dismiss();
                                dialogRoadDetails.dismiss();
                                saveMarkedLocationAndUploadPhoto(type);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else {
                        isPass = true;
                        houseTypeSpinner.setSelection(0);
                        common.showAlertBox("Please Connect to internet", "Ok", "", MapActivity.this);
                    }
                }
            }.execute();
//            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
//                return;
//            }
//            mLastClickTime = SystemClock.elapsedRealtime();
//            showCardDetailAlert();
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

    private void getEntityDetailsAlert(long selectedItemId) {

        View dialogLayout = MapActivity.this.getLayoutInflater().inflate(R.layout.entity_detail_info_layout, null);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this).setView(dialogLayout).setCancelable(false);
        dialogRoadDetails = alertDialog.create();
        edtPerson = dialogLayout.findViewById(R.id.etNoOfPerson);
        edtOwner = dialogLayout.findViewById(R.id.etEntityOwner);
        edtTHouse = dialogLayout.findViewById(R.id.etTotalHouse);

        edtOwner.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(edtOwner, InputMethodManager.SHOW_IMPLICIT);

        edtTHouse.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    Log.e("TAG", "Enter pressed");
                    if (!edtTHouse.getText().toString().equals("")) {
                        int no = Integer.parseInt(edtTHouse.getText().toString());
                        ownerName = edtOwner.getText().toString();
                        if (!ownerName.equals("")) {
                            String person = edtPerson.getText().toString();
                            if (!person.equals("")) {
                                noPerson = Integer.parseInt(edtPerson.getText().toString());
                                if (noPerson > 1) {
                                    if (no > 1) {
                                        checkGpsForEntity("CardNotFound");
                                    } else {
//                                    houseTypeSpinner.setSelection(0);
                                        isPass = true;
                                        common.showAlertBox("Please Enter Total Number of Houses greater than one", "ok", "", MapActivity.this);
                                    }
                                } else {
//                                    houseTypeSpinner.setSelection(0);
                                    isPass = true;
                                    common.showAlertBox("Please Enter Total Number of Person greater than one", "ok", "", MapActivity.this);
                                }
                            } else {
                                isPass = true;
                                common.showAlertBox("Please Enter Total Number of Person greater than one", "ok", "", MapActivity.this);
                            }
                        } else {
                            isPass = true;
                            common.showAlertBox("Please Enter Owner Name", "ok", "", MapActivity.this);
                        }

                    }
                }
                return false;
            }
        });

        if (selectedItemId == 19 || selectedItemId == 20) {
            edtTHouse.setVisibility(View.VISIBLE);
        } else {
            edtTHouse.setVisibility(View.GONE);
        }

        if (selectedItemId == 1 || selectedItemId == 19 || selectedItemId == 25 || selectedItemId == 26 || selectedItemId == 27) {
            edtOwner.setVisibility(View.VISIBLE);
            edtPerson.setVisibility(View.VISIBLE);
            addDetails(selectedItemId);
        } else {
            edtOwner.setVisibility(View.VISIBLE);
            edtPerson.setVisibility(View.GONE);
            addDetails(selectedItemId);
        }

        dialogLayout.findViewById(R.id.imgCloseDialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogRoadDetails.dismiss();
                houseTypeSpinner.setSelection(0);
            }
        });

        dialogRoadDetails.show();
    }

    private void showRoadDetailAlert() {

        View dialogLayout = MapActivity.this.getLayoutInflater().inflate(R.layout.entity_info_layout, null);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MapActivity.this).setView(dialogLayout).setCancelable(false);
        dialogRoadDetails = alertDialog.create();
        roadTypeSp = dialogLayout.findViewById(R.id.road_type_spinner);
        edtRoadWidth = dialogLayout.findViewById(R.id.edtRoadWidth);
        roadImg = dialogLayout.findViewById(R.id.imgRoad);
        roadImg.setImageBitmap(photo);
//        fHouseTypeFromSto();
        prepareRoadSpinner();
        Button btnSubmit = dialogLayout.findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();
                if (roadTypeSp.getSelectedItemId() != 0) {
                    Log.e("houseTypeSpinner", "" + roadTypeSp.getSelectedItemId());
                    long selectedItemId = roadTypeSp.getSelectedItemId();
                    if (!edtRoadWidth.getText().toString().trim().isEmpty()) {
                        submitRoadDetail();

                    } else {
                        edtRoadWidth.setError("Enter road width(In Feet)");
                        edtRoadWidth.requestFocus();
                    }

                } else {
                    Toast.makeText(MapActivity.this, "Please Select road type", Toast.LENGTH_SHORT).show();
                }

            }
        });

        dialogLayout.findViewById(R.id.imgCloseDialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialogRoadDetails.dismiss();
            }
        });

        dialogRoadDetails.show();
    }

    private void submitRoadDetail() {

        /*rootRef.child("EntityMarkingData/WardRoadDetail/"+selectedWard+"/"+(currentLineNumber+1)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                common.closeDialog(PaymentHistoryActivity.this);
                int count = 1;
                if (!snapshot.exists()) {
                    count = 1;
                } else {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Log.e("snapshot", dataSnapshot.getKey() + dataSnapshot.getChildrenCount());
                        count = count + 1;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });*/
        String roadType = roadTypeList.get(roadTypeSp.getSelectedItemPosition());
        String roadWidth = edtRoadWidth.getText().toString().trim();
        String MARKS_COUNT = String.valueOf((currentLineNumber + 1));
        HashMap<String, Object> hM = new HashMap<>();
        hM.put("latLng", lastKnownLatLngForWalkingMan.latitude + "," + lastKnownLatLngForWalkingMan.longitude);
        hM.put("roadType", roadType);
        hM.put("roadWidth", roadWidth);
        hM.put("image", MARKS_COUNT + ".jpg");
        common.setProgressDialog("", "Saving data", MapActivity.this, MapActivity.this);
        rootRef.child("EntityMarkingData/WardRoadDetail/" + selectedWard + "/" + (currentLineNumber + 1)).updateChildren(hM).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                ByteArrayOutputStream toUpload = new ByteArrayOutputStream();
                Bitmap.createScaledBitmap(photo, 400, 600, false)
                        .compress(Bitmap.CompressFormat.JPEG, 80, toUpload);
                FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + selectedCity)
                        .child("/WardRoadImages/" + selectedWard + "/" + (currentLineNumber + 1) + "/" + MARKS_COUNT + ".jpg")
                        .putBytes(toUpload.toByteArray())
                        .addOnSuccessListener((UploadTask.TaskSnapshot taskSnapshot) -> {
                            if (taskSnapshot.getTask().isSuccessful()) {
                                photo = null;
                                enableZoom = true;
                                common.closeDialog(MapActivity.this);
                                dialogRoadDetails.dismiss();
                                AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);

                                // Set the message show for the Alert time
                                builder.setMessage("Road Details submit successfully.");

                                // Set Alert Title
                                builder.setTitle("Road Detail Alert !");

                                // Set Cancelable false for when the user clicks on the outside the Dialog Box then it will remain show
                                builder.setCancelable(false);

                                // Set the Negative button with No name Lambda OnClickListener method is use of DialogInterface interface.
                                builder.setNegativeButton("OK", (DialogInterface.OnClickListener) (dialog, which) -> {
                                    // If user click no then dialog box is canceled.
                                    dialog.cancel();
                                    getWardRoadDetail();
                                });

                                // Create the Alert dialog
                                AlertDialog alertDialog = builder.create();
                                // Show the Alert Dialog box
                                alertDialog.show();

                            }
                        }).addOnFailureListener(e -> common.closeDialog(MapActivity.this));
            }
        });

    }

    private void prepareRoadSpinner() {

        roadTypeList = new ArrayList<>();
        roadTypeList.add("Select road type");
        roadTypeList.add("Single");
        roadTypeList.add("Double");
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(MapActivity.this, android.R.layout.simple_spinner_item, roadTypeList) {
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
        roadTypeSp.setAdapter(spinnerArrayAdapter);
        roadTypeSp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (roadTypeSp.getSelectedItemId() != 0) {
                    Log.e("houseTypeSpinner", "" + roadTypeSp.getSelectedItemId());
                    long selectedItemId = roadTypeSp.getSelectedItemId();

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
    private void saveMarkedLocationAndUploadPhoto(String type) {

        common.closeDialog(MapActivity.this);
        common.setProgressDialog("", "Saving data", MapActivity.this, MapActivity.this);

        try {
            if (photo != null) {
                common.closeDialog(MapActivity.this);
//            common.setProgressDialog("", "Please Wait..", MapActivity.this, MapActivity.this);
                htypee = houseDataHashMap.get(houseTypeSpinner.getSelectedItem());
                if (htypee > 0) {
                    rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1)).child("lastMarkerKey")
                            .runTransaction(new Transaction.Handler() {
                                @NonNull
                                @Override
                                public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                                    if (currentData.getValue() == null) {
                                        currentData.setValue(1);
                                    } else {
                                        currentData.setValue(String.valueOf((Integer.parseInt(currentData.getValue().toString()) + 1)));
                                    }
                                    return Transaction.success(currentData);
                                }

                                @Override
                                public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                                    if (error == null) {
                                        try {
                                            assert currentData != null;
                                            int MARKS_COUNT = Integer.parseInt(String.valueOf(currentData.getValue()));
//                                        htype = houseDataHashMap.get(houseTypeSpinner.getSelectedItem());
                                            Log.e("Marking", "House type select " + htype);
                                            HashMap<String, Object> hM = new HashMap<>();
                                            hM.put("latLng", lastKnownLatLngForWalkingMan.latitude + "," + lastKnownLatLngForWalkingMan.longitude);
                                            hM.put("userId", userId);
                                            hM.put("alreadyInstalled", "");
                                            hM.put("image", MARKS_COUNT + ".jpg");
                                            hM.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
                                            hM.put("houseType", htypee);
                                            hM.put("totalHouses", edtTHouse.getText().toString());
                                            hM.put("ownerName", ownerName);
                                            hM.put("totalPerson", edtPerson.getText().toString());
                                            rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1) + "/" + MARKS_COUNT).setValue(hM);
                                            rootRef.child("EntityMarkingData/LastScanTime/Surveyor").child(userId).setValue(new SimpleDateFormat("dd MMM HH:mm:ss").format(new Date()));
                                            rootRef.child("EntityMarkingData/SurveyorLastLocation").child(userId).setValue(lastKnownLatLngForWalkingMan.latitude + "," + lastKnownLatLngForWalkingMan.longitude);
                                            rootRef.child("EntityMarkingData/LastScanTime/Ward").child(selectedWard).setValue(new SimpleDateFormat("dd MMM HH:mm:ss").format(new Date()));
                                            common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/DateWise/" + date + "/" + userId + "/marked"));
                                            common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1)).child("marksCount"));
                                            common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/EmployeeWise/" + userId + "/" + selectedWard + "/marked"));
                                            common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/DateWise/" + date + "/" + selectedWard + "/marked"));
                                            common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/EmployeeWise/" + userId + "/totalMarked"));
                                            common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/Employee/DateWise/" + date + "/totalMarked"));
                                            common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/DateWise/" + date + "/totalMarked"));
                                            common.increaseCountByOne(rootRef.child("EntityMarkingData/MarkingSurveyData/WardSurveyData/WardWise/" + selectedWard + "/marked"));
                                            houseTypeSpinner.setSelection(0);
                                            edtTHouse.setText("");
                                            edtOwner.setText("");
                                            edtPerson.setText("");
                                            noPerson = 0;
                                            edtTHouse.setVisibility(View.GONE);
                                            edtOwner.setVisibility(View.GONE);
                                            edtPerson.setVisibility(View.GONE);
                                            dateTimeTv.setText(new SimpleDateFormat("dd MMM HH:mm:ss").format(new Date()));

                                            ByteArrayOutputStream toUpload = new ByteArrayOutputStream();
                                            Bitmap.createScaledBitmap(photo, 400, 600, false)
                                                    .compress(Bitmap.CompressFormat.JPEG, 80, toUpload);
                                            FirebaseStorage.getInstance().getReferenceFromUrl("gs://dtdnavigator.appspot.com/" + selectedCity)
                                                    .child("/MarkingSurveyImages/" + selectedWard + "/" + (currentLineNumber + 1) + "/" + MARKS_COUNT + ".jpg")
                                                    .putBytes(toUpload.toByteArray())
                                                    .addOnSuccessListener((UploadTask.TaskSnapshot taskSnapshot) -> {
                                                        if (taskSnapshot.getTask().isSuccessful()) {
                                                            photo = null;
                                                            enableZoom = true;
                                                            common.closeDialog(MapActivity.this);
                                                        }
                                                    }).addOnFailureListener(e -> common.closeDialog(MapActivity.this));

                                        } catch (Exception e) {
                                            photo = null;
                                            enableZoom = true;
                                            common.closeDialog(MapActivity.this);
                                            e.printStackTrace();
                                        }

                                    } else {
                                        houseTypeSpinner.setSelection(0);
                                        common.closeDialog(MapActivity.this);
                                        common.showAlertBox("Please Connect to internet", "Ok", "", MapActivity.this);
                                    }
                                }
                            });
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);

                    // Set the message show for the Alert time
                    builder.setMessage("Entity Type not select please try again!");

                    // Set Alert Title
                    builder.setTitle("Alert !");

                    // Set Cancelable false for when the user clicks on the outside the Dialog Box then it will remain show
                    builder.setCancelable(false);

                    // Set the Negative button with No name Lambda OnClickListener method is use of DialogInterface interface.
                    builder.setNegativeButton("OK", (DialogInterface.OnClickListener) (dialog, which) -> {
                        // If user click no then dialog box is canceled.
                        houseTypeSpinner.setSelection(0);
                        edtTHouse.setText("");
                        edtOwner.setText("");
                        edtPerson.setText("");
//                        edtTHouse.setVisibility(View.GONE);
                        dialog.cancel();
                        dialogRoadDetails.dismiss();
                    });

                    // Create the Alert dialog
                    AlertDialog alertDialog = builder.create();
                    // Show the Alert Dialog box
                    alertDialog.show();
                }
            } else {
                Toast.makeText(MapActivity.this, "Please Click Picture Again", Toast.LENGTH_SHORT).show();
                common.closeDialog(MapActivity.this);
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
                    Log.e("Lat ", lastKnownLatLngForWalkingMan.latitude + " Lng " + lastKnownLatLngForWalkingMan.longitude);
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

    private void openCam(String type) {

        if (type.equals("road_img")) {

            new Handler().post(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (MapActivity.this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
                    } else {
                        showAlertDialog(type);
                    }
                }
            });

        } else {
            new Handler().post(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (MapActivity.this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_CODE);
                    } else {
                        showAlertDialog(type);
                    }
                }
            });
        }
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
    public void showAlertDialog(String type) {

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
                        pickLocForEntity(type);
//                        updateMarksCount(type);

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
                        getWardRoadDetail();
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
                        getWardRoadDetail();
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
                                            if (bottomRoadLayout.getVisibility() == View.VISIBLE) {
                                                common.closeDialog(MapActivity.this);
                                                showAlertMsgDialog();
//                                              Toast.makeText(MapActivity.this, "Please enter current line road details first", Toast.LENGTH_SHORT).show();
                                            } else {
                                                currentLineNumber = lineNumber - 1;
                                                drawLine();
                                                fetchMarkerForLine(true);
                                                getWardRoadDetail();
                                            }
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

    public void showAlertMsgDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);

        // Set the message show for the Alert time
        builder.setMessage("कृपया पहले वर्तमान लाइन नंबर पर सड़क का विवरण दर्ज करें");

        // Set Alert Title
        builder.setTitle("Road Detail Alert !");

        // Set Cancelable false for when the user clicks on the outside the Dialog Box then it will remain show
        builder.setCancelable(false);

        // Set the Negative button with No name Lambda OnClickListener method is use of DialogInterface interface.
        builder.setNegativeButton("OK", (DialogInterface.OnClickListener) (dialog, which) -> {
            // If user click no then dialog box is canceled.
            dialog.cancel();
            getWardRoadDetail();
        });

        // Create the Alert dialog
        AlertDialog alertDialog = builder.create();
        // Show the Alert Dialog box
        alertDialog.show();
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
            RadioButton rb_yes = diaLayout.findViewById((R.id.is_surveyed_true_rb_for_rejected_marker));
            RadioButton rb_no = diaLayout.findViewById((R.id.is_surveyed_false_rb_for_rejected_marker));
            imageViewForRejectedMarker = diaLayout.findViewById(R.id.rejected_marker_image_preview);
            Button imageButton = diaLayout.findViewById(R.id.re_click_picture);
            textView.setText(cbText);

            rb_yes.setOnClickListener(view -> {
                rb_yes.setChecked(true);
                rb_no.setChecked(false);
            });

            rb_no.setOnClickListener(view -> {
                rb_yes.setChecked(false);
                rb_no.setChecked(true);
            });

            if ((mdm.isAlreadyInstalled())) {
                rb_yes.setChecked(true);
            } else {
                rb_no.setChecked(true);
            }

            imageButton.setOnClickListener(view -> {
                if (isPass) {
                    isPass = false;
                    isEdit = true;
                    openCam("entity_img");
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
                                    mapTemp.put("alreadyInstalled", rb_yes.isChecked());
                                    mapTemp.put("image", mdm.getImageName());
                                    mapTemp.put("modifiedDate", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
                                    mapTemp.put("status", "Re-marked");
                                    mapTemp.put("houseType", houseDataHashMap.get(spinner.getSelectedItem()));
                                    mapTemp.put("totalHouses", edtTHouse.getText().toString());
                                    rootRef.child("EntityMarkingData/MarkedHouses/" + selectedWard + "/" + (currentLineNumber + 1) + "/" + mdm.getMarkerNumber()).updateChildren(mapTemp);

                                    int temp = Boolean.compare(mdm.isAlreadyInstalled(), rb_yes.isChecked());
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
                    showAlertDialog("entity_img");
                } else {
                    houseTypeSpinner.setSelection(0);
                    isPass = true;
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    mainCheckLocationForRealTimeRequest();
                }
            }

            if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    showAlertDialog("road_img");
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
                    openCam("entity_img");
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
        rootRef.child("Settings/LatestVersions/entityMarking").addListenerForSingleValueEvent(new ValueEventListener() {
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

}