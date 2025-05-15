// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    dependencies {
        classpath ("com.google.gms:google-services:4.4.1")

    }

    repositories {
        google()  // << 이거 꼭 필요
        mavenCentral()
    }
}

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

