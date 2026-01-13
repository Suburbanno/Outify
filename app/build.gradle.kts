import groovy.xml.MarkupBuilder
import java.io.StringWriter

plugins {
    alias(libs.plugins.android.application)
		id("eclipse")
}

android {
    namespace = "cc.tomko.outify"
    compileSdk = 36

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.androidx.security.crypto)
    implementation(libs.log4j.slf4j2.impl)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

// put this in app/build.gradle.kts (module build file)
tasks.register("generateEclipseClasspath") {
    group = "ide"
    description = "Generate .classpath with resolved dependency jars for Eclipse/JDTLS (Kotlin DSL)"

    doLast {
        val outFile = project.rootDir.resolve(".classpath")
        val sb = StringBuilder()
        fun line(s: String) { sb.append(s).append('\n') }

        line("""<?xml version="1.0" encoding="UTF-8"?>""")
        line("<classpath>")

        // JRE container (change JavaSE-21 -> the JRE name you want)
        line("""  <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/JavaSE-21"/>""")

        // output + source folders
        line("""  <classpathentry kind="output" path="bin"/>""")
        line("""  <classpathentry kind="src" path="src/main/java"/>""")
        line("""  <classpathentry kind="src" path="src/main/kotlin"/>""")
        line("""  <classpathentry kind="src" path="src/test/java"/>""")
        line("""  <classpathentry kind="src" path="src/androidTest/java"/>""")

        val seen = mutableSetOf<String>()
        // prefer the debugCompileClasspath (includes AndroidX) but fall back if absent
        val conf = configurations.findByName("debugCompileClasspath") ?: configurations.findByName("compileClasspath")

        if (conf == null) {
            logger.lifecycle("No compile classpath configuration found. Available: ${configurations.map { it.name }}")
        } else {
            // force resolution and iterate resolved artifacts
            conf.resolvedConfiguration.resolvedArtifacts.forEach { art ->
                val path = art.file.absoluteFile.normalize().absolutePath.replace("\\", "/")
                if (seen.add(path)) {
                    // write a lib entry for each jar
                    line("""  <classpathentry kind="lib" path="$path"/>""")
                }
            }
        }

        // keep the gradle container entry (harmless if Buildship is later available)
        line("""  <classpathentry kind="con" path="org.eclipse.buildship.core.gradleclasspathcontainer"/>""")
        line("</classpath>")

        outFile.writeText(sb.toString())
        logger.lifecycle("Wrote .classpath (${sb.length} chars) to $outFile")
    }
}

