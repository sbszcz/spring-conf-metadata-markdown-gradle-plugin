package dev.sbszcz.gradle.plugin

import assertk.assertThat
import assertk.assertions.isEqualTo
import dev.sbszcz.spring.conf.metadata.markdown.ReadmeWriter
import org.junit.jupiter.api.Test
import kotlin.io.path.toPath

class ReadmeMarkerTest {

    companion object{

        val GIVEN_README = """
            # Example Project
            
            <!-- springconfmetadata -->
            nonsense
            <!-- /springconfmetadata -->
            
            paragraph 1
            
            <!--springconfmetadata-->
            <!--/springconfmetadata-->
            
            paragraph 2
            
            <!--springconfmetadata--><!--/springconfmetadata-->
        """.trimIndent()

        val EXPECTED_README = """
            # Example Project

            <!-- springconfmetadata -->
            | Tables   |      Are      |  Cool |
            |----------|:-------------:|------:|
            | col 1 is |  left-aligned | €1600 |            
            <!-- /springconfmetadata -->
            
            paragraph 1
            
            <!--springconfmetadata-->
            | Tables   |      Are      |  Cool |
            |----------|:-------------:|------:|
            | col 1 is |  left-aligned | €1600 |            
            <!--/springconfmetadata-->
            
            paragraph 2
            
            <!--springconfmetadata--><!--/springconfmetadata-->
            
        """.trimIndent()
    }

    @Test
    fun `should fill 'springconfmetadata' marker in readme`() {

        val readmeFileUrl = this::class.java.getResource("/readme.md")
        checkNotNull(readmeFileUrl)
        val readmeFile = readmeFileUrl.toURI().toPath().toFile()

        assertThat(readmeFile.readText()).isEqualTo(GIVEN_README)

        val table = """
            | Tables   |      Are      |  Cool |
            |----------|:-------------:|------:|
            | col 1 is |  left-aligned | €1600 |            
        """.trimIndent()

        ReadmeWriter.writeTextInsideMarker(table, readmeFile)

        assertThat(readmeFile.readText()).isEqualTo(EXPECTED_README)
    }
}