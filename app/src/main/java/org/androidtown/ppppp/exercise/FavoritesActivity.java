package org.androidtown.ppppp.exercise;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.androidtown.ppppp.R;
import org.androidtown.ppppp.exercise.ExerciseAdapter;
import org.androidtown.ppppp.exercise.ExerciseModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView favoriteRecyclerView;
    private TextView emptyTextView;
    private String uid;

    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        favoriteRecyclerView = findViewById(R.id.favoriteRecyclerView);
        emptyTextView = findViewById(R.id.emptyTextView);
        favoriteRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        uid = prefs.getString("uid", null);

        if (uid == null) {
            emptyTextView.setText("로그인이 필요합니다.");
            emptyTextView.setVisibility(View.VISIBLE);
            favoriteRecyclerView.setVisibility(View.GONE);
            return;
        }

        loadFavoriteExercises();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }


    private void loadFavoriteExercises() {
        CollectionReference favRef = firestore.collection("user_favorites").document(uid).collection("favorites");
        DatabaseReference exerciseRef = FirebaseDatabase.getInstance().getReference("exercise"); // ✅ 수정

        favRef.get().addOnSuccessListener(favSnap -> {
            if (favSnap.isEmpty()) {
                emptyTextView.setVisibility(View.VISIBLE);
                favoriteRecyclerView.setVisibility(View.GONE);
                return;
            }

            List<ExerciseModel> favoriteList = new ArrayList<>();
            List<String> favoriteTitles = new ArrayList<>();
            for (DocumentSnapshot doc : favSnap.getDocuments()) {
                favoriteTitles.add(doc.getId()); // 즐겨찾기 제목
            }

            exerciseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot item : snapshot.getChildren()) {
                        String title = item.getKey();
                        if (favoriteTitles.contains(title)) {
                            String description = item.child("description").getValue(String.class);
                            String videoUrl = item.child("videoUrl").getValue(String.class);
                            Object rawCategory = item.child("category").getValue();
                            List<String> categoryList = new ArrayList<>();

                            if (rawCategory instanceof String) {
                                categoryList.add((String) rawCategory);
                            } else if (rawCategory instanceof List) {
                                categoryList = (List<String>) rawCategory;
                            }

                            ExerciseModel ex = new ExerciseModel();
                            ex.setTitle(title);
                            ex.setCategory(categoryList);  // ✅ List로 저장
                            ex.setDescription(description);
                            ex.setVideoUrl(videoUrl);


                            favoriteList.add(ex);
                        }
                    }

                    if (favoriteList.isEmpty()) {
                        emptyTextView.setVisibility(View.VISIBLE);
                        favoriteRecyclerView.setVisibility(View.GONE);
                    } else {
                        emptyTextView.setVisibility(View.GONE);
                        favoriteRecyclerView.setVisibility(View.VISIBLE);
                        favoriteRecyclerView.setAdapter(new ExerciseAdapter(FavoritesActivity.this, favoriteList));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    emptyTextView.setText("운동 정보를 불러오지 못했습니다: " + error.getMessage());
                    emptyTextView.setVisibility(View.VISIBLE);
                    favoriteRecyclerView.setVisibility(View.GONE);
                }
            });
        });
    }


    @Override
    public boolean onSupportNavigateUp() {
        finish(); // 현재 액티비티 종료 (뒤로 가기)
        return true;
    }
}
