import com.lehaine.littlekt.gradle.texturepacker.littleKt
import com.lehaine.littlekt.gradle.texturepacker.packing
import com.lehaine.littlekt.gradle.texturepacker.texturePacker
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

buildscript {
    val littleKtVersion: String by project
    val androidPluginVersion: String by project
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven(url ="https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
    dependencies {
        classpath("com.lehaine.littlekt.gradle:texturepacker:$littleKtVersion")
        classpath("com.android.tools.build:gradle:$androidPluginVersion")
    }
}

plugins {
    kotlin("multiplatform") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
    id("com.android.application") version "7.3.1"
    id("com.lehaine.littlekt.gradle.texturepacker") version "0.8.0"
}

group = "com.lehaine.littlekt.samples"
version = "1.0"

repositories {
    google()
    mavenLocal()
    mavenCentral()
    maven(url ="https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven(url = "https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

littleKt {
    texturePacker {
        inputDir = "art/export_tiles/"
        outputDir = "src/commonMain/resources/"
        outputName = "tiles.atlas"
        packing {
            extrude = 2
        }
    }
}

kotlin {
    android()
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    js(KotlinJsCompilerType.IR) {
        binaries.executable()
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }

        this.attributes.attribute(
            KotlinPlatformType.attribute,
            KotlinPlatformType.js
        )

        compilations.all {
            kotlinOptions.sourceMap = true
        }
    }
    val kotlinCoroutinesVersion: String by project
    val littleKtVersion: String by project

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.lehaine.littlekt:core:$littleKtVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                val kotlinxHtmlVersion = "0.7.2"
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:$kotlinxHtmlVersion")
            }

        }
        val jsTest by getting

        val androidMain by getting {
            dependencies {
                implementation("com.lehaine.littlekt:core:$littleKtVersion")
            }
        }
    }
}

android {
    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        assets.srcDirs("src/commonMain/resources")
    }
    compileSdk = (findProperty("android.compileSdk") as String).toInt()

    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

rootProject.extensions.configure<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension> {
    versions.webpackCli.version = "4.10.0"
}