package org.androidtown.ppppp.models;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import org.androidtown.ppppp.adapters.FoodAdapter;
import org.androidtown.ppppp.database.FoodDatabaseManager;

import org.androidtown.ppppp.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity2 extends AppCompatActivity {
    private CalendarView calendarView;
    private Button btnAddFood;
    private Button btnWorkoutDiary;
    private RecyclerView recyclerView;
    private FoodDatabaseManager dbManager;
    private TextView emptyTextView;
    private TextView tvWorkoutEntry;
    private String selectedDate = null;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        calendarView = findViewById(R.id.calendarView);
        btnAddFood = findViewById(R.id.btnAddFood);
        btnWorkoutDiary = findViewById(R.id.btnWorkoutDiary);
        recyclerView = findViewById(R.id.recyclerView);
        emptyTextView = findViewById(R.id.emptyTextView);
        tvWorkoutEntry = findViewById(R.id.tvWorkoutEntry);
        Button btnChatbot = findViewById(R.id.btnChatbot);

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        uid = prefs.getString("uid", null); // 🔄 필드 변수에 할당

        if (uid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();  // 또는 로그인 화면으로 이동
            return;
        }

        dbManager = new FoodDatabaseManager(uid); // ✅ 이 시점 이후에 초기화

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(new Date());
        updateFoodList();
        updateWorkoutEntryVisibility();

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            updateFoodList();
            updateWorkoutEntryVisibility();
        });

        btnChatbot.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity2.this, ChatbotActivity.class);
            startActivity(intent);
        });

        btnAddFood.setOnClickListener(v -> {
            if (selectedDate == null || selectedDate.isEmpty()) {
                Toast.makeText(this, "먼저 날짜를 선택해주세요!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(MainActivity2.this, AddFoodActivity.class);
            intent.putExtra("selectedDate", selectedDate);
            startActivity(intent);
        });

        btnWorkoutDiary.setOnClickListener(v -> {
            if (selectedDate == null || selectedDate.isEmpty()) {
                Toast.makeText(this, "먼저 날짜를 선택해주세요!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(MainActivity2.this, WorkoutDiaryActivity.class);
            intent.putExtra("selectedDate", selectedDate);
            startActivity(intent);
        });

        tvWorkoutEntry.setOnClickListener(v -> {
            if (selectedDate == null || selectedDate.isEmpty()) {
                Toast.makeText(this, "먼저 날짜를 선택해주세요!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(MainActivity2.this, WorkoutDiaryActivity.class);
            intent.putExtra("selectedDate", selectedDate);
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }


    private void updateFoodList() {
        if (selectedDate == null || selectedDate.isEmpty()) return;

        dbManager.getFoodListByDate(selectedDate, foodList -> {
            if (foodList.isEmpty()) {
                emptyTextView.setText("저장된 식단이 없습니다.");
                emptyTextView.setVisibility(View.VISIBLE);
                recyclerView.setAdapter(null);
            } else {
                emptyTextView.setVisibility(View.GONE);
                recyclerView.setAdapter(new FoodAdapter(foodList, dbManager, selectedDate, MainActivity2.this));
            }
        });
    }

    private void updateWorkoutEntryVisibility() {
        if (selectedDate == null) return;

        dbManager.hasWorkoutLog(selectedDate, hasWorkout -> {
            tvWorkoutEntry.setVisibility(hasWorkout ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateFoodList();
        updateWorkoutEntryVisibility();
    }
}