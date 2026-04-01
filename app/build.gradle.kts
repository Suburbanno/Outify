import com.android.build.api.dsl.ApplicationExtension

plugins {
    id("eclipse")
    id("com.google.devtools.ksp") version "2.3.4"
    id("com.google.dagger.hilt.android") version "2.59.1"
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    kotlin("plugin.serialization") version "2.3.0"
}

ksp {
    arg("dagger.fastInit", "enabled")
    arg("dagger.formatGeneratedSource", "disabled")
}

extensions.configure<ApplicationExtension>("android") {
    compileSdk = 36
    namespace = "cc.tomko.outify"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "cc.tomko.outify"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = (project.findProperty("versionName") ?: "dev") as String?
    }

    signingConfigs {
        create("ciRelease") {
            val keystoreFile = System.getenv("ANDROID_KEYSTORE_FILE")
            val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("ANDROID_KEY_ALIAS")
            val keyPassword = System.getenv("ANDROID_KEY_PASSWORD")

            if (!keystoreFile.isNullOrBlank() &&
                !keystorePassword.isNullOrBlank() &&
                !keyAlias.isNullOrBlank() &&
                !keyPassword.isNullOrBlank()
            ) {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            } else {
                // Fallback to debug
            }
        }
    }

    buildTypes {
        release {
//            isMinifyEnabled = true
//            isShrinkResources = true

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (
                !System.getenv("ANDROID_KEYSTORE_FILE").isNullOrBlank() &&
                !System.getenv("ANDROID_KEYSTORE_PASSWORD").isNullOrBlank() &&
                !System.getenv("ANDROID_KEY_ALIAS").isNullOrBlank() &&
                !System.getenv("ANDROID_KEY_PASSWORD").isNullOrBlank()
            ) {
                signingConfigs.getByName("ciRelease")
            } else {
                signingConfigs.getByName("debug")
            }
        }

        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
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
    compilerOptions {
        freeCompilerArgs.addAll("-Xjvm-default=all")
    }
}

dependencies {
    constraints {
        implementation("org.jetbrains:annotations:23.0.0") {
            because("Unify annotations artifact to avoid duplicate classes")
        }
    }

//    // Rustls
//    implementation(libs.rustls.platform.verifier)

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

    // Preferences
    implementation(libs.androidx.datastore.preferences)

    // Hilt
    implementation("com.google.dagger:hilt-android:2.59.1")
    implementation(libs.androidx.lifecycle.service)
    ksp("com.google.dagger:hilt-compiler:2.59.1")
    ksp("com.google.dagger:hilt-android-compiler:2.59.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Coil images
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0")

    implementation(libs.androidx.palette)
    implementation("com.google.android.material:material:1.13.0")

    // Native metadata serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")

    // Reorderable lists
    implementation(libs.reorderable)

    // Media3
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)

    // HTTP server for oauth
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    implementation(libs.kotlinx.coroutines.guava)

    implementation(platform(libs.compose.bom))
    implementation(libs.material)
    implementation(libs.material3.window.size.class1)
    implementation(libs.navigation.compose)

//    implementation(libs.room.compiler){
//        exclude(group = "com.intellij", module = "annotations")
//    }
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
