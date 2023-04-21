package dev.sbszcz.gradle.plugin

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.exists
import assertk.assertions.isEqualTo
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.FAILED
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.intellij.lang.annotations.Language
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
import kotlin.io.path.createDirectory
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

    @Language("json")
    val SPRING_CONF_METADATA_SAMPLE_1 = """
    {
      "properties": [
        {
          "name": "foo",
          "type": "java.lang.Boolean",
          "description": "example",
          "sourceType": "Main"
        }
      ]
    }""".trimIndent()

    @Language("json")
    val SPRING_CONF_METADATA_SAMPLE_2 = """
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
    }""".trimIndent()

    @Language("json")
    val SPRING_CONF_METADATA_SAMPLE_3 = """
    {
      "properties": [
        {
          "name": "bar",
          "type": "java.lang.String",
          "description": "example",
          "sourceType": "Main",
          "defaultValue": "bar",
          "deprecation": {
            "level": "warning",
            "reason": "because its broken",
            "replacement": "use something else"
          }
        }
      ]
    }""".trimIndent()

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
                    id("io.github.sbszcz.spring-conf-metadata-to-markdown") version "0.0.1"
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

        val readmeFile = createReadmeFileWithTags()

        subFolder1.resolve("spring-configuration-metadata.json")
            .createFile()
            .addText(SPRING_CONF_METADATA_SAMPLE_1)

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
    fun `renderMetadataTable should render spring-configuration-metadata_json with optional deprecation element`() {

        val readmeFile = createReadmeFileWithTags()

        subFolder1.resolve("spring-configuration-metadata.json")
            .createFile()
            .addText(SPRING_CONF_METADATA_SAMPLE_3)

        val result = executeGradleWithArgs("renderMetadataTable")

        assertThat(result.task(":renderMetadataTable")?.outcome).isEqualTo(SUCCESS)
        assertThat(readmeFile.readText()).isEqualTo("""
        <!-- springconfmetadata -->
        **Source**: */test-project/subfolder_1/spring-configuration-metadata.json*

        | Name | Type | Description | Default | Source | Deprecation |
        |:---|:---|:---|:---|:---|:---|
        | bar | java.lang.String | example | bar | Main | level: warning, reason: because its broken, replacement: use something else |
        <!-- /springconfmetadata -->
        """.trimIndent())

    }

    @Test
    fun `renderMetadataTable task execution is successful for custom markdown file`() {

        buildFile.addText("""
            springConfMetadataMarkdown {
                readMeTarget.set(project.file("documentation.md"))
            }
        """.trimIndent())

        val customMarkdownFile = projectDir
            .resolve("documentation.md")
            .createFile(asFileAttribute(fromString("rw-------")))
            .addText(
                """
                <!-- springconfmetadata -->
                <!-- /springconfmetadata -->
                """.trimIndent()
            )

        subFolder1.resolve("spring-configuration-metadata.json")
            .createFile()
            .addText(SPRING_CONF_METADATA_SAMPLE_1)

        val result = executeGradleWithArgs("renderMetadataTable")

        assertThat(result.task(":renderMetadataTable")?.outcome).isEqualTo(SUCCESS)
        assertThat(customMarkdownFile.readText()).isEqualTo("""
            <!-- springconfmetadata -->
            **Source**: */test-project/subfolder_1/spring-configuration-metadata.json*
            
            | Name | Type | Description | Default | Source |
            |:---|:---|:---|:---|:---|
            | foo | java.lang.Boolean | example | n/a | Main |
            <!-- /springconfmetadata -->
            """.trimIndent())

    }

    @Test
    fun `renderMetadataTable task should do nothing if marker tags are in one line`() {
        val readmeFile = projectDir
            .resolve("README.md")
            .createFile(asFileAttribute(fromString("rw-------")))
            .addText("<!-- springconfmetadata --><!-- /springconfmetadata -->")

        subFolder1.resolve("spring-configuration-metadata.json")
            .createFile()
            .addText(SPRING_CONF_METADATA_SAMPLE_1)

        val result = executeGradleWithArgs("renderMetadataTable")

        assertThat(result.task(":renderMetadataTable")?.outcome).isEqualTo(SUCCESS)
        assertThat(readmeFile.readText()).isEqualTo("<!-- springconfmetadata --><!-- /springconfmetadata -->")
    }

    @Test
    fun `renderMetadataTable should render a table for each detected spring-configuration-metadata_json file`() {

        val readmeFile = createReadmeFileWithTags()

        subFolder1.resolve("spring-configuration-metadata.json")
            .createFile()
            .addText(SPRING_CONF_METADATA_SAMPLE_1)

        subFolder2.resolve("spring-configuration-metadata.json")
            .createFile()
            .addText(SPRING_CONF_METADATA_SAMPLE_2)

        val result = executeGradleWithArgs("renderMetadataTable")

        assertThat(result.task(":renderMetadataTable")?.outcome).isEqualTo(SUCCESS)

        val actual = readmeFile.readText()

        println("======================= ACTUAL =================================")
        println(actual)
        println("======================= ACTUAL =================================")

        assertThat(actual).isEqualTo("""
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

    @Test
    fun `renderMetadataTable task should ignore spring-configuration-metadata_json in build folders`() {

        val readmeFile = createReadmeFileWithTags()

        subFolder1.resolve("spring-configuration-metadata.json")
            .createFile()
            .addText(SPRING_CONF_METADATA_SAMPLE_1)

        val ignoredConfig = projectDir.resolve("build")
            .createDirectory()
            .resolve("spring-configuration-metadata.json")
            .createFile()
            .addText(SPRING_CONF_METADATA_SAMPLE_2)

        assertThat(ignoredConfig.toFile()).exists()

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
    fun `renderMetadataTable should work correctly with gradle caching features`() {

        val readmeFile = createReadmeFileWithTags()

        val springConfMetadataFile = subFolder1.resolve("spring-configuration-metadata.json")
            .createFile()
            .addText(SPRING_CONF_METADATA_SAMPLE_1)

        // initial task execution
        var result = executeGradleWithArgs("renderMetadataTable")
        assertThat(result.task(":renderMetadataTable")?.outcome).isEqualTo(SUCCESS)

        // second task execution without input/output changes
        result = executeGradleWithArgs("renderMetadataTable")

        assertThat(result.task(":renderMetadataTable")?.outcome).isEqualTo(UP_TO_DATE)
        assertThat(readmeFile.readText()).isEqualTo("""
        <!-- springconfmetadata -->
        **Source**: */test-project/subfolder_1/spring-configuration-metadata.json*

        | Name | Type | Description | Default | Source |
        |:---|:---|:---|:---|:---|
        | foo | java.lang.Boolean | example | n/a | Main |
        <!-- /springconfmetadata -->
        """.trimIndent())

        // change task input file
        springConfMetadataFile.addText("\n\n")

        result = executeGradleWithArgs("renderMetadataTable")
        assertThat(result.task(":renderMetadataTable")?.outcome).isEqualTo(SUCCESS)

        // third task execution without input/output changes
        result = executeGradleWithArgs("renderMetadataTable")
        assertThat(result.task(":renderMetadataTable")?.outcome).isEqualTo(UP_TO_DATE)

        // change task output
        readmeFile.addText("\n\nA new line")
        result = executeGradleWithArgs("renderMetadataTable")
        assertThat(result.task(":renderMetadataTable")?.outcome).isEqualTo(SUCCESS)

        assertThat(readmeFile.readText()).isEqualTo("""
        <!-- springconfmetadata -->
        **Source**: */test-project/subfolder_1/spring-configuration-metadata.json*

        | Name | Type | Description | Default | Source |
        |:---|:---|:---|:---|:---|
        | foo | java.lang.Boolean | example | n/a | Main |
        <!-- /springconfmetadata -->
        
        A new line
        """.trimIndent())

        // fourth task execution without input/output changes
        result = executeGradleWithArgs("renderMetadataTable")
        assertThat(result.task(":renderMetadataTable")?.outcome).isEqualTo(UP_TO_DATE)

        // change task input by adding a second spring-configuration-metadata.json file
        subFolder2.resolve("spring-configuration-metadata.json")
            .createFile()
            .addText(SPRING_CONF_METADATA_SAMPLE_2)

        result = executeGradleWithArgs("renderMetadataTable")
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
            
            A new line
            """.trimIndent())

    }

    private fun createReadmeFileWithTags() = projectDir
        .resolve("README.md")
        .createFile(asFileAttribute(fromString("rw-------")))
        .addText(
            """
            <!-- springconfmetadata -->
            <!-- /springconfmetadata -->
            """.trimIndent()
        )


    private fun executeGradleWithArgs(vararg arguments: String): BuildResult = GradleRunner.create()
        .withProjectDir(projectDir.toFile())
        .withPluginClasspath()
        .withArguments(arguments.toList())
//        .withDebug(true)
        .build()

}