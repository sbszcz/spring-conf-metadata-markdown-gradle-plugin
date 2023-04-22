package dev.sbszcz.gradle.plugin

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import dev.sbszcz.spring.conf.metadata.markdown.Column
import dev.sbszcz.spring.conf.metadata.markdown.RenderMarkdownTableTask
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test


class SpringConfMetadataMarkdownGradlePluginTest {
    @Test
    fun `plugin registers task renderMetadataTable`() {

        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.sbszcz.spring-conf-metadata-to-markdown")

        val task = project.tasks.named("renderMetadataTable", RenderMarkdownTableTask::class.java).get()
        assertThat(task).isNotNull()
        assertThat(task.readMeTarget.isPresent).isTrue()
        assertThat(task.readMeTarget.get().asFile.name).isEqualTo("README.md")

        assertThat(task.columns.isPresent).isTrue()
        assertThat(task.columns.get()).containsExactly(
            Column.Name,
            Column.Type,
            Column.Description,
            Column.Default
        )
    }

}
