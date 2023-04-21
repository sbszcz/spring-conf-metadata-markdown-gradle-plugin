package dev.sbszcz.spring.conf.metadata.markdown

import dev.sbszcz.spring.conf.metadata.markdown.Column.Default
import dev.sbszcz.spring.conf.metadata.markdown.Column.Deprecation
import dev.sbszcz.spring.conf.metadata.markdown.Column.Description
import dev.sbszcz.spring.conf.metadata.markdown.Column.Name
import dev.sbszcz.spring.conf.metadata.markdown.Column.Source
import dev.sbszcz.spring.conf.metadata.markdown.Column.Type
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

        Files.write(targetFile.toPath(), result.toString().trimEnd().toByteArray())
    }
}

enum class Column {
    Name, Type, Description, Default, Source, Deprecation
}

fun StringBuilder.appendTableHeader(rows: List<Map<Column, String>>): java.lang.StringBuilder {

    this.append("|")

    val availableColumns: Set<Column> = rows.firstOrNull()?.keys ?: emptySet()

    Column.values().forEach { column ->
        if (availableColumns.contains(column)){
            this.append(" ${column.name} |")
        }
    }

    this.append("\n")
    this.append("|")
    availableColumns.forEach { this.append(":---|") }

    return this
}

fun StringBuilder.appendTableRows(rows: List<Map<Column, String>>): java.lang.StringBuilder {

    this.append("|")

    for (row in rows) {
        this
            .append(" ${row[Name]} |")
            .append(" ${row[Type]} |")
            .append(" ${row[Description]} |")
            .append(" ${row[Default]} |")
            .append(" ${row[Source]} |")

        if(row.containsKey(Deprecation)){
            this.append(" ${row[Deprecation]} |")
        }

        this.append("\n")
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
            val json = JSONObject(jsonFile.readText())
            val rows =  collectRows(json)

            val tableContent = StringBuilder("")

            tableContent
                .append("**Source**: *${sourcePath}*")
                .append("\n\n")
                .appendTableHeader(rows)
                .append("\n")

            try {
                tableContent
                    .appendTableRows(rows)
                    .append("\n")
                content.append(tableContent)
            } catch (e: JSONException) {
                logger.error("error during json parsing ${jsonFile.absolutePath}", e)
                content
                    .append("**Source**: *${sourcePath}*\n\n")
                    .append("\tERROR: Invalid json format. ${e.message}\n\n")
                continue
            }
        }

        ReadmeWriter.writeTextInsideMarker(content.toString().trimEnd(), readmeTargetFile)
    }

    private fun collectRows(jsonObject: JSONObject): List<Map<Column, String>>{

        return jsonObject.getJSONArray("properties").map {
            val property = it as JSONObject
            val row = mutableMapOf<Column, String>(
                Name to property.optString("name", "n/a"),
                Type to property.optString("type", "n/a"),
                Description to property.optString("description", "n/a"),
                Default to property.optString("defaultValue", "n/a"),
                Source to property.optString("sourceType", "n/a"),
            )

            val deprecation = property.optJSONObject("deprecation", null)
            if (deprecation != null){
                val level =  deprecation.optString("level", "n/a")
                val reason =  deprecation.optString("reason", "n/a")
                val replacement =  deprecation.optString("replacement", "n/a")
                row[Deprecation] = "level: $level, reason: $reason, replacement: $replacement"
            }

            row.toMap()
        }


//            val name = property.optString("name", "n/a")
//            this.append("$name |")
//
//            val type = property.optString("type", "n/a")
//            this.append(" $type |")
//
//            val description = property.optString("description", "n/a")
//            this.append(" $description |")
//
//            val defaultValue = property.optString("defaultValue", "n/a")
//            this.append(" $defaultValue |")
//
//            val sourceType = property.optString("sourceType", "n/a")
//            this.append(" $sourceType |")

//            this.append("\n")
    }

    private fun sourcePath(jsonFile: File, projectFolderName: String): String {
        val pathElements = jsonFile.absolutePath.split("/").filter { it.isNotEmpty() }
        return pathElements.subList(pathElements.indexOf(projectFolderName), pathElements.size - 1)
            .joinToString("/", "/") + "/spring-configuration-metadata.json"
    }
}