import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.feedflow.detekt)
}

kotlin {
    jvmToolchain(21)

    androidLibrary {
        namespace = "com.prof18.feedflow.shared"
        compileSdk = libs.versions.android.compile.sdk.get().toInt()
        minSdk = libs.versions.android.min.sdk.get().toInt()
        compilerOptions.jvmTarget = JvmTarget.JVM_21

        withHostTest {}

        androidResources {
            enable = true
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        all {
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
            languageSettings.optIn("kotlin.time.ExperimentalTime")
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            languageSettings.optIn("kotlinx.coroutines.FlowPreview")
            languageSettings.optIn("co.touchlab.kermit.ExperimentalKermitApi")
        }

        commonMain {
            dependencies {
                implementation(project(":database"))
                implementation(project(":feedSync:database"))
                implementation(project(":feedSync:greader"))
                implementation(project(":feedSync:networkcore"))
                implementation(project(":feedSync:feedbin"))
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.koin.core.vm)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.com.prof18.rss.parser)
                implementation(libs.csv)
                implementation(libs.multiplatform.settings)
                implementation(libs.kotlinx.date.time)
                implementation(libs.ktor.client.core)
                implementation(libs.stately.concurrency)

                api(project(":core"))
                api(project(":i18n"))
                api(project(":feedSync:dropbox"))
                api(project(":feedSync:googledrive"))
                api(libs.touchlab.kermit)
                api(libs.immutable.collections)
                api(libs.androidx.lifecycle.viewModel)
            }
        }

        commonTest {
            dependencies {
                implementation(project(":database"))
                implementation(project(":feedSync:feedbin"))
                implementation(project(":feedSync:test-utils"))
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.touchlab.kermit.test)
                implementation(libs.koin.test)
                implementation(libs.turbine)
                implementation(libs.multiplatform.settings.test)
                implementation(libs.ktor.client.mock)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.jsoup)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.koin.android)
                implementation(libs.workmanager)
                implementation(libs.koin.workmanager)
            }
        }

        getByName("androidHostTest") {
            dependsOn(commonTest.get())

            dependencies {
                implementation(libs.kotlin.test.junit)
                implementation(libs.junit)
                implementation(libs.org.robolectric)
                implementation(libs.androidx.test.core.ktx)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }

    }
}

// Configure TEST_RESOURCES_ROOT for Android host tests.
val testResourcesDir = project(":feedSync:test-utils")
    .file("src/commonMain/resources")
    .absolutePath

tasks.withType<Test> {
    environment("TEST_RESOURCES_ROOT", testResourcesDir)
}
