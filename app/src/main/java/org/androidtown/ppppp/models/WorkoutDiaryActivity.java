package org.androidtown.ppppp.models;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;

import org.androidtown.ppppp.database.FoodDatabaseManager;

import org.androidtown.ppppp.R;

import java.text.SimpleDateFormat;
import java.util.*;

public class WorkoutDiaryActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_MEDIA = 101;
    private static final int REQUEST_PERMISSION = 102;
    private LinearLayout mediaContainer;
    private EditText workoutMemo;
    private Button btnSelectMedia, btnSaveWorkout, btnDeleteWorkout;
    private List<Uri> selectedMediaUris = new ArrayList<>();
    private String selectedDate;
    private FrameLayout rootLayout;
    private FoodDatabaseManager db;
    private String uid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_diary);

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        uid = prefs.getString("uid", null);

        if (uid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish(); // 또는 로그인 화면으로 전환
            return;
        }

        selectedDate = getIntent().getStringExtra("selectedDate");
        if (selectedDate == null) {
            selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        }

        mediaContainer = findViewById(R.id.mediaContainer);
        workoutMemo = findViewById(R.id.workoutMemo);
        btnSelectMedia = findViewById(R.id.btnSelectMedia);
        btnSaveWorkout = findViewById(R.id.btnSaveWorkout);
        btnDeleteWorkout = findViewById(R.id.btnDeleteWorkout);
        rootLayout = findViewById(android.R.id.content);
        db = new FoodDatabaseManager(uid);

        // 기존 데이터가 있다면 불러오기 (읽기 모드 X, 수정 가능)
        loadWorkoutData(selectedDate);

        btnSelectMedia.setOnClickListener(v -> {
            if (checkPermissions()) {
                pickMedia();
            } else {
                requestPermissions();
            }
        });

        btnSaveWorkout.setOnClickListener(v -> {
            String memo = workoutMemo.getText().toString().trim();
            if (memo.isEmpty() && selectedMediaUris.isEmpty()) {
                Toast.makeText(this, "내용을 입력하거나 사진/영상을 선택해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            db.deleteWorkoutLogsByDate(selectedDate).addOnSuccessListener(aVoid -> {
                List<Task<Void>> saveTasks = new ArrayList<>();
                for (Uri uri : selectedMediaUris) {
                    saveTasks.add(db.insertWorkoutLog(selectedDate, memo, uri.toString()));
                }
                if (selectedMediaUris.isEmpty()) {
                    saveTasks.add(db.insertWorkoutLog(selectedDate, memo, ""));
                }

                Tasks.whenAll(saveTasks).addOnSuccessListener(unused -> {
                    Toast.makeText(this, "운동 일지가 저장되었습니다!", Toast.LENGTH_SHORT).show();
                    finish();
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "운동 일지 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            });
        });


        btnDeleteWorkout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("운동 일지 삭제")
                    .setMessage("해당 날짜의 운동 일지를 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        db.deleteWorkoutLogsByDate(selectedDate);
                        Toast.makeText(this, "운동 일지가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });
    }

    private void loadWorkoutData(String date) {
        db.getWorkoutLogsByDate(date, dataList -> {
            if (dataList == null || dataList.isEmpty()) return;

            workoutMemo.setText("");         // 메모 초기화
            selectedMediaUris.clear();       // 이전 미디어 초기화

            StringBuilder combinedMemo = new StringBuilder();  // 메모 여러 개일 경우

            for (String[] item : dataList) {
                String memo = item[0];
                String mediaUri = item[1];

                // ✅ 메모 누적 (하나만 보여주려면 setText만 사용)
                if (memo != null && !memo.isEmpty()) {
                    combinedMemo.append(memo).append("\n");
                }

                // ✅ 미디어 URI 처리
                if (mediaUri != null && !mediaUri.isEmpty()) {
                    try {
                        selectedMediaUris.add(Uri.parse(mediaUri));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            workoutMemo.setText(combinedMemo.toString().trim());  // 메모 반영
            showSelectedMedia();  // UI 갱신
        });
    }


    private void pickMedia() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "미디어 선택"), REQUEST_CODE_MEDIA);

    }

    private boolean checkPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO},
                    REQUEST_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_MEDIA && resultCode == RESULT_OK && data != null) {
            selectedMediaUris.clear();
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    selectedMediaUris.add(uri);
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                selectedMediaUris.add(uri);
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            showSelectedMedia();
        }
    }

    private void showSelectedMedia() {
        mediaContainer.removeAllViews();
        for (int i = 0; i < selectedMediaUris.size(); i++) {
            Uri uri = selectedMediaUris.get(i);
            String type = getContentResolver().getType(uri);

            if (type != null && type.startsWith("video")) {
                FrameLayout frameLayout = new FrameLayout(this);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        (int) (getResources().getDisplayMetrics().density * 200),
                        LinearLayout.LayoutParams.MATCH_PARENT);
                layoutParams.setMargins(8, 8, 8, 8);
                frameLayout.setLayoutParams(layoutParams);

                ImageView thumbnail = new ImageView(this);
                thumbnail.setAdjustViewBounds(true);
                thumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(this, uri);
                    thumbnail.setImageBitmap(retriever.getFrameAtTime(1000000));
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    thumbnail.setBackgroundColor(Color.DKGRAY);
                } finally {
                    try { retriever.release(); } catch (Exception e) { e.printStackTrace(); }
                }

                thumbnail.setLayoutParams(new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

                ImageView playOverlay = new ImageView(this);
                playOverlay.setImageResource(android.R.drawable.ic_media_play);
                playOverlay.setAlpha(0.6f);
                FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT);
                overlayParams.gravity = Gravity.CENTER;
                playOverlay.setLayoutParams(overlayParams);

                int index = i;
                frameLayout.addView(thumbnail);
                frameLayout.addView(playOverlay);
                frameLayout.setOnClickListener(v -> showFullscreenImage(uri, index));

                mediaContainer.addView(frameLayout);

            } else {
                ImageView imageView = new ImageView(this);
                imageView.setImageURI(uri);
                imageView.setAdjustViewBounds(true);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        (int) (getResources().getDisplayMetrics().density * 200),
                        LinearLayout.LayoutParams.MATCH_PARENT);
                params.setMargins(8, 8, 8, 8);
                imageView.setLayoutParams(params);

                int index = i;
                imageView.setOnClickListener(v -> showFullscreenImage(uri, index));
                mediaContainer.addView(imageView);
            }
        }
    }

    private void showFullscreenImage(Uri uri, int index) {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.parseColor("#CC000000"));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        container.setLayoutParams(containerParams);

        String type = getContentResolver().getType(uri);
        View mediaView;

        if (type != null && type.startsWith("video")) {
            FrameLayout videoLayout = new FrameLayout(this);
            videoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f));

            VideoView videoView = new VideoView(this);
            videoView.setVideoURI(uri);
            videoView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));

            ImageView playButton = new ImageView(this);
            playButton.setImageResource(android.R.drawable.ic_media_play);
            playButton.setAlpha(0.7f);
            FrameLayout.LayoutParams playParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            playParams.gravity = Gravity.CENTER;
            playButton.setLayoutParams(playParams);

            playButton.setOnClickListener(v -> {
                playButton.setVisibility(View.GONE);
                videoView.start();
            });

            videoLayout.addView(videoView);
            videoLayout.addView(playButton);
            mediaView = videoLayout;

        } else {
            ImageView fullImage = new ImageView(this);
            fullImage.setImageURI(uri);
            fullImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            fullImage.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f));
            mediaView = fullImage;
        }

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);

        Button deleteButton = new Button(this);
        deleteButton.setText("🧺");
        deleteButton.setBackgroundColor(Color.WHITE);
        deleteButton.setTextSize(24f);
        deleteButton.setPadding(20, 10, 20, 10);

        Button closeButton = new Button(this);
        closeButton.setText("닫기");
        closeButton.setBackgroundColor(Color.WHITE);
        closeButton.setTextSize(19f);
        closeButton.setPadding(20, 10, 20, 10);

        deleteButton.setOnClickListener(v -> {
            selectedMediaUris.remove(index);
            rootLayout.removeView(overlay);
            showSelectedMedia();
        });

        closeButton.setOnClickListener(v -> rootLayout.removeView(overlay));

        buttonRow.addView(deleteButton);
        buttonRow.addView(closeButton);

        container.addView(mediaView);
        container.addView(buttonRow);

        overlay.addView(container);
        rootLayout.addView(overlay);
    }
}
