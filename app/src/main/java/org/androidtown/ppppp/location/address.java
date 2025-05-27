/*
package com.example.gymtest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import android.location.Address;
import android.location.Geocoder;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.location.LocationRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class address extends AppCompatActivity {
    private static final int PICK_IMAGE = 1;
    String kakaoKey = BuildConfig.KAKAO_API_KEY;
    String googleKey = BuildConfig.GOOGLE_MAP_API_KEY;

    private EditText nEtAddress;
    private Button btnLoadLocation, btnCheckMatch;
    private ImageView imageView;
    private TextView resultText, locationTextView;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private FusedLocationProviderClient fusedLocationClient;
    private String extractedAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);

        nEtAddress = findViewById(R.id.et_address);
        btnCheckMatch = findViewById(R.id.btnCheckMatch);
        btnLoadLocation = findViewById(R.id.btnLoadLocation);
        imageView = findViewById(R.id.imageView);
        resultText = findViewById(R.id.resultText);
        locationTextView = findViewById(R.id.locationText);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnLoadLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestLocationPermission(); // 위치 권한 확인 후 위치 가져오기 실행
                locationTextView.setVisibility(View.VISIBLE);
            }
        });

        //btnLoadLocation(v -> );
        btnCheckMatch.setOnClickListener(v -> compareAddresses());

        //block touch
        nEtAddress.setFocusable(false);

        nEtAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(address.this, SearchActivity.class);
                getSearchResult.launch(intent);
            }
        });
    }

    private void getCoordinatesFromAddress(String address) {
        String apiKey = googleKey; // ← 본인의 키로 변경
        String urlString = "https://maps.googleapis.com/maps/api/geocode/json?address="
                + Uri.encode(address) + "&key=" + apiKey;

        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonObject = new JSONObject(response.toString());
                JSONArray results = jsonObject.getJSONArray("results");

                if (results.length() > 0) {
                    JSONObject location = results.getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONObject("location");

                    double lat = location.getDouble("lat");
                    double lng = location.getDouble("lng");

                    runOnUiThread(() -> {
                        Toast.makeText(this, "위도: " + lat + "\n경도: " + lng, Toast.LENGTH_LONG).show();
                        Log.d("Geocode", "Lat: " + lat + ", Lng: " + lng);
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "결과가 없습니다.", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "오류 발생: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
                e.printStackTrace();
            }
        }).start();
    }

    private final ActivityResultLauncher<Intent> getSearchResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Search Activity로부터의 결과 값이 이곳으로 전달
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        String data = result.getData().getStringExtra("data");

                        nEtAddress.setText(data);
                    }
                }
            }
    );

    private void requestLocationPermission() {
        Log.d("TAG", "requestLocationPermission 실행");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }
    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        Log.d("TAG", "getCurrentLocation 실행");
        fusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    Location location = task.getResult();
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    Log.d("TAG", "longitude: "+longitude+"latitude: "+latitude);
                    //getAddressFromLocation(latitude, longitude);
                    getRoadAddressFromLocation(latitude, longitude);
                } else {
                    // 위치 정보가 없으면 실시간 요청
                    requestNewLocationData();
                }
            }
        });
    }
    // 실시간 위치 요청
    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000); // 5초마다 업데이트

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d("TAG", "onLocationResult 실행");
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    //getAddressFromLocation(latitude, longitude);
                    Log.d("requestNewLocationData", "longitude: "+longitude+"latitude: "+latitude);
                    getRoadAddressFromLocation(latitude, longitude);
                    fusedLocationClient.removeLocationUpdates(this); // 업데이트 중지
                }
            }
        }, Looper.getMainLooper());
    }

    private void getRoadAddressFromLocation(double latitude, double longitude) {
        Log.d("TAG", "getRoadAddressFromLocation 실행");
        String apiKey = kakaoKey; // 발급받은 카카오 API 키 입력
        String url = "https://dapi.kakao.com/v2/local/geo/coord2address.json?x=" + longitude + "&y=" + latitude;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "KakaoAK " + apiKey)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                Log.e("TAG", "API 호출 실패", e);
                locationTextView.setText("주소 변환 실패");
                runOnUiThread(() -> locationTextView.setText("주소 변환 실패"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    // 👉 `string()`을 한 번만 호출하고 변수에 저장
                    String responseData = response.body().string();

                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray documents = jsonObject.getJSONArray("documents");

                        if (documents.length() > 0) {
                            JSONObject addressInfo = documents.getJSONObject(0);
                            JSONObject roadAddress = addressInfo.optJSONObject("road_address");

                            String fullAddress;
                            if (roadAddress != null) {
                                fullAddress = roadAddress.getString("address_name"); // 도로명 주소
                                Log.d("TAG", "도로명주소 반환");
                            } else {
                                fullAddress = addressInfo.getJSONObject("address").getString("address_name"); // 지번 주소
                                Log.d("TAG", "지번 반환");
                            }

                            String finalAddress = fullAddress;
                            extractedAddress = fullAddress;
                            Log.d("Extracted Address", "onResponse: " + fullAddress);

                            runOnUiThread(() -> locationTextView.setText("현재 위치: " + finalAddress));
                        } else {
                            runOnUiThread(() -> locationTextView.setText("도로명 주소를 찾을 수 없습니다."));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> locationTextView.setText("주소 파싱 실패"));
                    }
                } else {
                    Log.e("API_CALL", "API 호출 실패, 응답 코드: " + response.code());
                    Log.e("API_CALL", "API 에러 응답: " + response.body().string()); // 여기서도 한 번만 호출해야 함
                }
            }

        });
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void compareAddresses() {
        String userInputAddress = nEtAddress.getText().toString();
        Log.d("userInputAddress", userInputAddress);
        resultText.setVisibility(View.VISIBLE);
        if (userInputAddress.isEmpty() || extractedAddress == null) {
            resultText.setText("주소를 입력하고 현재 위치를 확인하세요.");
            return;
        }

        if (userInputAddress.equalsIgnoreCase(extractedAddress)) {
            resultText.setText("현재 위치와 입력 주소가 일치합니다!");
        } else {
            resultText.setText("현재 위치와 입력 주소가 일치하지 않습니다!");
        }
    }
}*/

