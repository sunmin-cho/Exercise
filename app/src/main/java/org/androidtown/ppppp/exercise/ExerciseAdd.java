package org.androidtown.ppppp.exercise;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.androidtown.ppppp.R;

import java.util.*;

public class ExerciseAdd extends AppCompatActivity {

    private EditText titleEditText, categoryEditText, descriptionEditText, videoUrlEditText;
    private Button btnAddExercise;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_exercise);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // ← 버튼 활성화
        }

        // 뷰 연결
        titleEditText = findViewById(R.id.title);
        categoryEditText = findViewById(R.id.category);
        descriptionEditText = findViewById(R.id.description);
        videoUrlEditText = findViewById(R.id.videoUrl);
        btnAddExercise = findViewById(R.id.btnAddExercise);

        // Firebase Realtime Database 참조
        databaseRef = FirebaseDatabase.getInstance().getReference("exercise");

        // 운동 추가 버튼 클릭
        btnAddExercise.setOnClickListener(v -> {
            String title = titleEditText.getText().toString().trim();
            String categoryInput = categoryEditText.getText().toString().trim();
            String description = descriptionEditText.getText().toString().trim();
            String videoUrl = videoUrlEditText.getText().toString().trim();

            if (title.isEmpty() || categoryInput.isEmpty() || description.isEmpty() || videoUrl.isEmpty()) {
                Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔧 title → Firebase 키로 안전하게 변환
            String key = title.replaceAll("[.#$\\[\\]/]", "_");

            // ✅ 카테고리 문자열 → 리스트로 변환 (예: "코어,하체" → ["코어", "하체"])
            List<String> categoryList = new ArrayList<>();
            for (String cat : categoryInput.split(",")) {
                String trimmed = cat.trim();
                if (!trimmed.isEmpty()) categoryList.add(trimmed);
            }

            // 🔧 Firebase에 저장할 데이터 구성
            Map<String, Object> exerciseData = new HashMap<>();
            exerciseData.put("category", categoryList); // ✅ 리스트로 저장
            exerciseData.put("description", description);
            exerciseData.put("videoUrl", videoUrl);

            // 🔄 Firebase에 업로드
            databaseRef.child(key).setValue(exerciseData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "운동이 저장되었습니다!", Toast.LENGTH_SHORT).show();
                        titleEditText.setText("");
                        categoryEditText.setText("");
                        descriptionEditText.setText("");
                        videoUrlEditText.setText("");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    // 툴바 뒤로가기 버튼 동작
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
