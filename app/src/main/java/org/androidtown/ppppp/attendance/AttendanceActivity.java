package org.androidtown.ppppp.attendance;

import org.androidtown.ppppp.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
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
    private Button btnCheckIn, btnSetAlarm;
    private RecyclerView recyclerAttendanceHistory;
    private FirebaseFirestore db;
    private AttendanceAdapter adapter;
    private List<String> attendanceList = new ArrayList<>();
    private String todayDate, uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        uid = prefs.getString("uid", null);
        if (uid == null) {
            Toast.makeText(this, "사용자 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvTodayDate = findViewById(R.id.tvTodayDate);
        tvStatus = findViewById(R.id.tvAttendanceStatus);
        btnCheckIn = findViewById(R.id.btnCheckIn);
        btnSetAlarm = findViewById(R.id.btnSetAlarm);
        recyclerAttendanceHistory = findViewById(R.id.recyclerAttendanceHistory);

        db = FirebaseFirestore.getInstance();

        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvTodayDate.setText("오늘 날짜: " + todayDate);

        recyclerAttendanceHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(attendanceList);
        recyclerAttendanceHistory.setAdapter(adapter);

        checkAlreadyCheckedIn();
        loadAttendanceHistory();

        btnCheckIn.setOnClickListener(v -> markAttendance());
        btnSetAlarm.setOnClickListener(v -> showTimePickerAndSetAlarm());

        restoreAlarmIfExists();
    }
    private void showTimePickerAndSetAlarm() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (TimePicker view, int hourOfDay, int minute) -> {
                    saveAlarmTime(hourOfDay, minute);
                    scheduleAlarm(hourOfDay, minute);
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.setTitle("알람 받을 시간을 선택하세요");
        timePickerDialog.show();
    }

    private void saveAlarmTime(int hour, int minute) {
        SharedPreferences prefs = getSharedPreferences("alarmPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("alarm_hour", hour);
        editor.putInt("alarm_minute", minute);
        editor.apply();
    }

    private void scheduleAlarm(int hour, int minute) {
        Intent intent = new Intent(this, AttendanceReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);

        Toast.makeText(this, String.format("출석 알림이 %02d:%02d에 설정되었습니다.", hour, minute), Toast.LENGTH_SHORT).show();
    }

    private void restoreAlarmIfExists() {
        SharedPreferences prefs = getSharedPreferences("alarmPrefs", MODE_PRIVATE);
        int hour = prefs.getInt("alarm_hour", -1);
        int minute = prefs.getInt("alarm_minute", -1);
        if (hour != -1 && minute != -1) {
            scheduleAlarm(hour, minute);
        }
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
