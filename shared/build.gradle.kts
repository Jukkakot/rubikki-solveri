import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Everything both apps show: screens, navigation, theme, texts, fonts and pictures. Platform parts
// (camera, colours, clocks) are expect/actual seams; the Android and browser apps add the rest.
kotlin {
    jvmToolchain(21)

    android {
        namespace = "fi.jukkakot.rubikkisolveri.shared"
        compileSdk = 37
        minSdk = 31
        androidResources { enable = true }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            api(project(":cube"))
            api(libs.cmp.runtime)
            api(libs.cmp.foundation)
            api(libs.cmp.ui)
            api(libs.cmp.ui.backhandler)
            api(libs.cmp.material3)
            api(libs.cmp.components.resources)
            api(libs.jb.navigation.compose)
            api(libs.jb.lifecycle.runtime.compose)
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.camerax.core)
            implementation(libs.camerax.camera2)
            implementation(libs.camerax.lifecycle)
            implementation(libs.camerax.view)
        }
    }
}

compose.resources {
    packageOfResClass = "fi.jukkakot.rubikkisolveri.res"
    publicResClass = true
}
