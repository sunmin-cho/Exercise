package org.androidtown.ppppp.exercise;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.GenericTypeIndicator;

import org.androidtown.ppppp.R;

import java.util.ArrayList;
import java.util.List;

public class ExerciseList extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ExerciseAdapter adapter;
    private List<ExerciseModel> exerciseList;
    private TextView resultView;
    private DatabaseReference database;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sub);

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        uid = prefs.getString("uid", null);

        if (uid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        resultView = findViewById(R.id.result_text);
        recyclerView = findViewById(R.id.recommendation_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        exerciseList = new ArrayList<>();
        adapter = new ExerciseAdapter(this, exerciseList); // uid 사용 X → OK
        recyclerView.setAdapter(adapter);

        String query = getIntent().getStringExtra("search_query");
        if (query != null) {
            query = query.trim().toLowerCase();
            resultView.setText(query);
            saveRecentSearch(query);
            fetchDataFromFirebase(query);
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }


    private void saveRecentSearch(String query) {;
        DatabaseReference recentSearchRef = FirebaseDatabase.getInstance()
                .getReference("user_recent_searches")
                .child(uid);
        recentSearchRef.push().setValue(query);
    }

    private void fetchDataFromFirebase(String categoryQuery) {
        database = FirebaseDatabase.getInstance().getReference("exercise");

        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                exerciseList.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String title = child.getKey();
                    String description = child.child("description").getValue(String.class);
                    String videoUrl = child.child("videoUrl").getValue(String.class);

                    // ✅ category: 문자열 또는 배열 모두 처리
                    List<String> categoryList = new ArrayList<>();
                    DataSnapshot categorySnap = child.child("category");

                    if (categorySnap.getValue() instanceof String) {
                        String category = categorySnap.getValue(String.class);
                        if (category != null) {
                            categoryList.add(category.toLowerCase());
                        }
                    } else if (categorySnap.exists()) {
                        GenericTypeIndicator<List<String>> t = new GenericTypeIndicator<List<String>>() {};
                        List<String> temp = categorySnap.getValue(t);
                        if (temp != null) {
                            for (String cat : temp) {
                                if (cat != null) {
                                    categoryList.add(cat.toLowerCase());
                                }
                            }
                        }
                    }

                    // 검색어 포함 여부 확인
                    if (categoryList.contains(categoryQuery)) {
                        ExerciseModel exercise = new ExerciseModel();
                        exercise.setTitle(title);
                        exercise.setCategory(categoryList); // ✅ List<String>로 저장
                        exercise.setDescription(description);
                        exercise.setVideoUrl(videoUrl);

                        exerciseList.add(exercise);
                    }
                }

                if (exerciseList.isEmpty()) {
                    Toast.makeText(ExerciseList.this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show();
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ExerciseList.this, "데이터 로드 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
