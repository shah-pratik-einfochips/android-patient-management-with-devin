plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    jacoco
}

android {
    namespace = "com.example.patienthealth"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.patienthealth"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    flavorDimensions += "environment"
    productFlavors {
        create("dev") { dimension = "environment"; applicationIdSuffix = ".dev"; resValue("string", "app_name", "Patient Health Dev") }
        create("qa") { dimension = "environment"; applicationIdSuffix = ".qa"; resValue("string", "app_name", "Patient Health QA") }
        create("prod") { dimension = "environment"; resValue("string", "app_name", "Patient Health") }
    }
    buildTypes {
        debug { enableUnitTestCoverage = true }
        release { isMinifyEnabled = false }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions { unitTests.isReturnDefaultValues = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }

jacoco { toolVersion = "0.8.13" }
tasks.register<JacocoReport>("domainCoverage") {
    dependsOn("testDevDebugUnitTest")
    reports { xml.required.set(true); html.required.set(true) }
    classDirectories.setFrom(fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/devDebug")) { include("**/domain/**") })
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(layout.buildDirectory.file("outputs/unit_test_code_coverage/devDebugUnitTest/testDevDebugUnitTest.exec"))
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.navigation)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.serialization)
    implementation(libs.coroutines)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(libs.room.testing)
}
