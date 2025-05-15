package org.androidtown.ppppp.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.androidtown.ppppp.R;
import org.androidtown.ppppp.database.FoodDatabaseManager;
import org.androidtown.ppppp.models.FoodItem;
import java.util.List;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.ViewHolder> {
    private final List<FoodItem> foodList;
    private final FoodDatabaseManager dbManager;
    private final String selectedDate;
    private final Context context;

    public FoodAdapter(List<FoodItem> foodList, FoodDatabaseManager dbManager, String selectedDate, Context context) {
        this.foodList = foodList;
        this.dbManager = dbManager;
        this.selectedDate = selectedDate;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodItem food = foodList.get(position);
        holder.mealType.setText("[" + food.getMealType() + "]");
        holder.foodName.setText(food.getFoodName());
        holder.foodDetails.setText(food.getCalories() + " kcal | 단백질 " + food.getProtein() + "g | 탄수화물 " + food.getCarbs() + "g | 지방 " + food.getFat() + "g");

        // ✅ 삭제 기능 추가 (길게 누르면 확인 메시지 후 삭제)
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("식단 삭제")
                    .setMessage("이 음식을 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        dbManager.deleteFood(selectedDate, food.getFoodName());
                        foodList.remove(position);
                        notifyItemRemoved(position);
                        Toast.makeText(context, "식단이 삭제되었습니다!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("취소", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mealType, foodName, foodDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mealType = itemView.findViewById(R.id.mealType);
            foodName = itemView.findViewById(R.id.foodName);
            foodDetails = itemView.findViewById(R.id.foodDetails);
        }
    }
}
