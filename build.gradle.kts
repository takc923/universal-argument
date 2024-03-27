plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.2"
}

group = "io.github.takc923"
version = "0.8-SNAPSHOT"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.1")
    updateSinceUntilBuild.set(false)
    pluginName.set("universal-argument")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("231")
        pluginDescription.set(
            """
            <p>universal-argument implements universal-argument of emacs</p>
            <p>Type C-u, input number, then input something. It repeats the "something" specified times</p>
            """.trimIndent()
        )
        changeNotes.set(
            """
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
        )
    }
}
