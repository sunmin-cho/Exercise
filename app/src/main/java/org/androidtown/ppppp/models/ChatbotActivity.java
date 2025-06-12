package org.androidtown.ppppp.models;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.androidtown.ppppp.adapters.ChatAdapter;
import org.androidtown.ppppp.models.ChatMessage;

import okhttp3.*;

import org.androidtown.ppppp.R;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatbotActivity extends AppCompatActivity {

    private EditText inputMessage;
    private Button btnSend;
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatList;

    private static final String API_KEY = "AIzaSyAj0DSPGKaTQpv6rxeEE7n77YL-UT69Ang";
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-latest:generateContent?key=" + API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        inputMessage = findViewById(R.id.inputMessage);
        btnSend = findViewById(R.id.btnSend);
        recyclerView = findViewById(R.id.recyclerView);

        chatList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        btnSend.setOnClickListener(v -> {
            String userMessage = inputMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(userMessage)) {
                chatList.add(new ChatMessage(userMessage, true));
                chatAdapter.notifyItemInserted(chatList.size() - 1);
                recyclerView.scrollToPosition(chatList.size() - 1);
                inputMessage.setText("");
                sendToGemini(userMessage);
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
    private void sendToGemini(String userInput) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        try {
            JSONObject textObj = new JSONObject();
            textObj.put("text", userInput);

            JSONArray parts = new JSONArray();
            parts.put(textObj);

            JSONObject content = new JSONObject();
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject body = new JSONObject();
            body.put("contents", contents);

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(GEMINI_URL)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        chatList.add(new ChatMessage("❌ 네트워크 오류 발생: " + e.getMessage(), false));
                        chatAdapter.notifyItemInserted(chatList.size() - 1);
                        recyclerView.scrollToPosition(chatList.size() - 1);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        try {
                            JSONObject json = new JSONObject(responseBody);
                            String reply = json.getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");

                            runOnUiThread(() -> {
                                chatList.add(new ChatMessage(reply, false));
                                chatAdapter.notifyItemInserted(chatList.size() - 1);
                                recyclerView.scrollToPosition(chatList.size() - 1);
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                chatList.add(new ChatMessage("❌ 응답 파싱 실패", false));
                                chatAdapter.notifyItemInserted(chatList.size() - 1);
                                recyclerView.scrollToPosition(chatList.size() - 1);
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            chatList.add(new ChatMessage("❌ 응답 실패\n" + responseBody, false));
                            chatAdapter.notifyItemInserted(chatList.size() - 1);
                            recyclerView.scrollToPosition(chatList.size() - 1);
                        });
                    }
                }
            });

        } catch (Exception e) {
            runOnUiThread(() -> {
                chatList.add(new ChatMessage("❌ 오류 발생", false));
                chatAdapter.notifyItemInserted(chatList.size() - 1);
                recyclerView.scrollToPosition(chatList.size() - 1);
            });
        }
    }
}