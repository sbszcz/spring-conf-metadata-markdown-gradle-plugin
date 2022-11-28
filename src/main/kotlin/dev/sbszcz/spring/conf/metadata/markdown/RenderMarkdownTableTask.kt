package dev.sbszcz.spring.conf.metadata.markdown

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.nio.file.Files

object ReadmeWriter {

    private val START_TAG = Regex("<!--\\s*springconfmetadata\\s*-->", RegexOption.IGNORE_CASE)
    private val END_TAG = Regex("<!--\\s*/springconfmetadata\\s*-->", RegexOption.IGNORE_CASE)

    fun writeTextInsideMarker(content: String, targetFile: File) {

        val result = StringBuilder()
        var insideTag = false
        var contentHasBeenWritten = false

        for (line in targetFile.bufferedReader().readLines()) {

            if (START_TAG.containsMatchIn(line) && END_TAG.containsMatchIn(line)) {
                result.append(line).append("\n")
                continue
            }

            if (insideTag && !contentHasBeenWritten) {
                result.append(content).append("\n")
                contentHasBeenWritten = true
            }

            if (!insideTag || END_TAG.containsMatchIn(line)) {
                result.append(line).append("\n")
            }

            if (START_TAG.containsMatchIn(line)) {
                insideTag = true
            }
            if (END_TAG.containsMatchIn(line)) {
                insideTag = false
                contentHasBeenWritten = false
            }
        }

        Files.write(targetFile.toPath(), result.toString().toByteArray())
    }
}

fun StringBuilder.appendTableRows(jsonObject: JSONObject): java.lang.StringBuilder {

    for (prop in jsonObject.getJSONArray("properties")) {
        val property = prop as JSONObject
        val name = property.optString("name", "n/a")
        val type = property.optString("type", "n/a")
        val description = property.optString("description", "n/a")
        val defaultValue = property.optString("defaultValue", "n/a")
        val sourceType = property.optString("sourceType", "n/a")

        this.append("| $name | $type | $description | $defaultValue | $sourceType |\n")
    }

    return this
}

abstract class RenderMarkdownTableTask : DefaultTask() {

    @get:InputFiles
    @get:Optional
    abstract val springConfigMetadataJson: ConfigurableFileCollection

    @get:OutputFile
    @get:Optional
    abstract val readMeTarget: RegularFileProperty

    private val tableHeader = """
        | Name | Type | Description | Default | Source |
        |:---|:---|:---|:---|:---|
    """.trimIndent()

    init {
        this.group = "documentation"
        this.description = "Renders a markdown table of spring-configuration-metadata.json files."
    }

    @TaskAction
    fun render() {

        val readmeTargetFile = readMeTarget.get().asFile

        if (!readmeTargetFile.exists()) {
            throw GradleException("Markdown file '${readmeTargetFile.absolutePath}' does not exist.")
        }

        val projectFolderName = project.layout.projectDirectory.asFile.name

        val content = StringBuilder("")

        for (jsonFile in springConfigMetadataJson.files) {
            val sourcePath = sourcePath(jsonFile, projectFolderName)
            val tableContent = StringBuilder("")
            tableContent
                .append("**Source**: *${sourcePath}*")
                .append("\n\n")
                .append(tableHeader)
                .append("\n")

            try {
                tableContent.appendTableRows(JSONObject(jsonFile.readText()))
                content.append(tableContent)
            } catch (e: JSONException) {
                logger.error("error during json parsing ${jsonFile.absolutePath}", e)
                content
                    .append("**Source**: *${sourcePath}*\n\n")
                    .append("\tERROR: Invalid json format. ${e.message}\n\n")
                continue
            }
        }

        ReadmeWriter.writeTextInsideMarker(content.toString(), readmeTargetFile)
    }

    private fun sourcePath(jsonFile: File, projectFolderName: String): String {
        val pathElements = jsonFile.absolutePath.split("/").filter { it.isNotEmpty() }
        return pathElements.subList(pathElements.indexOf(projectFolderName), pathElements.size - 1)
            .joinToString("/", "/") + "/spring-configuration-metadata.json"
    }
}