// buildSrc/src/main/kotlin/BundleHelpers.kt
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency

/**
 * Project extension function available in build scripts:
 *   implementationBundled("group:artifact:version", transitive = true)
 *
 * Adds the dependency to implementation AND a project-local "bundled" configuration.
 * If "bundled" doesn't exist, it will be created.
 */
fun Project.implementationBundled(notation: Any, transitive: Boolean = true) {
    val dep = dependencies.create(notation)
    if (dep is ModuleDependency) {
        dep.isTransitive = transitive
    }
    // ensure the configuration exists
    val bundled = configurations.findByName("bundled") ?: configurations.create("bundled").apply {
        isCanBeResolved = true
        isCanBeConsumed = false
    }

    // add to implementation and bundled
    dependencies.add("implementation", dep)
    dependencies.add(bundled.name, dep)
}
