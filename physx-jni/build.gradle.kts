plugins {
    alias(libs.plugins.webidl)
    `maven-publish`
    signing
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withSourcesJar()
    withJavadocJar()

    sourceSets {
        main {
            java {
                srcDir("src/main/generated")
            }
        }
    }
}

webidl {
    modelPath.set(file("${projectDir}/src/main/webidl/"))
    modelName.set("PhysXJni")

    generateJni {
        javaClassesOutputDirectory.set(file("$projectDir/src/main/generated/physx"))
        nativeGlueCodeOutputFile.set(file("${rootDir}/PhysX/physx/source/webidlbindings/src/jni/PhysXJniGlue.h"))

        packagePrefix.set("physx")
        onClassLoadStatement.set("de.fabmax.physxjni.Loader.load();")
        nativeIncludeDir.set(file("$rootDir/PhysX/physx/include"))
    }

    generateCompactWebIdl {
        outputFile.set(file("${rootDir}/PhysX/physx/source/webidlbindings/src/wasm/PhysXWasm.idl"))
    }
}

tasks.javadoc {
    val opts = options as StandardJavadocDocletOptions
    opts.addStringOption("Xdoclint:none", "-quiet")
}

// make sure version string constants in main project and platform projects match the gradle project version
tasks.register<VersionNameUpdate>("updateVersionNames") {
    versionName = "$version"
    filesToUpdate = listOf(
        "${rootDir}/physx-jni/src/main/java/de/fabmax/physxjni/Loader.java",
        "${rootDir}/physx-jni-natives-windows/src/main/java/de/fabmax/physxjni/windows/NativeLibWindows.java",
        "${rootDir}/physx-jni-natives-windows-cuda/src/main/java/de/fabmax/physxjni/windows/NativeLibWindows.java",
        "${rootDir}/physx-jni-natives-linux/src/main/java/de/fabmax/physxjni/linux/NativeLibLinux.java",
        "${rootDir}/physx-jni-natives-linux-cuda/src/main/java/de/fabmax/physxjni/linux/NativeLibLinux.java",
        "${rootDir}/physx-jni-natives-macos/src/main/java/de/fabmax/physxjni/macos/NativeLibMacos.java",
        "${rootDir}/physx-jni-natives-macos-arm64/src/main/java/de/fabmax/physxjni/macosarm/NativeLibMacosArm64.java"
    )
}

tasks["clean"].doLast {
    delete("$projectDir/src/main/generated")
}

tasks.compileJava {
    dependsOn("generateJniJavaBindings")
    dependsOn("updateVersionNames")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)

    //testRuntimeOnly(project(":physx-jni-natives-windows-cuda"))
    //testRuntimeOnly(project(":physx-jni-natives-linux-cuda"))
    testRuntimeOnly(project(":physx-jni-natives-windows"))
    testRuntimeOnly(project(":physx-jni-natives-linux"))
    testRuntimeOnly(project(":physx-jni-natives-macos"))
    testRuntimeOnly(project(":physx-jni-natives-macos-arm64"))

    testImplementation(libs.lwjgl.core)

    val os = org.gradle.internal.os.OperatingSystem.current()
    val arch = System.getProperty("os.arch", "unknown")
    val lwjglPlatform = when {
        os.isLinux -> "natives-linux"
        os.isMacOsX && arch == "aarch64" -> "natives-macos-arm64"
        os.isMacOsX && arch != "aarch64" -> "natives-macos"
        else -> "natives-windows"
    }
    testRuntimeOnly("${libs.lwjgl.core.get()}:$lwjglPlatform")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifact(project(":physx-jni-natives-windows").tasks["jar"]).apply {
                classifier = "natives-windows"
            }
            artifact(project(":physx-jni-natives-linux").tasks["jar"]).apply {
                classifier = "natives-linux"
            }
            artifact(project(":physx-jni-natives-macos").tasks["jar"]).apply {
                classifier = "natives-macos"
            }
            artifact(project(":physx-jni-natives-macos-arm64").tasks["jar"]).apply {
                classifier = "natives-macos-arm64"
            }

            pom {
                name.set("physx-jni")
                description.set("JNI bindings for Nvidia PhysX.")
                url.set("https://github.com/fabmax/physx-jni")
                developers {
                    developer {
                        name.set("Max Thiele")
                        email.set("fabmax.thiele@gmail.com")
                        organization.set("github")
                        organizationUrl.set("https://github.com/fabmax")
                    }
                }
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/fabmax/physx-jni.git")
                    developerConnection.set("scm:git:ssh://github.com:fabmax/physx-jni.git")
                    url.set("https://github.com/fabmax/physx-jni/tree/main")
                }
            }
        }
    }

    repositories {
        maven {
            if (version.toString().endsWith("-SNAPSHOT")) {
                url = uri("https://central.sonatype.com/repository/maven-snapshots/")
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            } else {
                url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/")
            }
        }
    }

    val props = LocalProperties.get(project)
    if (!props.publishUnsigned) {
        signing {
            publications.forEach {
                val privateKey = props["GPG_PRIVATE_KEY"]
                val password = props["GPG_PASSWORD"]
                useInMemoryPgpKeys(privateKey, password)
                sign(it)
            }
        }
    }
}
