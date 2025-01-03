package plutoproject.platform.velocity;

import com.github.shynixn.mccoroutine.velocity.SuspendingPluginContainer;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.plugin.PluginClassLoader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import plutoproject.framework.common.util.LoggerKt;
import plutoproject.framework.velocity.dependency.DependenciesImpl;
import plutoproject.framework.velocity.util.PlatformKt;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class PlutoVelocityBootstrap {
    private final PlutoVelocityPlatform platform;

    @Inject
    public PlutoVelocityBootstrap(
            PluginContainer plugin,
            ProxyServer server,
            Logger logger,
            @DataDirectory Path dataDirectoryPath
    ) {
        loadDependencies(getCachePath(dataDirectoryPath));
        PlatformKt.setPlugin(plugin);
        final SuspendingPluginContainer suspendingPlugin = new SuspendingPluginContainer(plugin, server, LoggerFactory.getLogger("PlutoProject/MCCoroutine"));
        suspendingPlugin.initialize(this);
        PlatformKt.setSuspendingPlugin(suspendingPlugin);
        PlatformKt.setServer(server);
        LoggerKt.setLogger(logger);
        plutoproject.framework.common.util.PlatformKt.setServerThread(Thread.currentThread());
        plutoproject.framework.common.util.PlatformKt.initPluginDataFolder(dataDirectoryPath.toFile());
        platform = new PlutoVelocityPlatform();
        platform.load();
    }

    private void loadDependencies(@NotNull Path cachePath) {
        Objects.requireNonNull(cachePath);
        try {
            final Method addPathMethod = PluginClassLoader.class.getDeclaredMethod("addPath", Path.class);
            final ClassLoader classLoader = this.getClass().getClassLoader();
            final Set<Path> dependencies = DependenciesImpl.INSTANCE.resolve(cachePath);
            addPathMethod.setAccessible(true);
            for (Path dependency : dependencies) {
                addPathMethod.invoke(classLoader, dependency);
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private Path getCachePath(@NotNull Path parent) {
        Objects.requireNonNull(parent);
        final Path cacheDirPath = parent.resolve("libraries");
        final File cacheDir = cacheDirPath.toFile();
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        return cacheDirPath;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        platform.enable();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        platform.disable();
    }
}
