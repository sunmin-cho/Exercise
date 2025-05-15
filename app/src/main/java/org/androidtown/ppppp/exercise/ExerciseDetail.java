package org.androidtown.ppppp.exercise;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.*;

import org.androidtown.ppppp.R;

import java.util.*;

public class ExerciseDetail extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private String uid;
    private static final int MAX_RECENT_VIEWS = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_detail);

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        uid = prefs.getString("uid", null);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        TextView detailTextView = findViewById(R.id.exercise_detail_text);
        TextView descriptionTextView = findViewById(R.id.exercise_description);
        WebView youtubeWebView = findViewById(R.id.youtube_video);

        firestore = FirebaseFirestore.getInstance();

        String exerciseName = getIntent().getStringExtra("exercise_name");
        if (exerciseName != null) {
            detailTextView.setText(exerciseName);

            saveRecentView(exerciseName); // ✅ Firestore에 최근 조회 저장
            loadExerciseInfoFromRealtimeDB(exerciseName, descriptionTextView, youtubeWebView);
        }
    }

    private void saveRecentView(String exerciseName) {

        if (uid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference recentViewRef = firestore
                .collection("user_recent_views")
                .document(uid)
                .collection("views");

        Map<String, Object> viewData = new HashMap<>();
        viewData.put("exerciseName", exerciseName);
        viewData.put("timestamp", FieldValue.serverTimestamp());

        recentViewRef.add(viewData)
                .addOnSuccessListener(documentReference -> pruneRecentViewsIfNeeded(recentViewRef))
                .addOnFailureListener(e -> Toast.makeText(this, "최근 조회 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void pruneRecentViewsIfNeeded(CollectionReference recentViewRef) {
        recentViewRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    if (count > MAX_RECENT_VIEWS) {
                        int itemsToDelete = count - MAX_RECENT_VIEWS;

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            if (itemsToDelete-- <= 0) break;
                            doc.getReference().delete();
                        }
                    }
                });
    }

    private void loadExerciseInfoFromRealtimeDB(String exerciseName, TextView descriptionView, WebView videoWebView) {
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("exercise");

        database.child(exerciseName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String description = snapshot.child("description").getValue(String.class);
                            String videoUrl = snapshot.child("videoUrl").getValue(String.class);

                            descriptionView.setText(description != null ? description : "설명이 없습니다.");

                            if (videoUrl != null) {
                                videoWebView.getSettings().setJavaScriptEnabled(true);
                                videoWebView.setWebViewClient(new WebViewClient());
                                videoWebView.loadUrl(videoUrl);
                            }
                        } else {
                            descriptionView.setText("운동 정보가 없습니다.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ExerciseDetail.this, "데이터 불러오기 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
