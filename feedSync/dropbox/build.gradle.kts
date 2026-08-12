plugins {
    alias(libs.plugins.feedflow.library)
}

kotlin {
    androidLibrary {
        namespace = "com.prof18.feedflow.feedsync.dropbox"
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":core"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.touchlab.kermit)
                implementation(libs.multiplatform.settings)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.dropbox.core)
                api(libs.dropbox.core.android)
            }
        }
    }
}
