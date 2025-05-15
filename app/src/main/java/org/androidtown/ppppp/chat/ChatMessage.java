package org.androidtown.ppppp.chat;

public class ChatMessage {
    public String senderId;
    public String text;
    public long timestamp;

    public ChatMessage() {}

    public ChatMessage(String senderId, String text, long timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }
}

