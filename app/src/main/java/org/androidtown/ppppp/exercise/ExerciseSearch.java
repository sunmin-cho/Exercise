package org.androidtown.ppppp.exercise;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.*;

import org.androidtown.ppppp.R;

import java.util.HashMap;
import java.util.Map;

public class ExerciseSearch extends AppCompatActivity {

    private static final int MAX_RECENT_SEARCHES = 20;
    private FirebaseFirestore firestore;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.searchpage);

        firestore = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        uid = prefs.getString("uid", null);

        if (uid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        EditText searchBar = findViewById(R.id.search_bar);

        searchBar.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {

                String query = searchBar.getText().toString().trim();

                if (!query.isEmpty()) {
                    saveSearchHistory(query);

                    Intent intent = new Intent(getApplicationContext(), ExerciseList.class);
                    intent.putExtra("search_query", query);
                    startActivity(intent);
                } else {
                    Toast.makeText(ExerciseSearch.this, "검색어를 입력하세요.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        Button btnSearchExercise = findViewById(R.id.btnSearchExercise);
        btnSearchExercise.setOnClickListener(v -> {
            String query = searchBar.getText().toString().trim();

            if (!query.isEmpty()) {
                saveSearchHistory(query);

                Intent intent = new Intent(getApplicationContext(), ExerciseList.class);
                intent.putExtra("search_query", query);
                startActivity(intent);
            } else {
                Toast.makeText(ExerciseSearch.this, "검색어를 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnAddExercise = findViewById(R.id.btnGoToAddExercise);
        btnAddExercise.setOnClickListener(v -> {
            Intent intent = new Intent(ExerciseSearch.this, ExerciseAdd.class);
            startActivity(intent);
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void saveSearchHistory(String query) {
        if (uid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference searchRef = firestore.collection("user_recent_searches").document(uid).collection("searches");

        Map<String, Object> searchData = new HashMap<>();
        searchData.put("query", query);
        searchData.put("timestamp", FieldValue.serverTimestamp());

        searchRef.add(searchData)
                .addOnSuccessListener(documentReference -> pruneRecentSearchesIfNeeded(searchRef))
                .addOnFailureListener(e -> Toast.makeText(this, "검색어 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void pruneRecentSearchesIfNeeded(CollectionReference searchRef) {
        searchRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    if (count > MAX_RECENT_SEARCHES) {
                        int itemsToDelete = count - MAX_RECENT_SEARCHES;

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            if (itemsToDelete-- <= 0) break;
                            doc.getReference().delete();
                        }
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
