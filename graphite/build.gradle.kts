/*
 * Copyright 2022 AERIS IT Solutions GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.allopen")
}

description = "QALIPSIS plugin for Graphite"

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.majorVersion
        javaParameters = true
    }
}

tasks.withType<Test> {
    // Enables the search of memory leaks in the Netty buffers when running all tests.
    systemProperties("io.netty.leakDetectionLevel" to "paranoid")
}

allOpen {
    annotations(
        "io.micronaut.aop.Around",
        "jakarta.inject.Singleton",
        "io.qalipsis.api.annotations.StepConverter",
        "io.qalipsis.api.annotations.StepDecorator",
        "io.qalipsis.api.annotations.PluginComponent",
        "io.qalipsis.api.annotations.Spec",
        "io.micronaut.validation.Validated"
    )
}


kotlin.sourceSets["test"].kotlin.srcDir("build/generated/source/kaptKotlin/catadioptre")
kapt.useBuildCache = false

val pluginPlatformVersion: String by project

dependencies {
    implementation(platform("io.qalipsis:plugin-platform:${pluginPlatformVersion}"))
    compileOnly("io.aeris-consulting:catadioptre-annotations")
    compileOnly("io.micronaut:micronaut-runtime")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    implementation("io.netty:netty-handler")
    implementation("io.netty:netty-transport")
    implementation(group = "io.netty", name = "netty-transport-native-epoll", classifier = "linux-x86_64")
    implementation(group = "io.netty", name = "netty-transport-native-kqueue", classifier = "osx-x86_64")
    implementation("io.netty:netty-buffer")
    implementation("io.micronaut.micrometer:micronaut-micrometer-registry-graphite")
    implementation("io.ktor:ktor-client-cio:2.2.4")
    implementation("io.ktor:ktor-client-core:2.2.4")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    kapt(platform("io.qalipsis:plugin-platform:${pluginPlatformVersion}"))
    kapt("io.qalipsis:api-processors")
    kapt("io.qalipsis:api-dsl")
    kapt("io.qalipsis:api-common")
    kapt("io.aeris-consulting:catadioptre-annotations")
    kapt("io.micronaut:micronaut-inject-java")

    testImplementation(platform("io.qalipsis:plugin-platform:${pluginPlatformVersion}"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("io.micronaut:micronaut-runtime")
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("io.qalipsis:test")
    testImplementation("io.qalipsis:api-common")
    testImplementation("io.qalipsis:api-dsl")
    testImplementation("io.qalipsis:api-dev")
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation(testFixtures("io.qalipsis:api-dsl"))
    testImplementation(testFixtures("io.qalipsis:api-common"))
    testImplementation(testFixtures("io.qalipsis:runtime"))
    testImplementation("javax.annotation:javax.annotation-api")
    testImplementation("io.micronaut:micronaut-runtime")
    testImplementation("io.aeris-consulting:catadioptre-kotlin")
    testImplementation("org.awaitility:awaitility-kotlin")
    testRuntimeOnly("io.qalipsis:runtime")
    testRuntimeOnly("io.qalipsis:head")
    testRuntimeOnly("io.qalipsis:factory")

    kaptTest(platform("io.qalipsis:plugin-platform:${pluginPlatformVersion}"))
    kaptTest("io.micronaut:micronaut-inject-java")
    kaptTest("io.qalipsis:api-processors")
}