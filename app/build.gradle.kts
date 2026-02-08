plugins {
    id("eclipse")
    id("com.google.devtools.ksp") version "2.3.4"
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    kotlin("plugin.serialization") version "2.3.0"
}

android {
    namespace = "cc.tomko.outify"
    compileSdk = 36

    buildFeatures {
        compose = true
    }

    defaultConfig {
        applicationId = "cc.tomko.outify"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    constraints {
        implementation("org.jetbrains:annotations:23.0.0") {
            because("Unify annotations artifact to avoid duplicate classes")
        }
    }

    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.tink.android)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.animation.core)
    implementation(libs.navigation3.runtime)
    implementation(libs.adaptive.navigation3)

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")

    implementation(libs.reorderable)

    implementation(platform(libs.compose.bom))
    implementation(libs.material)
    implementation(libs.material3.window.size.class1)
    implementation(libs.navigation.compose)

    implementation(libs.room.compiler){
        exclude(group = "com.intellij", module = "annotations")
    }
    implementation(libs.room.runtime){
        exclude(group = "com.intellij", module = "annotations")
    }
    implementation(libs.room.ktx){
        exclude(group = "com.intellij", module = "annotations")
    }
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}