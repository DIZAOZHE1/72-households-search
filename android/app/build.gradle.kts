plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jordan.wailaixifu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jordan.wailaixifu"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        resourceConfigurations += listOf("zh", "en")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        noCompress += listOf("tsv", "txt")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val androidRoot = rootProject.projectDir
val toolsDir = androidRoot.resolve("tools")
val catalogueAsset = androidRoot.resolve("app/src/main/assets/wailai.tsv")

val extractWailai by tasks.registering(Exec::class) {
    group = "build"
    workingDir = androidRoot
    commandLine("node", toolsDir.resolve("extract_data.js").absolutePath)
    outputs.file(androidRoot.resolve("data/extracted/wailai.json"))
    onlyIf { !outputs.files.singleFile.exists() }
}

val extractQiershi by tasks.registering(Exec::class) {
    group = "build"
    workingDir = androidRoot
    commandLine("node", toolsDir.resolve("extract_qiershi.js").absolutePath)
    outputs.file(androidRoot.resolve("data/extracted/qiershi.json"))
    onlyIf { !outputs.files.singleFile.exists() }
}

val generateEpisodeAssets by tasks.registering(Exec::class) {
    group = "build"
    workingDir = androidRoot
    commandLine("node", toolsDir.resolve("build_asset.js").absolutePath)
    dependsOn(extractWailai, extractQiershi)
    inputs.dir(androidRoot.resolve("data/extracted"))
    outputs.file(androidRoot.resolve("app/src/main/assets/wailai.tsv"))
    outputs.file(androidRoot.resolve("app/src/main/assets/qiershi.tsv"))
    onlyIf { !catalogueAsset.exists() || catalogueAsset.length() == 0L }
}

tasks.named("preBuild") { dependsOn(generateEpisodeAssets) }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}
