package org.androidtown.ppppp.pt;

import android.util.Log;

import com.google.firebase.firestore.*;

import java.util.*;

public class FirestoreHelper {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // 시간 등록
    public void insertTime(String uid, String day, int hour) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("day", day);
        data.put("hour", hour);

        db.collection("times")
                .add(data)
                .addOnSuccessListener(documentReference ->
                        Log.d("Firestore", "Time added for UID " + uid + ": " + documentReference.getPath()))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "❌ Error adding time", e));
    }


    // 유저별 가능한 시간 조회
    public void getTimes(String uid, String day, final FirestoreCallback<List<Integer>> callback) {
        db.collection("times")
                .whereEqualTo("uid", uid)
                .whereEqualTo("day", day)
                .get()
                .addOnCompleteListener(task -> {
                    List<Integer> result = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            result.add(doc.getLong("hour").intValue());
                        }
                    }
                    callback.onCallback(result);
                });
    }


    // 예약 추가
    public void insertReservation(String time, String uid) {
        Map<String, Object> data = new HashMap<>();
        data.put("time", time);
        data.put("uid", uid); // 사용자 구분 필드 추가

        db.collection("reservations")
                .add(data)
                .addOnSuccessListener(documentReference ->
                        Log.d("Firestore", "Reservation added for uid: " + uid))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error adding reservation", e));
    }


    // 전체 예약 목록 조회
    public void getAllReservations(String uid, final FirestoreCallback<List<String>> callback) {
        db.collection("reservations")
                .whereEqualTo("uid", uid)
                .get()
                .addOnCompleteListener(task -> {
                    List<String> result = new ArrayList<>();
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            result.add(doc.getString("time"));
                        }
                    }
                    callback.onCallback(result);
                });
    }

    // 예약 삭제
    public void deleteReservation(String time, String uid) {
        db.collection("reservations")
                .whereEqualTo("time", time)
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        doc.getReference().delete();
                    }
                });
    }

    // FirestoreHelper.java

    public interface ClearTimesCallback {
        void onClearedSuccessfully();
        void onClearFailed(Exception e);
    }

    public void clearTimesForUser(String uid, final ClearTimesCallback callback) {
        db.collection("times")
                .whereEqualTo("uid", uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (task.getResult().isEmpty()) {
                            Log.d("Firestore", "UID " + uid + "에 대해 삭제할 문서 없음.");
                            callback.onClearedSuccessfully(); // 삭제할 문서가 없으므로 "정리됨"
                            return;
                        }

                        List<com.google.android.gms.tasks.Task<Void>> deleteTasks = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Log.d("Firestore", "문서 삭제 시도: " + doc.getId());
                            deleteTasks.add(doc.getReference().delete());
                        }

                        // 모든 삭제 작업이 완료될 때까지 대기
                        com.google.android.gms.tasks.Tasks.whenAll(deleteTasks)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("Firestore", "UID " + uid + "에 대한 지정된 모든 문서 삭제 성공.");
                                    callback.onClearedSuccessfully();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Firestore", "❌ UID " + uid + "에 대한 하나 이상의 문서 삭제 오류", e);
                                    callback.onClearFailed(e);
                                });

                    } else {
                        Log.e("Firestore", "❌ UID " + uid + "에 대한 삭제할 문서 가져오기 오류", task.getException());
                        callback.onClearFailed(task.getException());
                    }
                });
    }

    // 콜백 인터페이스
    public interface FirestoreCallback<T> {
        void onCallback(T result);
    }
}
