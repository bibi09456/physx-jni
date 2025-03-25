pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "physx-jni"

include("physx-jni")
include("physx-jni-android")
include("physx-jni-natives-windows")
//include("physx-jni-natives-windows-cuda")
include("physx-jni-natives-linux")
//include("physx-jni-natives-linux-cuda")
include("physx-jni-natives-macos")
include("physx-jni-natives-macos-arm64")
