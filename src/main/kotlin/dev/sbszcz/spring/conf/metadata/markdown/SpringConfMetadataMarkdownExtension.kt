package dev.sbszcz.spring.conf.metadata.markdown

import org.gradle.api.file.RegularFileProperty

abstract class SpringConfMetadataMarkdownExtension {
    abstract val readMeTarget: RegularFileProperty
}