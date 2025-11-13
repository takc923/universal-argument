import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.intellij.platform") version "2.10.4"
}

group = "io.github.takc923"
version = "0.10-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.2.1")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "io.github.takc923.universal-argument"
        name = "universal-argument"
        version = project.version.toString()
        vendor {
            name = "takc923"
            url = "https://github.com/takc923/universal-argument"
        }
        ideaVersion {
            sinceBuild = "252"
        }
        description = """
            <p>universal-argument implements universal-argument of emacs</p>
            <p>Type C-u, input number, then input something. It repeats the "something" specified times</p>
        """.trimIndent()
        changeNotes = """
            <p>v0.8</p>
            <ul>
              <li>Support only 193.1784+</li>
            </ul>
            <p>v0.7</p>
            <ul>
              <li>Refactoring</li>
              <li>Update kotlin</li>
            </ul>
            <p>v0.6</p>
            <ul>
              <li>Fix a bug that occurred when used with some plugins</li>
            </ul>
            <p>v0.5</p>
            <ul>
              <li>Prevent typed char from appearing for a moment</li>
            </ul>
            <p>v0.4</p>
            <ul>
              <li>Support more editor action</li>
              <li>Fix bug that prevent to repeat Enter</li>
            </ul>
            <p>v0.3</p>
            <ul>
              <li>Show dialog if repeat count is too large</li>
            </ul>
            <p>v0.2</p>
            <ul>
              <li>Escape to cancel</li>
              <li>C-u -> num1 -> C-u -> num2 to repeat num2 num1 times</li>
            </ul>
        """.trimIndent()
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}
