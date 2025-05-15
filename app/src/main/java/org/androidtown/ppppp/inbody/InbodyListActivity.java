package org.androidtown.ppppp.inbody;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.androidtown.ppppp.R;

import java.util.ArrayList;

public class InbodyListActivity extends AppCompatActivity {

    private static final String TAG = "InbodyList";

    private RecyclerView recyclerView;
    private InbodyAdapter adapter;
    private ArrayList<InbodyRecord> inbodyList = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbody_list);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InbodyAdapter(inbodyList);
        recyclerView.setAdapter(adapter);

        loadInbodyRecords();
    }

    private void loadInbodyRecords() {
        db.collection("inbody_records")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener((QuerySnapshot queryDocumentSnapshots) -> {
                    inbodyList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        InbodyRecord record = document.toObject(InbodyRecord.class);
                        inbodyList.add(record);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "불러오기 실패: " + e.getMessage());
                });
    }
}
