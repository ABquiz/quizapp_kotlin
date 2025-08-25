// Gradle 스크립트의 상단에 위치하며, 프로젝트에 적용할 플러그인들을 정의합니다.
plugins {
    // 안드로이드 애플리케이션 빌드를 위한 플러그인입니다.
    alias(libs.plugins.android.application)
    // 코틀린 안드로이드 개발을 위한 플러그인입니다.
    alias(libs.plugins.kotlin.android)
}

// 안드로이드 앱의 빌드 설정을 위한 블록입니다.
android {
    // 앱의 패키지 이름을 정의합니다.
    namespace = "com.example.quizapp_kotlin"
    // 앱을 컴파일할 안드로이드 SDK 버전을 정의합니다.
    compileSdk = 36

    // 앱의 기본 설정을 정의하는 블록입니다.
    defaultConfig {
        // 앱의 고유한 식별자(패키지 이름)를 정의합니다.
        applicationId = "com.example.quizapp_kotlin"
        // 앱이 실행될 수 있는 최소 안드로이드 API 레벨을 정의합니다.
        minSdk = 24
        // 앱이 대상으로 하는 안드로이드 SDK 버전을 정의합니다.
        targetSdk = 36
        // 앱의 버전 코드를 정의합니다. 업데이트 시마다 증가해야 합니다.
        versionCode = 1
        // 사용자에게 표시되는 앱의 버전 이름을 정의합니다.
        versionName = "1.0"

        // 안드로이드 테스트 코드를 실행하는 데 사용되는 러너를 정의합니다.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 빌드 유형(예: 디버그, 릴리스)에 대한 설정을 정의하는 블록입니다.
    buildTypes {
        // 'release' 빌드 유형에 대한 설정입니다. 앱 스토어에 배포할 때 사용됩니다.
        release {
            // 코드 난독화 및 최적화를 활성화할지 여부를 설정합니다. 'false'는 비활성화입니다.
            isMinifyEnabled = false
            // ProGuard 규칙 파일을 정의하여 코드 난독화를 세밀하게 제어할 수 있습니다.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    // 자바 컴파일러 옵션을 설정하는 블록입니다.
    compileOptions {
        // 소스 코드의 호환성을 정의합니다.
        sourceCompatibility = JavaVersion.VERSION_11
        // 컴파일된 바이트코드의 호환성을 정의합니다.
        targetCompatibility = JavaVersion.VERSION_11
    }
    // 코틀린 컴파일러 옵션을 설정하는 블록입니다.
    kotlinOptions {
        // 컴파일된 코드가 실행될 JVM 버전을 정의합니다.
        jvmTarget = "11"
    }
}

// 프로젝트에 필요한 종속성(라이브러리)을 정의하는 블록입니다.
dependencies {
    // 안드로이드 플랫폼의 핵심 기능을 위한 코틀린 확장 라이브러리입니다.
    implementation(libs.androidx.core.ktx)
    // 구형 안드로이드 버전에서 최신 기능을 사용할 수 있게 해주는 라이브러리입니다.
    implementation(libs.androidx.appcompat)
    // Material Design UI 컴포넌트 라이브러리입니다.
    implementation(libs.material)
    // Activity 컴포넌트 라이브러리입니다.
    implementation(libs.androidx.activity)
    // UI 레이아웃을 유연하게 구성하기 위한 라이브러리입니다.
    implementation(libs.androidx.constraintlayout)
    // 단위 테스트를 위한 라이브러리입니다.
    testImplementation(libs.junit)
    // 안드로이드 UI 테스트를 위한 라이브러리입니다.
    androidTestImplementation(libs.androidx.junit)
    // UI 테스트를 위한 Espresso 라이브러리입니다.
    androidTestImplementation(libs.androidx.espresso.core)
    // CSV 파일을 파싱하기 위해 외부에서 추가한 라이브러리입니다.
    implementation("com.opencsv:opencsv:5.9")

}