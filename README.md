plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.nova.tv.future'
    compileSdk 33

    defaultConfig {
        applicationId "com.nova.tv.future"
        minSdk 22 // Fire TV OS 7 (Android 9) માટે પરફેક્ટ
        targetSdk 33
        versionCode 1
        versionName "1.0.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = '11'
    }
    buildFeatures {
        viewBinding true
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.9.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.8.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    
    // TV Leanback UI અને RecyclerView
    implementation 'androidx.leanback:leanback:1.0.0'
    implementation 'androidx.recyclerview:recyclerview:1.3.0'
    
    // Image loading માટે (Apps ના આઇકોન સેટ કરવા માટે)
    implementation 'com.github.bumptech.glide:glide:4.15.1'
    annotationProcessor 'com.github.bumptech.glide:compiler:4.15.1'
}
