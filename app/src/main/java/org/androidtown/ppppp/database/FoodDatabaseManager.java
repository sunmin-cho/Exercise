package org.androidtown.ppppp.database;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.gms.tasks.OnSuccessListener;

import org.androidtown.ppppp.models.FoodItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodDatabaseManager {

    private final FirebaseFirestore db;
    private final String userId;

    public FoodDatabaseManager(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        this.db = FirebaseFirestore.getInstance();
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    // 음식 저장 기능
    public void insertFood(String date, String mealType, String foodName, float calories, float protein, float carbs, float fat) {
        Map<String, Object> foodData = new HashMap<>();
        foodData.put("date", date);
        foodData.put("meal_type", mealType);
        foodData.put("food_name", foodName);
        foodData.put("calories", calories);
        foodData.put("protein", protein);
        foodData.put("carbs", carbs);
        foodData.put("fat", fat);

        db.collection("users")
                .document(userId)
                .collection("food_data")
                .add(foodData);
    }

    // 특정 날짜의 음식 리스트 가져오기
    public void getFoodListByDate(String date, OnSuccessListener<List<FoodItem>> onSuccess) {
        db.collection("users")
                .document(userId)
                .collection("food_data")
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<FoodItem> foodList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        FoodItem food = new FoodItem(
                                doc.getString("meal_type"),
                                doc.getString("food_name"),
                                doc.getDouble("calories").floatValue(),
                                doc.getDouble("protein").floatValue(),
                                doc.getDouble("carbs").floatValue(),
                                doc.getDouble("fat").floatValue()
                        );
                        foodList.add(food);
                    }
                    onSuccess.onSuccess(foodList);
                });
    }

    // 음식 삭제
    public void deleteFood(String date, String foodName) {
        db.collection("users")
                .document(userId)
                .collection("food_data")
                .whereEqualTo("date", date)
                .whereEqualTo("food_name", foodName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        db.collection("users")
                                .document(userId)
                                .collection("food_data")
                                .document(doc.getId())
                                .delete();
                    }
                });
    }

    // 운동 일지 저장
    public Task<Void> insertWorkoutLog(String date, String memo, String mediaUrl) {
        Map<String, Object> log = new HashMap<>();
        log.put("date", date);
        log.put("memo", memo);
        log.put("media_url", mediaUrl);

        return db.collection("users")
                .document(userId)
                .collection("workout_log")
                .add(log)
                .continueWith(task -> null);
    }

    // 운동 일지 삭제
    public Task<Void> deleteWorkoutLogsByDate(String date) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        db.collection("users")
                .document(userId)
                .collection("workout_log")
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(query -> {
                    List<Task<Void>> deletes = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        deletes.add(db.collection("users")
                                .document(userId)
                                .collection("workout_log")
                                .document(doc.getId())
                                .delete());
                    }
                    Tasks.whenAll(deletes).addOnSuccessListener(aVoid -> tcs.setResult(null));
                });
        return tcs.getTask();
    }

    // 운동 일지 조회
    public void getWorkoutLogsByDate(String date, OnSuccessListener<List<String[]>> onSuccess) {
        db.collection("users")
                .document(userId)
                .collection("workout_log")
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String[]> logs = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        logs.add(new String[]{
                                doc.getString("memo"),
                                doc.getString("media_url")
                        });
                    }
                    onSuccess.onSuccess(logs);
                });
    }

    // 운동 일지 존재 여부
    public void hasWorkoutLog(String date, OnSuccessListener<Boolean> onSuccess) {
        db.collection("users")
                .document(userId)
                .collection("workout_log")
                .whereEqualTo("date", date)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    onSuccess.onSuccess(!queryDocumentSnapshots.isEmpty());
                });
    }
}
