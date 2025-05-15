package org.androidtown.ppppp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

import org.androidtown.ppppp.chat.ChatList;
import org.androidtown.ppppp.exercise.ExerciseSearch;
import org.androidtown.ppppp.exercise.FavoritesActivity;
import org.androidtown.ppppp.exercise.RecommendedExercisesActivity;
import org.androidtown.ppppp.inbody.loadInbodyInfo;
import org.androidtown.ppppp.location.address;
import org.androidtown.ppppp.models.MainActivity2;
import org.androidtown.ppppp.pt.PtTimeMain;
import org.androidtown.ppppp.user.UserPage;

public class Homepage extends AppCompatActivity {

    private Button btnLocation, btnInbody, btnDoi, btnPT, btnSearch, btnFavorite, btnRecommend, btnMy, btnChat, btnLogout;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage);

        btnSearch = findViewById(R.id.btnsearch);
        btnMy = findViewById(R.id.btnmy);
        btnChat = findViewById(R.id.btnchat);
        btnFavorite = findViewById(R.id.btnfavorite);
        btnRecommend = findViewById(R.id.btnrecommend);
        btnDoi = findViewById(R.id.btndoi);
        btnPT = findViewById(R.id.btnpt);
        btnLocation = findViewById(R.id.btnlocation);
        btnInbody = findViewById(R.id.btninbody);
        btnLogout = findViewById(R.id.btnlogout);

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);

        // 주소 비교 화면으로 이동
        btnLocation.setOnClickListener(v -> {
            Intent intent = new Intent(Homepage.this, address.class);
            startActivity(intent);
        });
        // 인바디 저장 화면으로 이동
        btnInbody.setOnClickListener(v -> {
            Intent intent = new Intent(Homepage.this, loadInbodyInfo.class);
            startActivity(intent);
        });

        // 도이님 파트로 이동
        btnDoi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Homepage.this, MainActivity2.class);
                startActivity(intent);
            }
        });

        // 피티예약 파트로 이동
        btnPT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Homepage.this, PtTimeMain.class);
                startActivity(intent);
            }
        });

        // 운동 검색 버튼 클릭 시 ExerciseSearch.java로 이동
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Homepage.this, ExerciseSearch.class);
                startActivity(intent);
            }
        });

        // 즐겨찾기 버튼 클릭 시 FavoritesActivity.java로 이동
        btnFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Homepage.this, FavoritesActivity.class);
                startActivity(intent);
            }
        });

        // 운동추천 버튼 클릭 시 RecommededExercisesActivity.java로 이동
        btnRecommend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Homepage.this, RecommendedExercisesActivity.class);
                startActivity(intent);
            }
        });

        // 유저페이지 버튼 클릭 시 UserPage.java로 이동
        btnMy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Homepage.this, UserPage.class);
                startActivity(intent);
            }
        });

        // 채팅 버튼 클릭 시 ChatList.java로 이동
        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Homepage.this, ChatList.class);
                startActivity(intent);
            }
        });

        // Google 로그아웃 클라이언트 초기화
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // MainActivity에서 사용한 것과 동일해야 함
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 로그아웃 버튼 클릭 시
        btnLogout.setOnClickListener(v -> logout());

        // Toolbar 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 뒤로 가기 버튼 활성화
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    private void logout() {
        // Firebase 로그아웃
        FirebaseAuth.getInstance().signOut();

        // Google 로그아웃
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // SharedPreferences 삭제 (선택사항)
            SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();

            Toast.makeText(Homepage.this, "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();

            // 로그인 화면으로 이동
            Intent intent = new Intent(Homepage.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // 이전 스택 제거
            startActivity(intent);
            finish();
        });
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish(); // 현재 액티비티 종료 (뒤로 가기)
        return true;
    }
}
