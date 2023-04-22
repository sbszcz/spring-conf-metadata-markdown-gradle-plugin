# Spring Configuration Metadata Markdown Generator

[![Build and run all tests](https://github.com/sbszcz/spring-conf-metadata-markdown-gradle-plugin/actions/workflows/test-gradle-plugin.yaml/badge.svg)](https://github.com/sbszcz/spring-conf-metadata-markdown-gradle-plugin/actions/workflows/test-gradle-plugin.yaml)

A gradle plugin that converts `spring-configuration-metadata.json` files (see [Spring Configuration Metadata](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html)) into markdown tables 
and writes them into your README.md (or other specified markdown files)

# Usage

The plugin provides only one task `renderMetadataTable`. 
By executing the task the plugin will search for all `spring-configuration-metadata.json` files except the `build` folder.

All `spring-configuration-metadata.json` files found are parsed and converted into a markdown table that is then written to the 
README.md (default) located in the projects folder. The README.md file must at least contain the following marker tags to indicate
where the table should be rendered. (Hint: Each tag must be on its own line)

Example README.md
```markdown

# Headline

<!-- springconfmetadata -->
<!-- /springconfmetadata -->

```

The target markdown file can be customized like this:

Example build.gradle.kts
```kotlin
...
springConfMetadataMarkdown {
    readMeTarget.set(project.file("documentation.md"))
}
...
```




