package org.androidtown.ppppp.models;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import org.androidtown.ppppp.database.FoodDatabaseManager;
import org.androidtown.ppppp.models.FoodItem;

import org.androidtown.ppppp.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddFoodActivity extends AppCompatActivity {
    private EditText editFoodName, editCalories, editProtein, editCarbs, editFats;
    private Button btnSearchFood, btnSaveFood;
    private ListView listViewFoodResults;
    private RadioGroup radioGroupMealType;
    private FoodDatabaseManager dbManager; // 이걸 꼭 필드에 저장해야 함
    private List<FoodItem> searchResults;
    private ArrayAdapter<String> adapter;
    private String selectedDate, mealType = "아침";
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_food);

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        uid = prefs.getString("uid", null);

        if (uid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ❗ 수정된 부분
        dbManager = new FoodDatabaseManager(uid);

        if (getIntent().hasExtra("selectedDate")) {
            selectedDate = getIntent().getStringExtra("selectedDate");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            selectedDate = sdf.format(new Date());
        }

        editFoodName = findViewById(R.id.editFoodName);
        editCalories = findViewById(R.id.editCalories);
        editProtein = findViewById(R.id.editProtein);
        editCarbs = findViewById(R.id.editCarbs);
        editFats = findViewById(R.id.editFats);
        btnSearchFood = findViewById(R.id.btnSearchFood);
        btnSaveFood = findViewById(R.id.btnSaveFood);
        listViewFoodResults = findViewById(R.id.listViewFoodResults);
        radioGroupMealType = findViewById(R.id.radioGroupMealType);

        radioGroupMealType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioBreakfast) mealType = "아침";
            else if (checkedId == R.id.radioLunch) mealType = "점심";
            else if (checkedId == R.id.radioDinner) mealType = "저녁";
        });

        btnSearchFood.setOnClickListener(v -> searchFood());
        btnSaveFood.setOnClickListener(v -> saveFoodData());

        listViewFoodResults.setOnItemClickListener((parent, view, position, id) -> {
            FoodItem selectedFood = searchResults.get(position);
            editFoodName.setText(selectedFood.getFoodName());
            editCalories.setText(String.valueOf(selectedFood.getCalories()));
            editProtein.setText(String.valueOf(selectedFood.getProtein()));
            editCarbs.setText(String.valueOf(selectedFood.getCarbs()));
            editFats.setText(String.valueOf(selectedFood.getFat()));
            btnSaveFood.setVisibility(View.VISIBLE);
        });
    }

    private void searchFood() {
        String query = editFoodName.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(this, "검색어를 입력하세요!", Toast.LENGTH_SHORT).show();
            return;
        }

        searchResults = new ArrayList<>();
        try {
            InputStream is = getResources().openRawResource(R.raw.food_data);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length == 5 && tokens[0].toLowerCase().contains(query.toLowerCase())) {
                    searchResults.add(new FoodItem("", tokens[0], Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3]), Float.parseFloat(tokens[4])));
                }
            }
            reader.close();
        } catch (IOException e) {
            Toast.makeText(this, "CSV 파일을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
        }

        List<String> foodNames = new ArrayList<>();
        for (FoodItem food : searchResults) {
            foodNames.add(food.getFoodName());
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, foodNames);
        listViewFoodResults.setAdapter(adapter);
    }

    private void saveFoodData() {
        if (editFoodName.getText().toString().isEmpty() ||
                editCalories.getText().toString().isEmpty() ||
                editProtein.getText().toString().isEmpty() ||
                editCarbs.getText().toString().isEmpty() ||
                editFats.getText().toString().isEmpty()) {
            Toast.makeText(this, "음식을 먼저 선택하세요!", Toast.LENGTH_SHORT).show();
            return;
        }

        dbManager.insertFood(selectedDate, mealType, editFoodName.getText().toString(),
                Float.parseFloat(editCalories.getText().toString()),
                Float.parseFloat(editProtein.getText().toString()),
                Float.parseFloat(editCarbs.getText().toString()),
                Float.parseFloat(editFats.getText().toString()));

        Toast.makeText(this, mealType + " 식단이 추가되었습니다!", Toast.LENGTH_SHORT).show();
        finish();
    }
}

