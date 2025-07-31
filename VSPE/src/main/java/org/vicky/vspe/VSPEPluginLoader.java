package org.vicky.vspe;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

public class VSPEPluginLoader implements PluginLoader {
    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addRepository(
                new RemoteRepository.Builder("central", "default", "https://repo1.maven.org/maven2/").build()
        );

        resolver.addDependency(new Dependency(new DefaultArtifact("org.hibernate.orm:hibernate-core:6.4.1.Final"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("org.hibernate.orm:hibernate-community-dialects:6.3.1.Final"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("org.jboss.logging:jboss-logging:3.5.3.Final"), null));

        classpathBuilder.addLibrary(resolver);
    }
}
