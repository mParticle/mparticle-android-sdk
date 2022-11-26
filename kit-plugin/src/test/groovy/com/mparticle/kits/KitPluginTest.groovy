package com.mparticle.kits

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class KitPluginTest {
    @Test
    void greeterPluginAddsGreetingTaskToProject() {
        Project project = ProjectBuilder.builder().build()

        project.pluginManager.apply 'com.mparticle.kit'

        project.mparticle.kitDescription = 'This is a sample kit description.'

    }
}
