package plutoproject.platform.velocity;

import com.github.shynixn.mccoroutine.velocity.SuspendingPluginContainer;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.LoggerFactory;
import plutoproject.framework.common.util.LoggerKt;
import plutoproject.framework.common.util.PlatformType;
import plutoproject.framework.velocity.util.EnvironmentKt;

import java.nio.file.Path;
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
        EnvironmentKt.setPlugin(plugin);
        final SuspendingPluginContainer suspendingPlugin = new SuspendingPluginContainer(plugin, server, LoggerFactory.getLogger("PlutoProject/MCCoroutine"));
        suspendingPlugin.initialize(this);
        EnvironmentKt.setSuspendingPlugin(suspendingPlugin);
        EnvironmentKt.setServer(server);
        LoggerKt.setLogger(logger);
        plutoproject.framework.common.util.EnvironmentKt.setPlatformType(PlatformType.VELOCITY);
        plutoproject.framework.common.util.EnvironmentKt.setServerThread(Thread.currentThread());
        plutoproject.framework.common.util.EnvironmentKt.initPluginDataFolder(dataDirectoryPath.toFile());
        platform = new PlutoVelocityPlatform(plugin, server, logger, dataDirectoryPath);
        platform.load();
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
