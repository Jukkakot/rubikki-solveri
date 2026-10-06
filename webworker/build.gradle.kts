import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

// The browser's scan worker (`camera-exposure` design 8): finds the video scan's faces off the page's
// thread. Only the cube logic; the web build copies its distribution next to the app's.
kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "scan-worker"
        browser {
            commonWebpackConfig { outputFileName = "scan-worker.js" }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":cube"))
            implementation(libs.kotlinx.browser)
        }
    }
}
