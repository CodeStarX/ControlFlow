import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish") version "0.26.0"
}

android {
    namespace = "io.github.codestarx"
    compileSdk = 34

    defaultConfig {
        minSdk = 16
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    val coroutine = "1.7.3"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutine")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutine")

    val lifecycle = "2.7.0-rc02"
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle")

    val hamcrest = "2.2"
    testImplementation("org.hamcrest:hamcrest-library:$hamcrest")

    val mockk = "1.12.1"
    testImplementation("io.mockk:mockk:$mockk")

    val testRules = "1.4.0"
    testImplementation("androidx.test:rules:$testRules")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutine")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
mavenPublishing{
    coordinates("io.github.codestarx", "control-flow", "1.0.0-alpha09")
    publishToMavenCentral(SonatypeHost.S01)
    pom {
        name.set("control-flow")
        description.set("control-flow is an Android library that facilitates task sequencing, rollback actions, and error handling. It systematically oversees task execution, offering structured error handling and facilitating rollback processes for efficient management.")
        inceptionYear.set("2023")
        url.set("https://github.com/CodeStarX/ControlFlow/")
        packaging = "aar"
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("xxx")
                name.set("Mohsen Soltanian")
                url.set("https://github.com/CodeStarX/")
                email.set("soltaniyan.mohsen@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/CodeStarX/ControlFlow/")
            connection.set("scm:git:git://github.com/CodeStarX/ControlFlow.git")
            developerConnection.set("scm:git:ssh://git@github.com/CodeStarX/ControlFlow.git")
        }
    }
}