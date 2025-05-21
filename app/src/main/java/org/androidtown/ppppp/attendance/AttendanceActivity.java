package org.androidtown.ppppp.attendance;

import org.androidtown.ppppp.R;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class AttendanceActivity extends AppCompatActivity {

    private TextView tvTodayDate, tvStatus;
    private Button btnCheckIn;
    private RecyclerView recyclerAttendanceHistory;

    private FirebaseFirestore db;

    private AttendanceAdapter adapter;
    private List<String> attendanceList = new ArrayList<>();

    private String todayDate;
    private String uid;  // 🔹 SharedPreferences로부터 불러올 사용자 ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        // 🔹 SharedPreferences에서 uid 가져오기
        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        uid = prefs.getString("uid", null);

        if (uid == null) {
            Toast.makeText(this, "사용자 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_LONG).show();
            finish(); // 액티비티 종료
            return;
        }

        TextView tvTodayDate = findViewById(R.id.tvTodayDate);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvTodayDate.setText("오늘 날짜: " + today);

        tvTodayDate = findViewById(R.id.tvTodayDate);
        tvStatus = findViewById(R.id.tvAttendanceStatus);
        btnCheckIn = findViewById(R.id.btnCheckIn);
        recyclerAttendanceHistory = findViewById(R.id.recyclerAttendanceHistory);

        db = FirebaseFirestore.getInstance();

        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvTodayDate.setText("오늘 날짜: " + todayDate);

        recyclerAttendanceHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(attendanceList);
        recyclerAttendanceHistory.setAdapter(adapter);

        checkAlreadyCheckedIn(); // 이미 출석했는지 확인
        loadAttendanceHistory(); // 출석 기록 불러오기

        btnCheckIn.setOnClickListener(v -> markAttendance());
    }

    private void checkAlreadyCheckedIn() {
        db.collection("attendance")
                .document(uid)
                .collection("records")
                .document(todayDate)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tvStatus.setText("이미 출석 완료되었습니다.");
                        btnCheckIn.setEnabled(false);
                    }
                });
    }

    private void markAttendance() {
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", Timestamp.now());

        db.collection("attendance")
                .document(uid)
                .collection("records")
                .document(todayDate)
                .set(data)
                .addOnSuccessListener(unused -> {
                    tvStatus.setText("출석 완료되었습니다!");
                    btnCheckIn.setEnabled(false);
                    attendanceList.add(0, todayDate); // 최신 출석일을 맨 위로
                    adapter.notifyItemInserted(0);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "출석 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadAttendanceHistory() {
        db.collection("attendance")
                .document(uid)
                .collection("records")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        attendanceList.add(doc.getId()); // 날짜가 문서 ID
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
