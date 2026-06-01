import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    testImplementation(libs.junit)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2025.3.5")
        testFramework(TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here:
        bundledPlugin("org.jetbrains.kotlin")
    }
}

changelog {
    version = project.version.toString()
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "232"       // 2023.2 — minimum supported IDE version
        }

        changeNotes = provider {
            changelog.renderItem(
                changelog.getOrNull(project.version.toString()) ?: changelog.getUnreleased(),
                Changelog.OutputType.HTML
            )
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}
