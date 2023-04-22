package dev.sbszcz.spring.conf.metadata.markdown

import dev.sbszcz.spring.conf.metadata.markdown.Column.Default
import dev.sbszcz.spring.conf.metadata.markdown.Column.Description
import dev.sbszcz.spring.conf.metadata.markdown.Column.Name
import dev.sbszcz.spring.conf.metadata.markdown.Column.Type
import org.gradle.api.Plugin
import org.gradle.api.Project

class SpringConfMetadataMarkdownGradlePlugin : Plugin<Project> {

    companion object {
        const val DEFAULT_README = "README.md"
        val DEFAULT_COLUMNS = listOf(Name, Type, Description, Default)
    }

    override fun apply(project: Project) {

        val extension =
            project.extensions.create("springConfMetadataMarkdown", SpringConfMetadataMarkdownExtension::class.java)

        project.tasks.register("renderMetadataTable", RenderMarkdownTableTask::class.java) {

            it.springConfigMetadataJson.from(project.layout.projectDirectory.asFileTree.matching { pattern ->
                pattern.include("**/spring-configuration-metadata.json")
                pattern.exclude("**/build/**/spring-configuration-metadata.json")
            })

            if(extension.columns.isPresent && extension.columns.get().isNotEmpty()){
                it.columns.set(extension.columns)
            } else {
                it.columns.set(DEFAULT_COLUMNS)
            }

            if (extension.readMeTarget.isPresent) {
                it.readMeTarget.set(extension.readMeTarget)
            } else (
                it.readMeTarget.set(project.layout.projectDirectory.file(DEFAULT_README))
            )
        }
    }
}
