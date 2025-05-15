package org.androidtown.ppppp.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.androidtown.ppppp.R;

import java.util.List;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ViewHolder> {

    private List<String> roomIds;
    private OnChatRoomClickListener listener;

    public interface OnChatRoomClickListener {
        void onClick(String chatRoomId);
    }

    public ChatRoomAdapter(List<String> roomIds, OnChatRoomClickListener listener) {
        this.roomIds = roomIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatRoomAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_room_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomAdapter.ViewHolder holder, int position) {
        String chatRoomId = roomIds.get(position);

        // 상대방 UID 추출
        String[] users = chatRoomId.split("_");
        String opponentUid = "";

        // 현재 사용자 ID 가져오기
        SharedPreferences prefs = holder.itemView.getContext()
                .getSharedPreferences("userPrefs", Context.MODE_PRIVATE);
        String myUid = prefs.getString("uid", null);

        if (users.length == 2 && myUid != null) {
            opponentUid = users[0].equals(myUid) ? users[1] : users[0];
        }

        // 상대방 이름 가져오기
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(opponentUid).child("name");
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String opponentName = snapshot.getValue(String.class);
                holder.chatRoomTitle.setText((opponentName != null ? opponentName : "알 수 없음") + " 님과의 채팅");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.chatRoomTitle.setText("상대방 님과의 채팅");
            }
        });

        // 마지막 메시지 실시간 반영
        DatabaseReference messageRef = FirebaseDatabase.getInstance()
                .getReference("chatRooms")
                .child(chatRoomId)
                .child("messages");

        messageRef.orderByChild("timestamp").limitToLast(1)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                                String lastMessage = msgSnapshot.child("text").getValue(String.class);
                                holder.lastMessageText.setText(lastMessage != null ? lastMessage : "메시지를 불러올 수 없습니다.");
                            }
                        } else {
                            holder.lastMessageText.setText("채팅을 시작해보세요!");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        holder.lastMessageText.setText("메시지를 불러올 수 없습니다.");
                    }
                });

        // 채팅방 클릭 시 콜백
        holder.itemView.setOnClickListener(v -> listener.onClick(chatRoomId));
    }

    @Override
    public int getItemCount() {
        return roomIds.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView chatRoomTitle;
        TextView lastMessageText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            chatRoomTitle = itemView.findViewById(R.id.chatRoomTitle);
            lastMessageText = itemView.findViewById(R.id.lastMessageText);
        }
    }
}
