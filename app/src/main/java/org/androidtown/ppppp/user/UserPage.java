package org.androidtown.ppppp.user;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.androidtown.ppppp.MainActivity;
import org.androidtown.ppppp.R;


public class UserPage extends AppCompatActivity {

    private TextView textViewName, textViewEmail, textViewAge, textViewGender;
    private Button buttonEdit;

    private ImageView imageViewProfile;

    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userpage);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        textViewName = findViewById(R.id.textViewName);
        textViewEmail = findViewById(R.id.textViewEmail);
        textViewAge = findViewById(R.id.textViewAge);
        textViewGender = findViewById(R.id.textViewGender);
        buttonEdit = findViewById(R.id.buttonEdit);
        imageViewProfile = findViewById(R.id.imageViewProfile);

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        uid = prefs.getString("uid", null);

        if (uid == null) {
            Toast.makeText(this, "유저 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 수정 페이지로 이동
        buttonEdit.setOnClickListener(view -> {
            Intent intent = new Intent(UserPage.this, UserEdit.class);
            startActivity(intent);
        });

        // 최초 정보 불러오기
        loadUserInfo();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        uid = prefs.getString("uid", null);

        if (uid == null) {
            Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        loadUserInfo();
    }


    private void loadUserInfo() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);
                String age = snapshot.child("age").getValue(String.class);
                String gender = snapshot.child("gender").getValue(String.class);

                textViewName.setText(name);
                textViewEmail.setText(email);
                textViewAge.setText(age);
                textViewGender.setText(gender);

                // Firebase Storage에서 프로필 사진 불러오기
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference profileRef = storage.getReference().child("profileImages/" + uid + ".jpg");

                profileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Glide.with(UserPage.this)
                            .load(uri)
                            .into(imageViewProfile);
                }).addOnFailureListener(e -> {
                    Toast.makeText(UserPage.this, "프로필 사진을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserPage.this, "유저 정보 불러오기 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish(); // 뒤로 가기
        return true;
    }
}
