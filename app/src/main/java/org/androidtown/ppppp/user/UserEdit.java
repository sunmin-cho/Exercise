package org.androidtown.ppppp.user;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.androidtown.ppppp.R;

public class UserEdit extends AppCompatActivity {

    private EditText editName, editEmail, editAge;
    private Spinner spinnerGender;
    private ImageView imageViewProfile;
    private Button  buttonSave;

    private DatabaseReference userRef;
    private Uri selectedImageUri = null;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edituser);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());

        editName = findViewById(R.id.textEditName);
        editEmail = findViewById(R.id.textEditEmail);
        editAge = findViewById(R.id.textEditAge);
        spinnerGender = findViewById(R.id.spinnerUserGender);
        imageViewProfile = findViewById(R.id.imageViewProfileEdit);
        buttonSave = findViewById(R.id.buttonSave);

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        uid = prefs.getString("uid", null);

        if (uid == null) {
            Toast.makeText(this, "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                editName.setText(snapshot.child("name").getValue(String.class));
                editEmail.setText(snapshot.child("email").getValue(String.class));
                editAge.setText(snapshot.child("age").getValue(String.class));

                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                        UserEdit.this,
                        R.array.gender_options,
                        android.R.layout.simple_spinner_item
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerGender.setAdapter(adapter);

                String gender = snapshot.child("gender").getValue(String.class);
                if (gender != null) {
                    int index = adapter.getPosition(gender);
                    spinnerGender.setSelection(index);
                }

                // 기존 프로필 이미지 로드
                StorageReference imageRef = FirebaseStorage.getInstance()
                        .getReference("profileImages/" + uid + ".jpg");

                imageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            Glide.with(UserEdit.this)
                                    .load(uri)
                                    .signature(new ObjectKey(System.currentTimeMillis()))
                                    .into(imageViewProfile);
                        })
                        .addOnFailureListener(e -> {
                            Log.w("LOAD_IMAGE", "기존 이미지 불러오기 실패", e);
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserEdit.this, "정보 불러오기 실패", Toast.LENGTH_SHORT).show();
            }
        });

        // 이미지뷰와 버튼 모두 이미지 선택 동작
        imageViewProfile.setOnClickListener(v -> openGallery());

        buttonSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String email = editEmail.getText().toString().trim();
            String age = editAge.getText().toString().trim();
            String gender = spinnerGender.getSelectedItem().toString();

            if (name.isEmpty() || email.isEmpty() || age.isEmpty()) {
                Toast.makeText(this, "모든 항목을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            userRef.child("name").setValue(name);
            userRef.child("email").setValue(email);
            userRef.child("age").setValue(age);
            userRef.child("gender").setValue(gender)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "정보 저장 완료", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "정보 저장 실패", Toast.LENGTH_SHORT).show();
                        }
                    });

            if (selectedImageUri != null) {
                StorageReference storageRef = FirebaseStorage.getInstance()
                        .getReference("profileImages/" + uid + ".jpg");

                storageRef.putFile(selectedImageUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            Log.d("UPLOAD", "프로필 이미지 업로드 성공");

                            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    Glide.with(UserEdit.this)
                                            .load(uri)
                                            .signature(new ObjectKey(System.currentTimeMillis()))
                                            .into(imageViewProfile);
                                }

                                Toast.makeText(this, "저장 완료", Toast.LENGTH_SHORT).show();
                                finish();
                            });

                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(UserEdit.this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "저장 완료", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, 1000);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1000 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            imageViewProfile.setImageURI(selectedImageUri); // 미리보기 즉시 반영
        }
    }
}
