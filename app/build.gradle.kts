plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "killua.dev.confundo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "killua.dev.confundo"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ShadowHook（inline hook）仅提供 ARM 架构，x86/x86_64 模拟器不支持，故只保留 ARM。
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
        externalNativeBuild {
            cmake {
                // -Oz + section GC + strip：以最小体积为目标编译 native 库。
                cppFlags += listOf("-std=c++17", "-Oz", "-fvisibility=hidden", "-ffunction-sections", "-fdata-sections")
                arguments += "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,--gc-sections,--exclude-libs,ALL"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // .so 必须以「未压缩 + 页对齐」形式保留在 APK 内（extractNativeLibs=false）。
    // 原因：LSPosed 以模块 APK 内嵌路径（base.apk!/lib/<abi>）作为 native 命名空间的库搜索路径，
    // ShadowHook 初始化时会用 soname `dlopen("libshadowhook_nothing.so")` 走该命名空间搜索加载辅助库。
    // 若用 useLegacyPackaging=true（extractNativeLibs=true），.so 在 APK 内是压缩的、无法被 dlopen，
    // 于是 shadowhook_init 报 SHADOWHOOK_ERRNO_INIT_LINKER(12)，所有 native hook 全部失效。
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            // 剔除仅编译期需要 / 调试用的打包产物，减小 APK。
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.version",
                "/META-INF/*.kotlin_module",
                "/META-INF/com/android/build/gradle/*",
                "/kotlin/**",
                "/DebugProbesKt.bin",
                "**/*.kotlin_builtins",
                "**/*.kotlin_metadata",
            )
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
        prefab = true
    }
}

dependencies {
    implementation(libs.yukihookapi.api)
    compileOnly(libs.xposed.api)
    ksp(libs.yukihookapi.ksp.xposed)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.material.kolor)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.shadowhook)
    debugImplementation(libs.androidx.ui.tooling)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}