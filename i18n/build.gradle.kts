import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.feedflow.library)
    alias(libs.plugins.ksp)
}

kotlin {
    androidLibrary {
        namespace = "com.prof18.feedflow.i18n"
    }

    sourceSets.commonMain {
        kotlin.srcDir("build/generated/ksp/android/androidMain/kotlin")
    }

    sourceSets.androidMain {
        kotlin.exclude("**/*FeedFlowStrings.kt")
    }
}

dependencies {
    add("kspAndroid", libs.lyricist.processorXml)
}

ksp {
    arg("lyricist.xml.resourcesPath", "$projectDir/src/commonMain/resources/locale")
    arg("lyricist.packageName", "com.prof18.feedflow.i18n")
    arg("lyricist.xml.moduleName", "FeedFlow")
    arg("lyricist.xml.defaultLanguageTag", "en")
    arg("lyricist.xml.generateComposeAccessors", "false")
}

tasks.withType<KotlinCompilationTask<*>>().all {
    if (name != "kspAndroidMain") {
        dependsOn("kspAndroidMain")
    }
}
