plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    `maven-publish`
}

android {
    namespace = "gg.offerwall.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "SDK_VERSION", "\"1.0.0\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // No runtime dependencies on purpose: the SDK is the system WebView and
    // the framework, so it adds nothing to a publisher's dependency tree.
    testImplementation("junit:junit:4.13.2")
    // Runs the real Activity and WebView client on the JVM, no emulator.
    testImplementation("org.robolectric:robolectric:4.13")
}

// What JitPack builds from a tag: com.github.offerwallgg:offerwallgg-android:<tag>
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.offerwallgg"
                artifactId = "offerwallgg-android"
                version = "1.0.0"
            }
        }
    }
}
