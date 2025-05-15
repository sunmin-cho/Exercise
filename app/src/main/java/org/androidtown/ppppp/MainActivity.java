package org.androidtown.ppppp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;

public class MainActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin, buttonGoogleLogin, buttonJoin;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private DatabaseReference usersRef;

    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonGoogleLogin = findViewById(R.id.buttonGoogleLogin);
        buttonJoin = findViewById(R.id.buttonJoin);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");


        // 이메일/비밀번호 로그인
        buttonLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(MainActivity.this, "이메일과 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            loginWithDatabase(email, password);
        });

        buttonJoin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, JoinMembership.class);
            startActivity(intent);
        });

        // 구글 로그인 설정
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // ✅ 구글 로그인 버튼 클릭 시 항상 계정 선택 화면을 띄움
        buttonGoogleLogin.setOnClickListener(v -> {
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            });
        });

        // 툴바
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // 이미 로그인 되어 있음 → Homepage로 바로 이동
            startActivity(new Intent(MainActivity.this, Homepage.class));
            finish();
        }
    }

    // 이메일/비밀번호 로그인 처리
    private void loginWithDatabase(String email, String password) {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean success = false;
                DataSnapshot matchedUser = null;

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String dbEmail = userSnapshot.child("email").getValue(String.class);
                    String dbPassword = userSnapshot.child("password").getValue(String.class);

                    if (email.equals(dbEmail) && password.equals(dbPassword)) {
                        success = true;
                        matchedUser = userSnapshot;
                        break;
                    }
                }

                if (success && matchedUser != null) {
                    String uid = matchedUser.getKey();

                    SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
                    prefs.edit().putString("uid", uid).apply();
                    Log.d("LoginSuccess", "로그인한 유저의 uid: " + uid);

                    Toast.makeText(MainActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, Homepage.class));
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, "이메일 또는 비밀번호가 잘못되었습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "데이터베이스 오류: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 구글 로그인 결과 처리
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w("GoogleLogin", "signInResult:failed code=" + e.getStatusCode(), e);
                Toast.makeText(this, "구글 로그인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Firebase 인증 처리 및 user 저장
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            Log.e("GoogleAuth", "FirebaseUser is null after signInWithCredential");
                            return;
                        }

                        String uid = user.getUid();
                        String email = user.getEmail();
                        String name = user.getDisplayName();

                        // SharedPreferences 저장
                        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
                        prefs.edit().putString("uid", uid).apply();

                        // DB에 사용자 정보 저장
                        usersRef.child(uid).child("email").setValue(email);
                        usersRef.child(uid).child("name").setValue(name);

                        Toast.makeText(this, "Google 로그인 성공!", Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(MainActivity.this, Homepage.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Firebase 인증 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
