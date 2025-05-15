package org.androidtown.ppppp.models;

public class FoodItem {
    private String mealType;
    private String foodName;
    private float calories;
    private float protein;
    private float carbs;
    private float fat;

    // ✅ 기본 생성자 추가 (Firebase, Room 등에서 필요할 수 있음)
    public FoodItem() {
    }

    public FoodItem(String mealType, String foodName, float calories, float protein, float carbs, float fat) {
        this.mealType = mealType;
        this.foodName = foodName;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public float getCalories() {
        return calories;
    }

    public void setCalories(float calories) {
        this.calories = calories;
    }

    public float getProtein() {
        return protein;
    }

    public void setProtein(float protein) {
        this.protein = protein;
    }

    public float getCarbs() {
        return carbs;
    }

    public void setCarbs(float carbs) {
        this.carbs = carbs;
    }

    public float getFat() {
        return fat;
    }

    public void setFat(float fat) {
        this.fat = fat;
    }

    // ✅ 객체를 문자열로 변환할 때 음식 이름이 보이도록 설정
    @Override
    public String toString() {
        return foodName + " (" + calories + " kcal)";
    }
}
