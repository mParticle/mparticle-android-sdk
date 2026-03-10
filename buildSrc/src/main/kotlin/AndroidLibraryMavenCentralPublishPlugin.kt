import com.mparticle.publish.MParticleMavenPublishExtension
import com.mparticle.configureMavenPublishing
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryMavenCentralPublishPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.vanniktech.maven.publish")
            val publishExtension = extensions.create("mparticleMavenPublish", MParticleMavenPublishExtension::class.java)
            configureMavenPublishing(publishExtension)
        }
    }
}
