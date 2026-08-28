plugins {
    id("com.android.application")
}

android {
    namespace = "com.ecrinlabs.repo2play"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ecrinlabs.repo2play"
        minSdk = 26
        targetSdk = 36
        versionCode = 13
        versionName = "12.1.0"
    }

    signingConfigs {
        create("release") {
            val ks = System.getenv("R2P_KEYSTORE_PATH")
            if (!ks.isNullOrBlank()) {
                storeFile = file(ks)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE",
            "META-INF/LICENSE.txt",
            "META-INF/NOTICE",
            "META-INF/NOTICE.txt"
        )
    }
}
