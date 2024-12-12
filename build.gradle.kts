import com.littlekt.gradle.texturepacker.littleKt
import com.littlekt.gradle.texturepacker.packing
import com.littlekt.gradle.texturepacker.texturePacker
import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

buildscript {
    val littleKtVersion: String by project
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven(url ="https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
    dependencies {
        classpath("com.littlekt.gradle:texturepacker:$littleKtVersion")
    }
}

plugins {
    kotlin("multiplatform") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.littlekt.gradle.texturepacker") version "0.10.2"
}

group = "com.littlekt.samples"
version = "1.0"

repositories {
    mavenLocal()
    google()
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
    tasks.withType<JavaExec> { jvmArgs("--enable-preview", "--enable-native-access=ALL-UNNAMED") }
    jvm {
        compilerOptions { jvmTarget = JvmTarget.JVM_21 }
        compilations {
            val main by getting

            val mainClassName = (findProperty("jvm.mainClass") as? String)?.plus("Kt")
            if (mainClassName == null) {
                project.logger.log(
                    LogLevel.ERROR,
                    "Property 'jvm.mainClass' has either changed or has not been set. Check 'gradle.properties' and ensure it is properly set!"
                )
            }
            tasks {
                register<Copy>("copyResources") {
                    group = "package"
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    dependsOn(named("jvmProcessResources"))
                    from(main.output.resourcesDir)
                    destinationDir = File("${layout.buildDirectory.asFile.get()}/publish")
                }
                register<Jar>("packageFatJar") {
                    group = "package"
                    archiveClassifier.set("all")
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    dependsOn(named("jvmJar"))
                    dependsOn(named("copyResources"))
                    manifest { attributes["Main-Class"] = mainClassName }
                    destinationDirectory.set(File("${layout.buildDirectory.asFile.get()}/publish/"))
                    from(
                        main.runtimeDependencyFiles.map { if (it.isDirectory) it else zipTree(it) },
                        main.output.classesDirs
                    )
                    doLast {
                        logger.lifecycle(
                            "[LittleKt] The packaged jar is available at: ${outputs.files.first().parent}"
                        )
                    }
                }
                if (Os.isFamily(Os.FAMILY_MAC)) {
                    register<JavaExec>("jvmRun") {
                        jvmArgs("-XstartOnFirstThread")
                        mainClass.set(mainClassName)
                        kotlin {
                            val mainCompile = targets["jvm"].compilations["main"]
                            dependsOn(mainCompile.compileAllTaskName)
                            classpath(
                                { mainCompile.output.allOutputs.files },
                                (configurations["jvmRuntimeClasspath"])
                            )
                        }
                    }
                }
            }
        }
    }
    js {
        binaries.executable()
        browser {
            testTask { useKarma { useChromeHeadless() } }
            commonWebpackConfig {
                devServer =
                    (devServer
                        ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
                            .DevServer())
                        .copy(
                            open = mapOf("app" to mapOf("name" to "chrome")),
                        )
            }
        }

        this.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)

        compilerOptions { sourceMap = true }
    }
    val kotlinCoroutinesVersion: String by project
    val littleKtVersion: String by project

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.littlekt:core:$littleKtVersion")
                implementation("com.littlekt:scene-graph:$littleKtVersion")
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
                val kotlinxHtmlVersion = "0.11.0"
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:$kotlinxHtmlVersion")
            }

        }
        val jsTest by getting
    }
}