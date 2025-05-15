package org.androidtown.ppppp.chat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.androidtown.ppppp.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChatList extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatRoomAdapter adapter;
    private List<String> chatRoomIds = new ArrayList<>();
    private DatabaseReference dbRef;
    private String uid;
    private EditText editTextTargetName;
    private Button buttonStartChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatlist);

        // Toolbar 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 뒤로 가기 버튼 활성화
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences prefs = getSharedPreferences("userPrefs", MODE_PRIVATE);
        uid = prefs.getString("uid", null);

        if (uid == null) {
            Toast.makeText(this, "사용자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerView = findViewById(R.id.chatRoomRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatRoomAdapter(chatRoomIds, this::openChatRoom);
        recyclerView.setAdapter(adapter);

        dbRef = FirebaseDatabase.getInstance().getReference("chatRooms");

        editTextTargetName = findViewById(R.id.editTextTargetName);
        buttonStartChat = findViewById(R.id.buttonStartChat);

        buttonStartChat.setOnClickListener(v -> {
            String targetName = editTextTargetName.getText().toString().trim();
            if (TextUtils.isEmpty(targetName)) {
                Toast.makeText(this, "상대방 이름을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String targetUid = null;
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String name = userSnapshot.child("name").getValue(String.class);
                        if (targetName.equals(name)) {
                            targetUid = userSnapshot.getKey();
                            break;
                        }
                    }

                    if (targetUid != null && !targetUid.equals(uid)) {
                        List<String> sorted = Arrays.asList(uid, targetUid);
                        Collections.sort(sorted);
                        String roomId = sorted.get(0) + "_" + sorted.get(1);

                        DatabaseReference roomRef = FirebaseDatabase.getInstance()
                                .getReference("chatRooms").child(roomId);
                        roomRef.child("participants").child(uid).setValue(true);
                        roomRef.child("participants").child(targetUid).setValue(true);

                        // 입력창 초기화
                        editTextTargetName.setText("");

                        // 채팅방으로 바로 이동
                        Intent intent = new Intent(ChatList.this, ChatActivity.class);
                        intent.putExtra("chatRoomId", roomId);
                        intent.putExtra("userId", uid);
                        startActivity(intent);

                    } else if (targetUid != null && targetUid.equals(uid)) {
                        Toast.makeText(ChatList.this, "자기 자신과는 채팅할 수 없습니다", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ChatList.this, "해당 이름의 사용자를 찾을 수 없습니다", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(ChatList.this, "DB 오류: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChatRooms(); // 리스트 재진입 시 최신 채팅방 불러오기
    }

    private void loadChatRooms() {
        dbRef.orderByChild("participants/" + uid).equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        chatRoomIds.clear();
                        for (DataSnapshot room : snapshot.getChildren()) {
                            chatRoomIds.add(room.getKey());
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChatList.this, "채팅방 목록 오류", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openChatRoom(String chatRoomId) {
        Intent intent = new Intent(ChatList.this, ChatActivity.class);
        intent.putExtra("chatRoomId", chatRoomId);
        intent.putExtra("userId", uid);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