package org.androidtown.ppppp.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;

import org.androidtown.ppppp.BuildConfig;

import org.androidtown.ppppp.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class address extends AppCompatActivity {
    private static final int PICK_IMAGE = 1;
    String kakaoKey = BuildConfig.KAKAO_API_KEY;
    String googleKey = BuildConfig.GOOGLE_MAP_API_KEY;

    private EditText nEtAddress;
    private Button btnLoadLocation, btnCheckMatch;
    private ImageView imageView;
    private TextView resultText, locationTextView;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private FusedLocationProviderClient fusedLocationClient;
    private String extractedAddress;
    private double EtLongitude, EtLatitude;
    private double UserLongitude, UserLatitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);

        nEtAddress = findViewById(R.id.et_address);
        btnCheckMatch = findViewById(R.id.btnCheckMatch);
        btnLoadLocation = findViewById(R.id.btnLoadLocation);
        imageView = findViewById(R.id.imageView);
        resultText = findViewById(R.id.resultText);
        locationTextView = findViewById(R.id.locationText);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnLoadLocation.setOnClickListener(v -> {
            requestLocationPermission();
            locationTextView.setVisibility(View.VISIBLE);
        });

        btnCheckMatch.setOnClickListener(v -> compareAddresses());

        nEtAddress.setFocusable(false);
        nEtAddress.setOnClickListener(v -> {
            Intent intent = new Intent(address.this, SearchActivity.class);
            getSearchResult.launch(intent);
        });
    }

    private void getCoordinatesFromAddress(String address) {
        String apiKey = googleKey;
        String urlString = "https://maps.googleapis.com/maps/api/geocode/json?address="
                + Uri.encode(address) + "&key=" + apiKey;

        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonObject = new JSONObject(response.toString());
                JSONArray results = jsonObject.getJSONArray("results");

                if (results.length() > 0) {
                    JSONObject location = results.getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONObject("location");

                    double lat = location.getDouble("lat");
                    double lng = location.getDouble("lng");
                    EtLatitude=lat;
                    EtLongitude=lng;

                    runOnUiThread(() -> nEtAddress.setText("위도: " + lat + ", 경도: " + lng));
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "결과가 없습니다.", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "오류 발생: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
                e.printStackTrace();
            }
        }).start();
    }

    private final ActivityResultLauncher<Intent> getSearchResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String data = result.getData().getStringExtra("data");
                    if (data != null) {
                        nEtAddress.setText(data);
                        getCoordinatesFromAddress(data);
                    }
                }
            }
    );

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Location location = task.getResult();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                UserLongitude = longitude;
                UserLatitude = latitude;
                locationTextView.setText("현재 위도: " + latitude + ", 경도: " + longitude);
            } else {
                requestNewLocationData();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    locationTextView.setText("현재 위도: " + latitude + ", 경도: " + longitude);
                    getRoadAddressFromLocation(latitude, longitude);
                    fusedLocationClient.removeLocationUpdates(this);
                }
            }
        }, Looper.getMainLooper());
    }

    private void getRoadAddressFromLocation(double latitude, double longitude) {
        String apiKey = kakaoKey;
        String url = "https://dapi.kakao.com/v2/local/geo/coord2address.json?x=" + longitude + "&y=" + latitude;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "KakaoAK " + apiKey)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> locationTextView.setText("주소 변환 실패"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray documents = jsonObject.getJSONArray("documents");

                        if (documents.length() > 0) {
                            JSONObject addressInfo = documents.getJSONObject(0);
                            JSONObject roadAddress = addressInfo.optJSONObject("road_address");
                            String fullAddress;
                            if (roadAddress != null) {
                                fullAddress = roadAddress.getString("address_name");
                            } else {
                                fullAddress = addressInfo.getJSONObject("address").getString("address_name");
                            }
                            extractedAddress = fullAddress;
                            runOnUiThread(() -> locationTextView.setText("현재 위치 주소: " + fullAddress));
                        } else {
                            runOnUiThread(() -> locationTextView.setText("도로명 주소를 찾을 수 없습니다."));
                        }
                    } catch (JSONException e) {
                        runOnUiThread(() -> locationTextView.setText("주소 파싱 실패"));
                    }
                } else {
                    runOnUiThread(() -> locationTextView.setText("API 호출 실패: " + response.code()));
                }
            }
        });
    }

    private void compareAddresses() {
        // 구현된 주소 비교 로직 (사용자 정의)
        // 지구 반지름 (단위: km)

        double EARTH_RADIUS = 6371.0;
        double dLat = Math.toRadians(EtLatitude - UserLatitude);
        double dLon = Math.toRadians(EtLongitude - UserLongitude);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(UserLatitude)) * Math.cos(Math.toRadians(EtLatitude)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = EARTH_RADIUS * c * 1000; // 거리 (단위: km)
        if (distance < 100)
            resultText.setText("기준위치와 100m 이내에 위치해 있습니다. 출석 인정이 됩니다.");
        else
            resultText.setText("기준위치와 100m 이상 떨어져 있습니다. 출석 인정이 되지 않습니다.");
        resultText.setVisibility(View.VISIBLE);
    }

    private void checkAndMarkAttendance() {
        String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String uid = getSharedPreferences("userPrefs", MODE_PRIVATE).getString("uid", null);

        if (uid == null) {
            Toast.makeText(this, "사용자 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("attendance")
                .document(uid)
                .collection("records")
                .document(todayDate)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Toast.makeText(this, "이미 출석 완료한 상태입니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Map<String, Object> data = new HashMap<>();
                        data.put("timestamp", com.google.firebase.Timestamp.now());

                        db.collection("attendance")
                                .document(uid)
                                .collection("records")
                                .document(todayDate)
                                .set(data)
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this, "🎉 출석이 기록되었습니다!", Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "출석 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    }
                });
    }
}
