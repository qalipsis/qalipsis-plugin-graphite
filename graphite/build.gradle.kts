plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.allopen")
}

description = "Qalipsis Plugins - Graphite"

tasks.withType<Test> {
    maxParallelForks = 1
}

val micronautVersion: String by project

kotlin.sourceSets["test"].kotlin.srcDir("build/generated/source/kaptKotlin/catadioptre")
kapt.useBuildCache = false

dependencies {
    implementation("io.micronaut:micronaut-http-server-netty")

    api("io.qalipsis:api-common:${project.version}")

    kapt("io.micronaut:micronaut-inject-java:$micronautVersion")

    testImplementation("io.micronaut:micronaut-runtime")
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("io.qalipsis:test:${project.version}")

    kaptTest(platform("io.micronaut:micronaut-bom:$micronautVersion"))
    kaptTest("io.micronaut:micronaut-inject-java:$micronautVersion")
}