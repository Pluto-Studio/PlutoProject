package plutoproject.platform.paper;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import plutoproject.framework.paper.dependency.DependenciesImpl;
import xyz.jpenilla.gremlin.runtime.platformsupport.PaperClasspathAppender;

import java.io.File;
import java.nio.file.Path;

@SuppressWarnings("unused")
public class PlutoPaperLoader implements PluginLoader {
    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        Path cacheDirPath = classpathBuilder.getContext().getDataDirectory().resolve("libraries");
        File cacheDir = cacheDirPath.toFile();
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        new PaperClasspathAppender(classpathBuilder).append(DependenciesImpl.INSTANCE.resolve(cacheDirPath));
    }
}
