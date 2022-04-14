plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.allopen")
}

description = "Qalipsis Plugins - Graphite"

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

tasks.withType<Test> {
    maxParallelForks = 1
}

val micronautVersion: String by project
val jacksonVersion: String by project
val catadioptreVersion: String by project

kotlin.sourceSets["test"].kotlin.srcDir("build/generated/source/kaptKotlin/catadioptre")
kapt.useBuildCache = false

dependencies {
    compileOnly("io.aeris-consulting:catadioptre-annotations:${catadioptreVersion}")
    implementation("io.micronaut:micronaut-http-server-netty")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    api("io.qalipsis:api-common:${project.version}")

    kapt("io.micronaut:micronaut-inject-java:$micronautVersion")
    kapt("io.aeris-consulting:catadioptre-annotations:${catadioptreVersion}")


    testImplementation("io.micronaut:micronaut-runtime")
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("io.qalipsis:test:${project.version}")

    kaptTest(platform("io.micronaut:micronaut-bom:$micronautVersion"))
    kaptTest("io.micronaut:micronaut-inject-java:$micronautVersion")
}