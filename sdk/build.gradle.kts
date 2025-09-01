import java.util.Date

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    `maven-publish`
}

val libName = "taonfc"

val skipCommitsCount = 0
val versionMajor = 1
val versionMinor = 0
val versionPatch = providers
    .exec {
        commandLine("git", "rev-list", "--count", "HEAD")
    }.standardOutput.asText
    .get()
    .trim()
    .toInt()

val versionName = "$versionMajor.$versionMinor.${versionPatch - skipCommitsCount}"

fun TaskContainer.registerCopyAarTask(variant: String) {
    val capVariant = variant.replaceFirstChar { it.uppercaseChar() }
    register<Delete>("deleteOld${capVariant}Aar") {
        group = "aar"
        description = "Удаляет ранее собранные AAR в ../aar для $variant"
        delete(fileTree("../aar") {
            include("$libName-$variant*.aar")
        })
    }

    register<Copy>("copy${capVariant}Aar") {
        group = "aar"
        description = "Copy AAR $variant with version $versionName to ../aar"
        dependsOn("assemble${capVariant}")
        dependsOn("deleteOld${capVariant}Aar")
        val aarFile = file("build/outputs/aar/$libName-$versionName-$variant.aar")
        doFirst {
            // Создать ../aar если не существует
            file("../aar").mkdirs()
            if (!aarFile.exists()) {
                throw GradleException("AAR file does not exist: $aarFile")
            }
            println("Copying $aarFile to ../aar/$libName-$variant.aar")
        }
        from(aarFile)
        into("../aar")
        if (variant == "release") {
            rename { "$libName.aar" }
        } else {
            rename { "$libName-$variant.aar" }
        }
        doLast {
            val versionFile = file("../aar/README.txt")
            versionFile.writeText("Library: $libName\nVersion: $versionName\nCreated: ${Date()}")
            println("Created version file: ${versionFile.absolutePath}")
        }
    }
}

tasks.registerCopyAarTask("release")
tasks.registerCopyAarTask("debug")

ktlint {
    android.set(true)
    filter {
        exclude("**/generated/**")
    }
}

detekt {
    config.setFrom(files("$projectDir/detekt.yml"))
    buildUponDefaultConfig = true
}

android {
    namespace = "ua.at.tsvetkov.nfcsdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.txt")
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.tao.core)
    implementation(libs.tao.log)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.mockito.core)
    androidTestImplementation(libs.mockito.inline)
    androidTestImplementation(libs.mockito.kotlin)
}

afterEvaluate {
    tasks.named("assembleDebug").configure {
        finalizedBy("copyDebugAar")
    }
    tasks.named("assembleRelease").configure {
        finalizedBy("copyReleaseAar")
    }
    tasks.named("build").configure {
        dependsOn("copyReleaseAar")
        dependsOn("copyDebugAar")
    }
}


val repo = "android-tao-nfc"
val repoDescription = "The NFC SDK library simplifies Near Field Communication (NFC) " +
        "interactions in Android applications."

val owner = "lordtao"
val libGroupId = "ua.at.tsvetkov"
val libArtifactId = libName
val libVersionName = versionName

val licenseName = "Apache License Version 2.0"
val licenseUrl = "http://www.apache.org/licenses/"

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = libGroupId
                artifactId = libArtifactId
                version = libVersionName

                from(components.getByName("release"))

                pom {
                    name.set(libArtifactId) // Или более описательное имя
                    description.set(repoDescription)
                    url.set("https://github.com/$owner/$repo") // URL of your project

                    licenses {
                        license {
                            name.set(licenseName)
                            url.set(licenseUrl)
                        }
                    }
                    developers {
                        developer {
                            id.set(owner)
                            name.set("Alexandr Tsvetkov")
                            email.set("tsvetkov2010@gmail.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/$owner/$repo.git")
                        developerConnection.set("scm:git:ssh://github.com/$owner/$repo.git")
                        url.set("https://github.com/$owner/$repo")
                    }
                }
            }
        }
    }
}
