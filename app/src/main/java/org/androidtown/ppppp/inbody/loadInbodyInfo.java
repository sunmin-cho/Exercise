package org.androidtown.ppppp.inbody;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import org.androidtown.ppppp.R;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class loadInbodyInfo extends AppCompatActivity {

    private static final int REQUEST_CODE_GALLERY = 100;
    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final String TAG = "InbodyOCR";

    private ImageView imageView;
    private Button btnSelectImage;
    private Bitmap selectedBitmap;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_inbody_info);

        imageView = findViewById(R.id.imgPreview);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        tvResult = findViewById(R.id.tvInbodyResult);

        btnSelectImage.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_CODE_PERMISSIONS);
            } else {
                openGallery();
            }
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
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_GALLERY && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), imageUri);
                    selectedBitmap = ImageDecoder.decodeBitmap(source);
                } else {
                    selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                }

                imageView.setImageBitmap(selectedBitmap);

                // OCR 시작
                recognizeInbodyInfo(selectedBitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void recognizeInbodyInfo(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap is null");
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        TextRecognizer recognizer = TextRecognition.getClient(
                new KoreanTextRecognizerOptions.Builder().build()
        );

        recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text visionText) {
                        Log.i(TAG, "OCR 인식 성공");
                        parseInbodyResult(visionText.getText());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "OCR 인식 실패: " + e.getMessage());
                    }
                });
    }

    private void parseInbodyResult(String extractedText) {
        Log.i(TAG, "전체 OCR 텍스트:\n" + extractedText);

        String weight = extractInfo(extractedText, "체중");
        String muscleMass = extractInfo(extractedText, "골격근량");
        String bodyFatMass = extractInfo(extractedText, "체지방량");
        String bodyFatPercent = extractInfo(extractedText, "체지방률");
        String bmi = extractInfo(extractedText, "BMI");

        Log.i(TAG, "체중: " + weight);
        Log.i(TAG, "골격근량: " + muscleMass);
        Log.i(TAG, "체지방량: " + bodyFatMass);
        Log.i(TAG, "체지방률: " + bodyFatPercent);
        Log.i(TAG, "BMI: " + bmi);

        Toast.makeText(this,
                "체중: " + weight + "kg\n" +
                        "골격근량: " + muscleMass + "kg\n" +
                        "체지방량: " + bodyFatMass + "kg\n" +
                        "체지방률: " + bodyFatPercent + "%\n" +
                        "BMI: " + bmi,
                Toast.LENGTH_LONG).show();

        tvResult.setText("체중: " + weight + "kg\n" +
                "골격근량: " + muscleMass + "kg\n" +
                "체지방량: " + bodyFatMass + "kg\n" +
                "체지방률: " + bodyFatPercent + "%\n" +
                "BMI: " + bmi);
        tvResult.setVisibility(View.VISIBLE);

        // Firestore에 저장
        saveInbodyResult(weight, muscleMass, bodyFatMass, bodyFatPercent, bmi);
    }

    private String extractInfo(String text, String keyword) {
        String[] lines = text.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.contains(keyword)) {
                if (i + 1 < lines.length) {
                    return extractNumber(lines[i + 1]);
                }
            }
        }
        return "값 없음";
    }

    private String extractNumber(String text) {
        return text.replaceAll("[^0-9.]", ""); // 숫자와 소수점만 남김
    }

    private void saveInbodyResult(String weight, String muscleMass, String bodyFatMass, String bodyFatPercent, String bmi) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> inbodyData = new HashMap<>();
        // saveInbodyResult 메서드 시작 부분에 로그 추가
        Log.d(TAG, "Firestore에 저장 시도: weight=" + weight + ", muscleMass=" + muscleMass);

        try {
            // 문자열을 숫자로 변환
            double weightVal = Double.parseDouble(weight);
            double muscleMassVal = Double.parseDouble(muscleMass);
            double bodyFatMassVal = Double.parseDouble(bodyFatMass);
            double bodyFatPercentVal = Double.parseDouble(bodyFatPercent);
            double bmiVal = Double.parseDouble(bmi);

            inbodyData.put("weight", weightVal);
            inbodyData.put("muscleMass", muscleMassVal);
            inbodyData.put("bodyFatMass", bodyFatMassVal);
            inbodyData.put("bodyFatPercent", bodyFatPercentVal);
            inbodyData.put("bmi", bmiVal);
        } catch (NumberFormatException e) {
            // 변환 실패 시 원래 문자열로 저장
            Log.w(TAG, "숫자 변환 실패, 문자열로 저장합니다: " + e.getMessage());
            inbodyData.put("weight", weight);
            inbodyData.put("muscleMass", muscleMass);
            inbodyData.put("bodyFatMass", bodyFatMass);
            inbodyData.put("bodyFatPercent", bodyFatPercent);
            inbodyData.put("bmi", bmi);
        }

        inbodyData.put("timestamp", System.currentTimeMillis());

        // 저장 시 오류 로그 자세히 확인
        db.collection("inbody_records")
                .add(inbodyData)
                .addOnSuccessListener(documentReference -> {
                    Log.i(TAG, "Firestore 저장 성공: " + documentReference.getId());
                    Toast.makeText(this, "인바디 결과 저장 완료!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore 저장 실패: " + e.getMessage(), e);
                    e.printStackTrace();
                    Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    /*private void saveInbodyResult(String weight, String muscleMass, String bodyFatMass, String bodyFatPercent, String bmi) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> inbodyData = new HashMap<>();
        inbodyData.put("weight", weight);
        inbodyData.put("muscleMass", muscleMass);
        inbodyData.put("bodyFatMass", bodyFatMass);
        inbodyData.put("bodyFatPercent", bodyFatPercent);
        inbodyData.put("bmi", bmi);
        inbodyData.put("timestamp", System.currentTimeMillis()); // 저장 시각

        db.collection("inbody_records")
                .add(inbodyData)
                .addOnSuccessListener(documentReference -> {
                    Log.i(TAG, "Firestore 저장 성공");
                    Toast.makeText(this, "인바디 결과 저장 완료!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore 저장 실패: " + e.getMessage());
                });
    }*/
}
