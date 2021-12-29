import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    kotlin("multiplatform") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "com.lehaine"
version = "1.0"

repositories {
    mavenCentral()
    maven(url = "https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
    mavenLocal()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
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
    val littleKtVersion:String by project

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
        val jvmMain by getting {
            dependencies {
                implementation("com.lehaine.littlekt:core:$littleKtVersion")
            }
        }
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                val kotlinxHtmlVersion = "0.7.2"
                implementation("com.lehaine.littlekt:core:$littleKtVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:$kotlinxHtmlVersion")
            }

        }
        val jsTest by getting

        all {
            languageSettings.apply {
                progressiveMode = true
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlin.contracts.ExperimentalContracts")
                optIn("kotlin.ExperimentalUnsignedTypes")
                optIn("kotlin.ExperimentalStdlibApi")
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
                optIn("kotlin.time.ExperimentalTime")
            }
        }
    }
}