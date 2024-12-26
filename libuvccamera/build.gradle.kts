plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "com.catchpig.uvccamera"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/libs")
        }
    }

    buildFeatures {
        buildConfig = true
    }
    ndkVersion = "26.1.10909125"
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.hutool)
}

val androidSourcesJar = tasks.register<Jar>("androidSourcesJar") {
    from(android.sourceSets["main"].java.srcDirs)
    archiveClassifier.convention("sources")
    archiveClassifier.set("sources")
}

publishing {
    publications {
        create<MavenPublication>("jitpack") {
            groupId = project.android.namespace
            artifactId = project.name
            version = "1.0.0"
            artifact(androidSourcesJar)
            afterEvaluate {
                artifact(tasks.named("bundleReleaseAar"))
            }
        }
    }
    repositories {
        maven {
            name = "local"
            url = uri("$rootDir/build")
        }
    }
}


