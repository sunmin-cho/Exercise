import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "org.androidtown.ppppp"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.androidtown.ppppp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ✅ local.properties에서 API 키 불러오기
        val localProps = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localProps.load(FileInputStream(localPropsFile))
        }

        // ✅ buildConfigField로 키 추가
        buildConfigField("String", "KAKAO_API_KEY", "\"${localProps["KAKAO_API_KEY"]}\"")
        buildConfigField("String", "GOOGLE_MAP_API_KEY", "\"${localProps["GOOGLE_MAP_API_KEY"]}\"")
        buildConfigField("String", "KAKAO_MAP_KEY", "\"${localProps["KAKAO_MAP_KEY"]}\"")

        // ✅ Manifest에서 Google Maps API 키 사용
        manifestPlaceholders["GOOGLE_MAP_API_KEY"] = localProps["GOOGLE_MAP_API_KEY"].toString()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        buildConfig = true // ✅ 이 줄 반드시 필요
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0") // 최신 버전 유지
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("com.google.android.gms:play-services-auth:20.7.0")

    // Firebase (BOM 사용)
    implementation(platform("com.google.firebase:firebase-bom:33.11.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // Google Play Services
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // ML Kit
    implementation("com.google.mlkit:vision-common:17.3.0")
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:text-recognition-korean:16.0.0")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition-common:19.1.0")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition-korean:16.0.1")

    // Kakao Map
    implementation("com.kakao.maps.open:android:2.6.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // YouTube Player
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:11.1.0")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // 테스트
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

}
