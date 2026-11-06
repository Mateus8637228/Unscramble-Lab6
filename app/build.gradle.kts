/*
* Copyright (C) 2023 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    jacoco
}

android {
    namespace = "com.example.unscramble"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.unscramble"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")


    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

jacoco { toolVersion = "0.8.14" }

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

val debugClasses = files(
    layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"),
    layout.buildDirectory.dir("tmp/kotlin-classes/debug"),
    layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")
)

tasks.register<JacocoReport>("jacocoGameViewModelReport") {
    dependsOn("testDebugUnitTest")
    executionData.setFrom(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
    classDirectories.setFrom(debugClasses)
    sourceDirectories.setFrom(files("src/main/java"))
    reports {
        html.required = true
        xml.required = true
    }
}

tasks.register<JacocoCoverageVerification>("verifyGameViewModelCoverage") {
    dependsOn("jacocoGameViewModelReport")
    executionData.setFrom(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
    classDirectories.setFrom(debugClasses)
    violationRules {
        rule {
            element = "CLASS"
            includes = listOf("com.example.unscramble.ui.GameViewModel")
            for (metric in listOf("LINE", "BRANCH", "INSTRUCTION", "METHOD")) {
                limit {
                    counter = metric
                    minimum = "1.0".toBigDecimal()
                }
            }
        }
    }
}
