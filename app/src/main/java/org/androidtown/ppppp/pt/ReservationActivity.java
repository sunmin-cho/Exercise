package org.androidtown.ppppp.pt;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.androidtown.ppppp.R;

import java.util.List;

public class ReservationActivity extends AppCompatActivity {

    private FirestoreHelper dbHelper;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation);

        dbHelper = new FirestoreHelper();
        listView = findViewById(R.id.reservation_list);

        String uid = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).getString("uid", null);
        if (uid == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper.getAllReservations(uid, reservations -> {
            if (reservations.isEmpty()) {
                Toast.makeText(this, "예약 내역이 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_list_item_1, reservations
            );
            listView.setAdapter(adapter);
        });
    }

}
