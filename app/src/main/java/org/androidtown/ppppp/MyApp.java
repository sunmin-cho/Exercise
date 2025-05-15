package org.androidtown.ppppp;

import android.app.Application;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false) // 캐시 OFF (딱 한 번만)
                .build();
        db.setFirestoreSettings(settings);
    }
}

