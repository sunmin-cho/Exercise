package org.androidtown.ppppp.exercise;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.firestore.*;

import org.androidtown.ppppp.R;

import java.util.HashMap;
import java.util.List;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {

    private List<ExerciseModel> exerciseList;
    private Context context;
    private String uid;
    private FirebaseFirestore firestore;

    public ExerciseAdapter(Context context, List<ExerciseModel> exerciseList) {
        this.context = context;
        this.exerciseList = exerciseList;

        SharedPreferences prefs = context.getSharedPreferences("userPrefs", Context.MODE_PRIVATE);
        this.uid = prefs.getString("uid", null);

        if (uid == null) {
            Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
        }

        this.firestore = FirebaseFirestore.getInstance();
    }


    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exercise, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        ExerciseModel exercise = exerciseList.get(position);
        String title = exercise.getTitle();

        holder.exerciseName.setText(title);

        // ✅ Firestore로 즐겨찾기 상태 확인
        DocumentReference favDoc = firestore
                .collection("user_favorites")
                .document(uid)
                .collection("favorites")
                .document(title);

        favDoc.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                holder.favoriteIcon.setText("❤️");
            } else {
                holder.favoriteIcon.setText("🤍");
            }
        });

        // ✅ Firestore로 즐겨찾기 토글 처리
        holder.favoriteIcon.setOnClickListener(v -> {
            favDoc.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    favDoc.delete();
                    holder.favoriteIcon.setText("🤍");
                } else {
                    favDoc.set(new HashMap<String, Object>() {{
                        put("liked", true);
                    }});
                    holder.favoriteIcon.setText("❤️");
                }
            });
        });

        // 상세화면으로 이동 (category를 문자열로 변환해서 전달)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ExerciseDetail.class);
            intent.putExtra("exercise_name", title);
            intent.putExtra("category", String.join(", ", exercise.getCategory()));
            intent.putExtra("description", exercise.getDescription());
            intent.putExtra("videoUrl", exercise.getVideoUrl());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return exerciseList.size();
    }

    static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        TextView exerciseName;
        TextView favoriteIcon;

        public ExerciseViewHolder(View itemView) {
            super(itemView);
            exerciseName = itemView.findViewById(R.id.exercise_name);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);
        }
    }
}
