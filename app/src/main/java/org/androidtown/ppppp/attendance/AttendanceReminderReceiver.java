package org.androidtown.ppppp.attendance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;

import java.util.*;

public class AttendanceReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlarmDebug", "Receiver triggered!");

        // SharedPreferences에서 사용자 UID와 목표 불러오기
        SharedPreferences prefs = context.getSharedPreferences("userPrefs", Context.MODE_PRIVATE);
        String uid = prefs.getString("uid", null);
        int weeklyGoal = prefs.getInt("weekly_goal", 3);

        if (uid == null) {
            Log.w("AlarmDebug", "UID 없음. 알림 건너뜀.");
            return;
        }

        // 이번 주 날짜 범위 계산
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        Date weekStart = calendar.getTime();

        calendar.add(Calendar.DAY_OF_WEEK, 6);
        Date weekEnd = calendar.getTime();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("attendance")
                .document(uid)
                .collection("records")
                .whereGreaterThanOrEqualTo("timestamp", weekStart)
                .whereLessThanOrEqualTo("timestamp", weekEnd)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int attendedCount = snapshot.size();
                    Log.d("AlarmDebug", "이번 주 출석 횟수: " + attendedCount + " / 목표: " + weeklyGoal);

                    if (attendedCount < weeklyGoal) {
                        sendNotification(context);
                    } else {
                        Log.d("AlarmDebug", "목표 달성 완료! 알림 미전송");
                    }
                })
                .addOnFailureListener(e -> Log.e("AlarmDebug", "Firestore 조회 실패: " + e.getMessage()));
    }

    private void sendNotification(Context context) {
        String channelId = "attendance_reminder";
        String channelName = "출석 리마인더";

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("출석 미달 주의!")
                .setContentText("이번 주 목표 출석 횟수를 아직 채우지 못했습니다. 오늘 꼭 출석하세요! 💪")
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        manager.notify(100, builder.build());
        Log.d("AlarmDebug", "출석 미달로 알림 전송됨");
    }
}
