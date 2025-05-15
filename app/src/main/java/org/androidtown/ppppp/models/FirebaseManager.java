package org.androidtown.ppppp.models;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class FirebaseManager {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static void addWorkoutLog(String uid, String date, String memo, String mediaUrl) {
        Map<String, Object> workoutData = new HashMap<>();
        workoutData.put("date", date);
        workoutData.put("memo", memo);
        workoutData.put("mediaUrl", mediaUrl);

        db.collection("users").document(uid)
                .collection("workout_log").document()
                .set(workoutData)
                .addOnSuccessListener(aVoid -> System.out.println("운동 로그 저장 완료"))
                .addOnFailureListener(e -> System.out.println("운동 로그 저장 실패: " + e));
    }

    public static void addFoodItem(String uid, String foodName, int calories, float protein, float carbs, float fats) {
        Map<String, Object> foodData = new HashMap<>();
        foodData.put("name", foodName);
        foodData.put("calories", calories);
        foodData.put("protein", protein);
        foodData.put("carbs", carbs);
        foodData.put("fats", fats);

        db.collection("users").document(uid)
                .collection("food_log").document()
                .set(foodData)
                .addOnSuccessListener(aVoid -> System.out.println("식단 추가 성공"))
                .addOnFailureListener(e -> System.out.println("식단 추가 실패: " + e));
    }
}