package dev.sbszcz.gradle.plugin

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions.asFileAttribute
import java.nio.file.attribute.PosixFilePermissions.fromString
import kotlin.io.path.Path
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.isDirectory
import kotlin.io.path.readText

fun Path.deleteRecursivelyIfExists(): Path {

    if(!this.toFile().exists())
        return this

    Files.walk(this).use { walk ->
        walk.sorted(Comparator.reverseOrder())
            .map (Path::toFile)
            .forEach (File::delete)
    }

    return this
}

fun Path.addText(text: String): Path {
    if(this.isDirectory())
        return this

    this.appendText(text)
    return this
}

class PluginFunctionalTest {

    lateinit var projectDir: Path
    lateinit var settingsFile: Path
    lateinit var buildFile: Path
    lateinit var subFolder1: Path
    lateinit var subFolder2: Path

    @BeforeEach
    fun setup() {

        projectDir = Path("build/test-project")
            .deleteRecursivelyIfExists()
            .createDirectories()

        settingsFile = projectDir.resolve("settings.gradle")
            .createFile()
            .addText("rootProject.name = 'world-domination'")

        buildFile = projectDir.resolve("build.gradle")
            .createFile()
            .addText("""
                plugins {
                    id("dev.sbszcz.spring-conf-metadata-to-markdown") version "0.0.1"
                }
            """.trimIndent())

        subFolder1 = projectDir.resolve("subfolder_1").createDirectories()
        subFolder2 = projectDir.resolve("subfolder_2").createDirectories()

    }

    @Test
    fun `renderMetadataTable task execution should fail when README_md does not exist`() {

        val exception = assertThrows<UnexpectedBuildFailure> {
            executeGradleWithArgs("renderMetadataTable")
        }
        assertThat(exception.buildResult.task(":renderMetadataTable")?.outcome).isEqualTo(FAILED)
        assertThat(exception.buildResult.output).contains("Markdown file '${projectDir.toFile().absolutePath}/README.md' does not exist")
    }

    @Test
    fun `renderMetadataTable task execution is successful for default README_md`() {

        val readmeFile = projectDir
            .resolve("README.md")
            .createFile(asFileAttribute(fromString("rw-------")))
            .addText("""
                <!-- springconfmetadata -->
                <!-- /springconfmetadata -->
            """.trimIndent())

        subFolder1.resolve("spring-configuration-metadata.json")
            .createFile()
            .addText(
                """
                  {
                  "properties": [
                    {
                      "name": "foo",
                      "type": "java.lang.Boolean",
                      "description": "example",
                      "sourceType": "Main"
                    }
                  ]
                }   
            """.trimIndent()
        )

        val result = executeGradleWithArgs("renderMetadataTable")

        assertThat(result.task(":renderMetadataTable")?.outcome).isEqualTo(SUCCESS)
        assertThat(readmeFile.readText()).isEqualTo("""
        <!-- springconfmetadata -->
        **Source**: */test-project/subfolder_1/spring-configuration-metadata.json*

        | Name | Type | Description | Default | Source |
        |:---|:---|:---|:---|:---|
        | foo | java.lang.Boolean | example | n/a | Main |

        <!-- /springconfmetadata -->
        
        """.trimIndent())

    }

    @Test
    fun `renderMetadataTable should render a table for each detected spring-configuration-metadata_json file`() {

        val readmeFile = projectDir
            .resolve("README.md")
            .createFile(asFileAttribute(fromString("rw-------")))
            .addText("""
                <!-- springconfmetadata -->
                <!-- /springconfmetadata -->
            """.trimIndent())

        subFolder1.resolve("spring-configuration-metadata.json")
            .createFile()
            .addText(
                """
                  {
                  "properties": [
                    {
                      "name": "foo",
                      "type": "java.lang.Boolean",
                      "description": "example",
                      "sourceType": "Main"
                    }
                  ]
                }   
            """.trimIndent()
            )

        subFolder2.resolve("spring-configuration-metadata.json")
            .createFile()
            .addText(
                """
                  {
                  "properties": [
                    {
                      "name": "bar",
                      "type": "java.lang.String",
                      "description": "example",
                      "sourceType": "Main",
                      "defaultValue": "bar"
                    }
                  ]
                }   
            """.trimIndent()
            )

        val result = executeGradleWithArgs("renderMetadataTable")

        assertThat(result.task(":renderMetadataTable")?.outcome).isEqualTo(SUCCESS)
        assertThat(readmeFile.readText()).isEqualTo("""
            <!-- springconfmetadata -->
            **Source**: */test-project/subfolder_1/spring-configuration-metadata.json*
            
            | Name | Type | Description | Default | Source |
            |:---|:---|:---|:---|:---|
            | foo | java.lang.Boolean | example | n/a | Main |
            **Source**: */test-project/subfolder_2/spring-configuration-metadata.json*
            
            | Name | Type | Description | Default | Source |
            |:---|:---|:---|:---|:---|
            | bar | java.lang.String | example | bar | Main |
            
            <!-- /springconfmetadata -->
        
        """.trimIndent())

    }

    private fun executeGradleWithArgs(vararg arguments: String): BuildResult = GradleRunner.create()
        .withProjectDir(projectDir.toFile())
        .withPluginClasspath()
        .withArguments(arguments.toList())
        .build()

}