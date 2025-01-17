plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")}

android {
    namespace = "com.example.appcontroller"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.appcontroller"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-analytics")
    implementation ("com.google.firebase:firebase-auth:21.1.0")
    implementation ("com.google.android.gms:play-services-auth:20.1.0") // Optional for Google sign-in
    implementation ("com.google.firebase:firebase-database")
    implementation ("com.google.firebase:firebase-database:20.0.5")

    // Add other Firebase dependencies as needed

}