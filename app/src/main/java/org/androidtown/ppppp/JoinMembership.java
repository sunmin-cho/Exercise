package org.androidtown.ppppp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.*;
import com.google.firebase.database.*;

public class JoinMembership extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword, editTextName, editTextAge;
    private Button buttonRegister;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private Spinner spinnerGender;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.join_membership);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextName = findViewById(R.id.editTextName);
        editTextAge = findViewById(R.id.editTextAge);
        buttonRegister = findViewById(R.id.buttonJoin);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // Toolbar 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 뒤로 가기 버튼 활성화
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        spinnerGender = findViewById(R.id.spinnerGender);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.gender_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);


        buttonRegister.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();
            String name = editTextName.getText().toString().trim();
            String age = editTextAge.getText().toString().trim();
            String gender = spinnerGender.getSelectedItem().toString();


            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) ||
                    TextUtils.isEmpty(name) || TextUtils.isEmpty(age) || TextUtils.isEmpty(gender)) {
                Toast.makeText(this, "모든 항목을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(email, password, name, age, gender);
        });
    }

    private void registerUser(String email, String password, String name, String age, String gender) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            Toast.makeText(this, "회원가입 실패: 사용자 없음", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String uid = user.getUid();
                        SharedPreferences prefs = getSharedPreferences("userPs", MODE_PRIVATE);
                        prefs.edit().putString("uid", uid).apply();

                        usersRef.child(uid).child("email").setValue(email);
                        usersRef.child(uid).child("password").setValue(password); // 학습용
                        usersRef.child(uid).child("name").setValue(name);
                        usersRef.child(uid).child("age").setValue(age);
                        usersRef.child(uid).child("gender").setValue(gender);

                        Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(JoinMembership.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish(); // 현재 액티비티 종료 (뒤로 가기)
        return true;
    }
}
