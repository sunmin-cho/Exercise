package org.androidtown.ppppp.attendance;

import org.androidtown.ppppp.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

import android.widget.NumberPicker;

public class AttendanceActivity extends AppCompatActivity {

    private TextView tvTodayDate, tvStatus;
    private Button  btnSetAlarm, btnSetGoal;
    private RecyclerView recyclerAttendanceHistory;
    private FirebaseFirestore db;
    private AttendanceAdapter adapter;
    private List<String> attendanceList = new ArrayList<>();
    private String todayDate, uid;
    private int weeklyGoal = 3;

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

        weeklyGoal = prefs.getInt("weekly_goal", 3);

        tvTodayDate = findViewById(R.id.tvTodayDate);
        tvStatus = findViewById(R.id.tvAttendanceStatus);
        btnSetAlarm = findViewById(R.id.btnSetAlarm);
        btnSetGoal = findViewById(R.id.btnSetGoal);
        recyclerAttendanceHistory = findViewById(R.id.recyclerAttendanceHistory);

        db = FirebaseFirestore.getInstance();

        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvTodayDate.setText("오늘 날짜: " + todayDate);

        recyclerAttendanceHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(attendanceList);
        recyclerAttendanceHistory.setAdapter(adapter);

        loadAttendanceHistory();
        btnSetAlarm.setOnClickListener(v -> showMaterialTimePickerAndSetAlarm());
        btnSetGoal.setOnClickListener(v -> showGoalSettingDialog());

        restoreAlarmIfExists();

        // Toolbar 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 뒤로 가기 버튼 활성화
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish(); // 현재 액티비티 종료 (뒤로 가기)
        return true;
    }

    private void showMaterialTimePickerAndSetAlarm() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(9)
                .setMinute(0)
                .setTitleText("출석 알림 시간 설정")
                .build();

        picker.show(getSupportFragmentManager(), "attendance_time_picker");

        picker.addOnPositiveButtonClickListener(view -> {
            int hour = picker.getHour();
            int minute = picker.getMinute();
            saveAlarmTime(hour, minute);
            scheduleAlarm(hour, minute);
            Toast.makeText(this, String.format("알람이 %02d:%02d로 설정됨", hour, minute), Toast.LENGTH_SHORT).show();
        });
    }

    private void showGoalSettingDialog() {
        final NumberPicker numberPicker = new NumberPicker(this);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(7);
        numberPicker.setValue(weeklyGoal);

        new MaterialAlertDialogBuilder(this)
                .setTitle("주간 출석 목표")
                .setMessage("목표 출석 횟수를 설정하세요 (1~7)")
                .setView(numberPicker)
                .setPositiveButton("저장", (dialog, which) -> {
                    int selectedGoal = numberPicker.getValue();
                    SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("weekly_goal", selectedGoal);  // 🔄 저장
                    editor.apply();
                    weeklyGoal = selectedGoal;
                    Toast.makeText(this, "목표가 " + selectedGoal + "회로 설정되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("취소", null)
                .show();
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
        Log.d("AlarmDebug", "Alarm set for: " + hour + ":" + minute);
    }

    private void restoreAlarmIfExists() {
        SharedPreferences prefs = getSharedPreferences("alarmPrefs", MODE_PRIVATE);
        int hour = prefs.getInt("alarm_hour", -1);
        int minute = prefs.getInt("alarm_minute", -1);
        if (hour != -1 && minute != -1) {
            scheduleAlarm(hour, minute);
        }
    }



    private void loadAttendanceHistory() {
        db.collection("attendance")
                .document(uid)
                .collection("records")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    attendanceList.clear();

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Calendar cal = Calendar.getInstance();

                    // 이번 주 시작 (일요일 00:00:00)
                    cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.MILLISECOND, 0);
                    Date weekStart = cal.getTime();

                    // 이번 주 끝 (토요일 23:59:59)
                    cal.add(Calendar.DAY_OF_WEEK, 6);
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    cal.set(Calendar.MILLISECOND, 999);
                    Date weekEnd = cal.getTime();

                    Log.d("AttendanceDebug", "WeekStart: " + weekStart + ", WeekEnd: " + weekEnd);

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Date docDate = sdf.parse(doc.getId());
                            if (docDate != null && !docDate.before(weekStart) && !docDate.after(weekEnd)) {
                                attendanceList.add(doc.getId());
                                Log.d("AttendanceDebug", "Included record: " + doc.getId());
                            } else {
                                Log.d("AttendanceDebug", "Excluded record: " + doc.getId());
                            }
                        } catch (Exception e) {
                            Log.e("AttendanceDebug", "Invalid date format in document ID: " + doc.getId(), e);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    Log.d("AttendanceDebug", "Filtered " + attendanceList.size() + " records this week.");
                })
                .addOnFailureListener(e -> Log.e("AttendanceDebug", "Failed to load attendance history", e));
    }

}
