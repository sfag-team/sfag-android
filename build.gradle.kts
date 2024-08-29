buildscript {
    dependencies {
        classpath (libs.gradle)
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.android) apply false
    alias(libs.plugins.daggerHilt) apply false
    alias(libs.plugins.devTools)
    alias(libs.plugins.googleGmsGoogleServices) apply false
}