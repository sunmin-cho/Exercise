package org.androidtown.ppppp.exercise;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.firestore.*;

import org.androidtown.ppppp.R;

import java.util.*;

public class RecommendedExercisesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyTextView;
    private String uid;

    private FirebaseFirestore firestore;
    private DatabaseReference exerciseRef; // 운동 정보는 Realtime DB 사용

    private List<ExerciseModel> recommendedList = new ArrayList<>();
    private ExerciseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommended_exercises);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recommend_recyclerView);
        emptyTextView = findViewById(R.id.empty_recommend_text);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        uid = prefs.getString("uid", null);

        if (uid == null) {
            emptyTextView.setText("로그인이 필요합니다.");
            emptyTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        firestore = FirebaseFirestore.getInstance();
        exerciseRef = FirebaseDatabase.getInstance().getReference("exercise");

        adapter = new ExerciseAdapter(this, recommendedList);
        recyclerView.setAdapter(adapter);

        loadRecommendations();
    }


    private void loadRecommendations() {
        CollectionReference favRef = firestore.collection("user_favorites").document(uid).collection("favorites");
        CollectionReference viewRef = firestore.collection("user_recent_views").document(uid).collection("views");
        CollectionReference searchRef = firestore.collection("user_recent_searches").document(uid).collection("searches");

        Map<String, Integer> scoreMap = new HashMap<>();
        Set<String> favorites = new HashSet<>();
        Set<String> recentlyViewed = new HashSet<>();
        List<String> recentSearchQueries = new ArrayList<>();

        favRef.get().addOnSuccessListener(favSnap -> {
            for (DocumentSnapshot doc : favSnap.getDocuments()) {
                favorites.add(doc.getId());
            }

            viewRef.get().addOnSuccessListener(viewSnap -> {
                for (DocumentSnapshot doc : viewSnap.getDocuments()) {
                    String exerciseName = doc.getString("exerciseName");
                    if (exerciseName != null) recentlyViewed.add(exerciseName);
                }

                searchRef.get().addOnSuccessListener(searchSnap -> {
                    for (DocumentSnapshot doc : searchSnap.getDocuments()) {
                        String query = doc.getString("query");
                        if (query != null) recentSearchQueries.add(query);
                    }

                    exerciseRef.get().addOnSuccessListener(exSnap -> {
                        for (DataSnapshot item : exSnap.getChildren()) {
                            String title = item.getKey();
                            // ✅ category: String 또는 List<String> 모두 대응
                            List<String> categoryList = new ArrayList<>();
                            Object rawCategory = item.child("category").getValue();
                            if (rawCategory instanceof List) {
                                categoryList = (List<String>) rawCategory;
                            } else if (rawCategory instanceof String) {
                                categoryList.add((String) rawCategory);
                            }
                            String description = item.child("description").getValue(String.class);
                            String videoUrl = item.child("videoUrl").getValue(String.class);

                            int score = 0;
                            if (favorites.contains(title)) score += 3;
                            if (recentlyViewed.contains(title)) score += 2;

                            // ✅ categoryList에 검색어가 포함되어 있는지 비교
                            for (String searched : recentSearchQueries) {
                                for (String category : categoryList) {
                                    if (category.equalsIgnoreCase(searched)) {
                                        score += 1;
                                        break;
                                    }
                                }
                            }

                            if (score > 0) {
                                ExerciseModel ex = new ExerciseModel();
                                ex.setTitle(title);
                                ex.setCategory(categoryList != null ? categoryList : new ArrayList<>());
                                ex.setDescription(description);
                                ex.setVideoUrl(videoUrl);

                                recommendedList.add(ex);
                                scoreMap.put(title, score);
                            }
                        }

                        if (recommendedList.isEmpty()) {
                            emptyTextView.setVisibility(View.VISIBLE);
                        } else {
                            emptyTextView.setVisibility(View.GONE);

                            // 점수 높은 순 정렬
                            Collections.sort(recommendedList, (a, b) -> {
                                int scoreA = scoreMap.getOrDefault(a.getTitle(), 0);
                                int scoreB = scoreMap.getOrDefault(b.getTitle(), 0);
                                return Integer.compare(scoreB, scoreA);
                            });

                            adapter.notifyDataSetChanged();
                        }
                    });
                });
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
