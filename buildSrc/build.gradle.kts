/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

import java.util.*
import java.io.FileInputStream

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    mavenLocal()
    if (project.hasProperty("kotlin_repo_url")) {
        maven(project.properties["kotlin_repo_url"] as String)
    }
    // kotlin-dev with space redirector
    maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
}

val kotlinVersion = run {
    if (project.hasProperty("build_snapshot_train")) {
        val ver = project.properties["kotlin_snapshot_version"] as? String
        require(!ver.isNullOrBlank()) {"kotlin_snapshot_version must be present if build_snapshot_train is used" }
        return@run ver
    }
    if (project.hasProperty("kotlin_repo_url")) {
        val ver = project.properties["kotlin_version"] as? String
        require(!ver.isNullOrBlank()) {"kotlin_version must be present if kotlin_repo_url is used" }
        return@run ver
    }
    val targetProp = if (project.hasProperty("bootstrap")) "kotlin.version.snapshot" else "kotlin.version"
    FileInputStream(file("../gradle.properties")).use { propFile ->
        val ver = project.findProperty("kotlin.version")?.toString() ?: Properties().apply { load(propFile) }[targetProp]
        require(ver is String) { "$targetProp must be string in ../gradle.properties, got $ver instead" }
        ver
    }
}

dependencyLocking {
    lockAllConfigurations()
}

listOf(
    Jar::class,
    Zip::class,
    Tar::class,
).forEach {
    tasks.withType(it).configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true

        when(this) {
            is Zip -> isZip64 = true
        }
    }
}

dependencies {
    implementation(kotlin("gradle-plugin", kotlinVersion))

    // fix from the Gradle team: makes version catalog symbols available in build scripts
    // see here for more: https://github.com/gradle/gradle/issues/15383
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}