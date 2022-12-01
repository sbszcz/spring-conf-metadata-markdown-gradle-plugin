# spring-conf-metadata-markdown-gradle-plugin

A gradle plugin that converts `spring-configuration-metadata.json` (see [Spring Configuration Metadata](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html)) files into markdown tables 
and writes them into a markdown files (e.g. README.md)

# Usage

The plugin provides only one task `renderMetadataTable`. 
By executing the task the plugin will search for all `spring-configuration-metadata.json` files except the `build` folder.

All `spring-configuration-metadata.json` files found are parsed and converted into a markdown table that is then written to the 
README.md (file) located in the projects folder. The README.md file must at least contain the following marker tags to indicate
where the table should be located.

Example README.md
```markdown
...
<!-- springconfmetadata -->
<!-- /springconfmetadata -->
...
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




