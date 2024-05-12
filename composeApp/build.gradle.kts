import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(dependency.plugins.kotlinx.serialization)
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            // intermediate source set for everything except js and wasm
            group("nonWeb") {
                withAndroidTarget()
                withNative()
                withJvm()
            }
            group("jsWasm") {
                withJs()
                withWasm()
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "composeApp"
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    js(IR) {
        browser()
        binaries.executable()
    }

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            version = "1.0"
        }
    }
    
    sourceSets {
        val desktopMain by getting
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(compose.components.uiToolingPreview)
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)

            // implementation(libs.kamel.image) // https://github.com/Kamel-Media/Kamel/issues/85
//            implementation(libs.ktor.core)
//            implementation(libs.ktor.contentNegotiation)
//            implementation(libs.ktor.serialization)

            // implementation(libs.orbital)

        }

        val nonWebMain by getting {
            dependencies {
                implementation(dependency.compose.webview.multiplatform)
            }
        }

        androidMain.dependencies {
//            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.java)
        }

        jsMain.dependencies {
            // TODO: replace with implementation("com.github.Hamamas:Kotlin-Wasm-Html-Interop:0.0.3-alpha")
            implementation(project(":composeWebInterop"))
            implementation(npm("leaflet", "1.9.4"))
        }

        val wasmJsMain by getting
        wasmJsMain.dependencies {
            // TODO: replace with implementation("com.github.Hamamas:Kotlin-Wasm-Html-Interop:0.0.3-alpha")
            implementation(project(":composeWebInterop"))
            implementation(npm("leaflet", "1.9.4"))
        }

    }
}

android {
    namespace = "org.msa.composekmm"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "org.msa.composekmm"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.msa.composekmm"
            packageVersion = "1.0.0"
            modules("java.net.http")
            macOS {
                iconFile.set(project.file("./launcher_icons/app_icon.icns"))
            }
            windows {
                iconFile.set(project.file("./launcher_icons/app_icon.ico"))
            }
            linux {
                iconFile.set(project.file("./launcher_icons/app_icon.png"))
            }
        }
    }
}

compose.experimental {
    web.application {}
}