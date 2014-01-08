package jetbrains.buildServer.buildTriggers.vcs.vault;

import jetbrains.buildServer.Used;
import jetbrains.buildServer.plugins.PluginManager;
import jetbrains.buildServer.plugins.bean.PluginInfo;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Created by Victory.Bedrosova on 9/27/13.
 */
public class TeamCityVaultApiConnector extends VaultApiConnector {
  @NotNull
  private final PluginManager myPluginManager;
  @NotNull
  private final PluginDescriptor myPluginDescriptor;

  @Used
  public TeamCityVaultApiConnector(final @NotNull PluginManager pluginManager, final @NotNull PluginDescriptor pluginDescriptor) {
    this(pluginManager, pluginDescriptor, null);
  }

  public TeamCityVaultApiConnector(final @NotNull PluginManager pluginManager, final @NotNull PluginDescriptor pluginDescriptor, final @Nullable Integer maxClassLoaders) {
    super(maxClassLoaders);
    myPluginManager = pluginManager;
    myPluginDescriptor = pluginDescriptor;
  }

  @NotNull
  @Override
  protected File getVaultConnectionJar() {
    return new File(myPluginDescriptor.getPluginRoot(), "standalone/vault-connection.jar");
  }

  @Nullable
  @Override
  protected File getVaultApiFolder() {
    for (PluginInfo pluginInfo : myPluginManager.getDetectedPlugins()) {
      if ("VaultAPI".equals(pluginInfo.getPluginName())) {
        final File pluginRoot = pluginInfo.getPluginRoot();
        final File lib = new File(pluginRoot, "lib");
        return lib.isDirectory() ? lib : pluginRoot;
      }
    }
    return null;
  }
}
