package dev.sbszcz.spring.conf.metadata.markdown

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty

abstract class SpringConfMetadataMarkdownExtension {
    abstract val readMeTarget: RegularFileProperty
    abstract val columns: ListProperty<Column>
}