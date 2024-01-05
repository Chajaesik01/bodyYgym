
buildscript {
    val agp_version by extra("7.2.0")
    val agp_version1 by extra("4.2.2")
    val agp_version2 by extra("7.0.0")
    val agp_version3 by extra(agp_version)
    val agp_version4 by extra("7.4.0")
    val agp_version5 by extra(agp_version3)
    val agp_version6 by extra(agp_version5)
    val agp_version7 by extra(agp_version5)
    val agp_version8 by extra("7.4.0")
    repositories {
        // ...
    }
    dependencies {
        // ...
        classpath("com.google.gms:google-services:4.3.14")// Google Services plugin
        classpath ("com.android.tools.build:gradle:$agp_version8")
    }
}

allprojects {
    repositories {
        // ...
        // ...
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.



plugins {
    id("com.android.application") version "7.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false

}