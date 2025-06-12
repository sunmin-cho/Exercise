package org.androidtown.ppppp.pt;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;

import org.androidtown.ppppp.R;

import java.util.*;

public class PtTimeMain extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;
    private static final String CHANNEL_ID = "pt_booking_channel";
    private String uid;
    private FirestoreHelper dbHelper;
    private TimeGridSelector timeGrid;
    private List<String> commonTimes = new ArrayList<>();

    private final String[] days = {
            "Monday", "Tuesday", "Wednesday",
            "Thursday", "Friday", "Saturday", "Sunday"
    };

    private final Map<String, List<Integer>> trainerAvailableTimes = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pt_time);
        dbHelper = new FirestoreHelper();

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        uid = prefs.getString("uid", null);

        if (uid == null) {
            Toast.makeText(this, "UID를 불러올 수 없습니다. 다시 로그인 해주세요.", Toast.LENGTH_LONG).show();
            finish(); // 또는 로그인 화면으로 이동
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR}, 200);
        }

        timeGrid = findViewById(R.id.timeGrid);
        Button btnSave = findViewById(R.id.btn_save_times);
        Button btnShow = findViewById(R.id.btn_show_common);
        Button btnViewReservations = findViewById(R.id.btn_view_reservations);

        createTrainerAvailableTimes();
        createNotificationChannel();

        btnSave.setOnClickListener(v -> saveCheckedTimes());

        btnShow.setOnClickListener(v -> calculateCommonTimes());

        btnViewReservations.setOnClickListener(v -> {
            String uid = getSharedPreferences("userPrefs", MODE_PRIVATE).getString("uid", null);
            if (uid == null) {
                Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            dbHelper.getAllReservations(uid, reservations -> {
                if (reservations.isEmpty()) {
                    Toast.makeText(this, "예약 내역이 없습니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("예약 내역 (길게 누르면 취소)");

                ArrayAdapter<String> reservationAdapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_list_item_1, reservations);

                builder.setAdapter(reservationAdapter, null);
                AlertDialog dialog = builder.create();
                dialog.show();

                ListView listView = dialog.getListView();
                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                    String toCancel = reservations.get(position);
                    new AlertDialog.Builder(this)
                            .setTitle("예약 취소")
                            .setMessage(toCancel + " 예약을 취소하시겠습니까?")
                            .setPositiveButton("네", (d, w) -> {
                                dbHelper.deleteReservation(toCancel, uid); // ✅ uid 기반 삭제로 변경
                                Toast.makeText(this, "예약이 취소되었습니다", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            })
                            .setNegativeButton("아니오", null)
                            .show();
                    return true;
                });
            });
        });
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
    private void createTrainerAvailableTimes() {
        trainerAvailableTimes.put("Monday", Arrays.asList(10, 11, 14, 15, 16, 18, 19, 20, 21));
        trainerAvailableTimes.put("Tuesday", Arrays.asList(11, 12, 13, 14, 15, 16, 17, 20, 21, 22));
        trainerAvailableTimes.put("Wednesday", Arrays.asList(10, 11, 12, 15, 16, 17, 18, 19, 20, 21, 22));
        trainerAvailableTimes.put("Thursday", Arrays.asList(12, 13, 14, 15, 16, 17, 18, 20, 21));
        trainerAvailableTimes.put("Friday", Arrays.asList(10, 11, 12, 13, 16, 17, 18, 19, 20, 21, 22));
        trainerAvailableTimes.put("Saturday", Arrays.asList(13, 14, 15, 16, 17, 18, 19, 20));
        trainerAvailableTimes.put("Sunday", Arrays.asList(13, 14, 15, 16, 17, 18, 19, 20));
    }

    // PtTimeMain.java
    private void saveCheckedTimes() {
        if (uid == null) {
            Toast.makeText(this, "UID가 없어 시간을 저장할 수 없습니다.", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d("PtTimeMain", "UID " + uid + "에 대한 saveCheckedTimes 시작");

        dbHelper.clearTimesForUser(uid, new FirestoreHelper.ClearTimesCallback() {
            @Override
            public void onClearedSuccessfully() {
                Log.d("PtTimeMain", "UID " + uid + "의 시간이 성공적으로 삭제됨. 이제 새 시간 삽입.");
                Set<String> selected = timeGrid.getSelectedTimes();

                if (selected.isEmpty()) {
                    Log.d("PtTimeMain", "저장할 선택된 시간 없음.");
                    // Firestore 콜백은 일반적으로 메인 스레드에서 실행되지만, 만약을 위해 runOnUiThread 사용
                    runOnUiThread(() -> Toast.makeText(PtTimeMain.this, "선택된 시간이 없습니다.", Toast.LENGTH_SHORT).show());
                    return;
                }

                // 모든 삽입이 완료되었는지 확인하기 위해 카운터를 추가할 수 있습니다 (최종 "모두 저장됨" 메시지가 필요한 경우)
                for (String s : selected) {
                    String[] parts = s.split("-");
                    String korDay = parts[0];
                    int hour = Integer.parseInt(parts[1]);
                    String engDay = convertDayToEnglish(korDay);

                    Log.d("PtTimeMain", "시간 삽입: 요일=" + engDay + ", 시간=" + hour + " (UID: " + uid + ")");
                    dbHelper.insertTime(uid, engDay, hour); // insertTime은 이미 자체 성공/실패를 로깅함
                }
                // 이 Toast는 시간 저장 *시작* 과정이 완료되었음을 나타냅니다.
                // 개별 insertTime 호출은 여전히 비동기입니다.
                runOnUiThread(() -> Toast.makeText(PtTimeMain.this, "시간 저장 요청 완료", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onClearFailed(Exception e) {
                Log.e("PtTimeMain", "UID " + uid + "의 이전 시간 삭제 실패", e);
                runOnUiThread(() -> Toast.makeText(PtTimeMain.this, "이전 시간 삭제 실패: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private String convertDayToEnglish(String kor) {
        switch (kor) {
            case "월": return "Monday";
            case "화": return "Tuesday";
            case "수": return "Wednesday";
            case "목": return "Thursday";
            case "금": return "Friday";
            case "토": return "Saturday";
            case "일": return "Sunday";
            default: return kor;
        }
    }

    private void calculateCommonTimes() {
        commonTimes.clear();
        List<String> tempCommon = new ArrayList<>();
        final int[] pendingTasks = {days.length};

        for (String day : days) {
            dbHelper.getTimes(uid, day, memberTimes -> {
                List<Integer> trainer = trainerAvailableTimes.getOrDefault(day, new ArrayList<>());
                for (Integer hour : memberTimes) {
                    if (trainer.contains(hour)) {
                        tempCommon.add(day + " " + hour + ":00");
                    }
                }
                pendingTasks[0]--;
                if (pendingTasks[0] == 0) {
                    commonTimes.addAll(tempCommon);
                    showCommonTimeDialog();
                }
            });
        }
    }

    private void showCommonTimeDialog() {
        if (commonTimes.isEmpty()) {
            Toast.makeText(this, "교집합 없음", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("가능한 PT 시간");

        ArrayAdapter<String> dialogAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, commonTimes);

        builder.setAdapter(dialogAdapter, (dialog, which) -> {
            String selectedTime = commonTimes.get(which);
            showConfirmationDialog(selectedTime);
        });

        builder.setNegativeButton("닫기", null);
        builder.show();
    }

    private void showConfirmationDialog(String timeSlot) {
        new AlertDialog.Builder(this)
                .setTitle("예약 확인")
                .setMessage(timeSlot + " PT 예약하시겠습니까?")
                .setPositiveButton("확인", (dialog, which) -> {
                    dbHelper.insertReservation(timeSlot, uid); // ✅ uid 포함
                    showNotification(timeSlot);
                    addToCalendar(timeSlot);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void addToCalendar(String timeSlot) {
        try {
            String[] parts = timeSlot.split(" ");
            String dayStr = parts[0];
            int hour = Integer.parseInt(parts[1].split(":" )[0]);

            Calendar cal = Calendar.getInstance();
            int today = cal.get(Calendar.DAY_OF_WEEK);
            int target = getDayOfWeekIndex(dayStr);
            int diff = (target - today + 7) % 7;
            cal.add(Calendar.DAY_OF_MONTH, diff);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, 0);
            long startMillis = cal.getTimeInMillis();
            long endMillis = startMillis + 60 * 60 * 1000;

            ContentValues event = new ContentValues();
            event.put(CalendarContract.Events.CALENDAR_ID, 1);
            event.put(CalendarContract.Events.TITLE, "PT 세션");
            event.put(CalendarContract.Events.DTSTART, startMillis);
            event.put(CalendarContract.Events.DTEND, endMillis);
            event.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
                    == PackageManager.PERMISSION_GRANTED) {
                Uri uri = getContentResolver().insert(CalendarContract.Events.CONTENT_URI, event);
                if (uri != null) {
                    Toast.makeText(this, "캘린더에 예약 등록 완료", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getDayOfWeekIndex(String dayStr) {
        switch (dayStr) {
            case "Sunday": return Calendar.SUNDAY;
            case "Monday": return Calendar.MONDAY;
            case "Tuesday": return Calendar.TUESDAY;
            case "Wednesday": return Calendar.WEDNESDAY;
            case "Thursday": return Calendar.THURSDAY;
            case "Friday": return Calendar.FRIDAY;
            case "Saturday": return Calendar.SATURDAY;
            default: return Calendar.MONDAY;
        }
    }

    private void showNotification(String timeSlot) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("PT 예약 완료")
                .setContentText(timeSlot + " 예약되었습니다.")
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "PT 예약 채널", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("PT 예약 알림");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}
