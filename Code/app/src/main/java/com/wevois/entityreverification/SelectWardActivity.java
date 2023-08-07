package com.wevois.entityreverification;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.storage.FirebaseStorage;

import org.json.JSONArray;
import org.json.JSONException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class SelectWardActivity extends AppCompatActivity {

    Spinner spCity, spWard;
    SharedPreferences preferences;
    String selectCity,selectWard;
    TextView tvWard;
    Button btnContinue;
    ArrayList<String> cityList = new ArrayList<>();
    ArrayList<String> wardList = new ArrayList<>();
    CommonFunctions common = new CommonFunctions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_ward_layout);
        init();

    }

    public void init() {

        preferences = getSharedPreferences("LoginDetails", MODE_PRIVATE);
        tvWard = findViewById(R.id.tv_ward);
        btnContinue = findViewById(R.id.btnContinue);
        spCity = findViewById(R.id.sp_city);
        spWard = findViewById(R.id.sp_ward);
        setCitySpinner();

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SelectWardActivity.this, MapActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }

    private void getAllWard() {

        FirebaseStorage.getInstance().getReferenceFromUrl("gs://entity-verification.appspot.com/" + selectCity + "/Default/AvailableWard.json").getBytes(10000000).addOnSuccessListener(taskSnapshot -> {
            String city = new String(taskSnapshot, StandardCharsets.UTF_8);
            try {
                JSONArray jsonArray = new JSONArray(city);
                wardList.clear();
                for (int i = 0; i < jsonArray.length(); i++) {
                    Log.e("wardList", jsonArray.get(i).toString() + "");
                    if (!jsonArray.get(i).toString().equalsIgnoreCase("null")) {
                        wardList.add(jsonArray.get(i).toString());
//                        Log.e("wardList", wardList.size() + "" + wardList.get(i));
                        preferences.edit().putString("wardList", wardList.toString()).apply();
                    }
                }
                common.closeDialog(SelectWardActivity.this);
                bindWardToSpinner();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }).addOnFailureListener(exception -> {
        });

    }


    private void bindWardToSpinner() {

        tvWard.setVisibility(View.VISIBLE);
        spWard.setVisibility(View.VISIBLE);
        wardList.clear();
        wardList.add("Select Ward");
        String listAsString = preferences.getString("wardList", null);
        Log.e("list", listAsString);
        String[] reasonString = listAsString.substring(1, listAsString.length() - 1).split(",");
        for (String s : reasonString) {
            String reasonType = s.replace(" ", "");
            if (!reasonType.contains("Helper") && !reasonType.contains("Thir") && !reasonType.contains("WetWaste") && !reasonType.contains("Compactor")) {
                wardList.add(reasonType);
            }

        }

        final ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, wardList) {
            @Override
            public boolean isEnabled(int position) {
                return !(position == 0);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(position == 0 ? Color.GRAY : Color.BLACK);
                return view;
            }
        };
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spWard.setAdapter(spinnerArrayAdapter);
        spWard.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (spWard.getSelectedItemId() != 0) {
                    Log.e("wardSpinner", "" + spWard.getSelectedItemPosition());
                    int selectedItemId = spWard.getSelectedItemPosition();
                    Log.e("city name", wardList.get(selectedItemId));
                    selectWard = wardList.get(selectedItemId);
                    preferences.edit().putString("ward", selectWard).apply();
                    btnContinue.setVisibility(View.VISIBLE);

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void setCitySpinner() {

        cityList.add("Select city");
        cityList.add("Malviyanagar");
        cityList.add("Murlipura");
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(SelectWardActivity.this, android.R.layout.simple_spinner_item, cityList) {
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
        spCity.setAdapter(spinnerArrayAdapter);
        spCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (spCity.getSelectedItemId() != 0) {
                    Log.e("houseTypeSpinner", "" + spCity.getSelectedItemPosition());
                    int selectedItemId = spCity.getSelectedItemPosition();
                    Log.e("city name", cityList.get(selectedItemId));
                    selectCity = cityList.get(selectedItemId);
                    preferences.edit().putString("city", selectCity).apply();
                    if(selectCity.equals("Malviyanagar")){
                        preferences.edit().putString("prefix", "MNZ").apply();
                    }else if (selectCity.equals("Murlipura")){
                        preferences.edit().putString("prefix", "MPZ").apply();
                    }
                    setDatabase(selectCity);
                    common.setProgressDialog("Please Wait..", " ", SelectWardActivity.this, SelectWardActivity.this);
                    getAllWard();

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    void setDatabase(String city) {
        String path = common.getDatabase(city);
        preferences.edit().putString("dbPath", path).apply();
        preferences.edit().putString("storagePath", city).apply();
    }
}
