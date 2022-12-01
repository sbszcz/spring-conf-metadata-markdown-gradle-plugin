import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT

plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.1.0"
    kotlin("jvm") version "1.7.20"
}

group = "io.github.sbszcz"
version = "0.0.2"

repositories {
    mavenCentral()
}

dependencies {

    implementation("org.json:json:20220924")

    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    website.set("https://github.com/sbszcz/spring-conf-metadata-markdown-gradle-plugin")
    vcsUrl.set("https://github.com/sbszcz/spring-conf-metadata-markdown-gradle-plugin")
    plugins {
        create("SpringConfMetadataMarkdownGradlePlugin") {
            id = "io.github.sbszcz.spring-conf-metadata-to-markdown"
            displayName = "Spring Configuration Metadata Markdown Generator"
            description = "A gradle plugin that converts spring-configuration-metadata.json files into markdown tables and writes them into a markdown file."
            tags.set(listOf("spring", "documentation", "markdown"))
            implementationClass = "dev.sbszcz.spring.conf.metadata.markdown.SpringConfMetadataMarkdownGradlePlugin"
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(
            STANDARD_OUT,
            STANDARD_ERROR,
            PASSED,
            SKIPPED,
            FAILED
        )

        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    outputs.upToDateWhen { false }
}
