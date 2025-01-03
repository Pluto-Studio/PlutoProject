package plutoproject.framework.common.dependency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.jpenilla.gremlin.runtime.DependencyCache;
import xyz.jpenilla.gremlin.runtime.DependencyResolver;
import xyz.jpenilla.gremlin.runtime.DependencySet;

import java.nio.file.Path;
import java.util.Set;

public interface Dependencies {
    Logger LOGGER = LoggerFactory.getLogger("framework/common/dependency");

    String getDependenciesFileName();

    default Set<Path> resolve(Path cacheDir) {
        DependencySet deps = DependencySet.readFromClasspathResource(
                Dependencies.class.getClassLoader(), getDependenciesFileName()
        );
        DependencyCache cache = new DependencyCache(cacheDir);
        Set<Path> files;
        try (DependencyResolver downloader = new DependencyResolver(LOGGER)) {
            files = downloader.resolve(deps, cache).jarFiles();
        }
        cache.cleanup();
        return files;
    }
}
