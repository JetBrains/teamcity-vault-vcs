/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.buildTriggers.vcs.vault.connection;

import java.io.File;
import jetbrains.buildServer.plugins.PluginManager;
import jetbrains.buildServer.plugins.bean.PluginInfo;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @User Victory.Bedrosova
 * 2/20/14.
 */
public class TeamCityVaultConnectionProxy extends VaultConnectionFactoryProxy {
  @NotNull
  private final PluginManager myPluginManager;
  @NotNull
  private final PluginDescriptor myPluginDescriptor;

  public TeamCityVaultConnectionProxy(@NotNull final PluginManager pluginManager, @NotNull final PluginDescriptor pluginDescriptor) {
    myPluginManager = pluginManager;
    myPluginDescriptor = pluginDescriptor;
  }

  @Override
  @NotNull
  protected File getVaultConnectionJar() {
    return new File(myPluginDescriptor.getPluginRoot(), "standalone/vault-connection.jar");
  }

  @Override
  @Nullable
  protected File getVaultApiFolder() {
    return getVaultApiFolder(myPluginManager);
  }

  @Nullable
  public static File getVaultApiFolder(@NotNull PluginManager pluginManager) {
    for (PluginInfo pluginInfo : pluginManager.getDetectedPlugins()) {
      if ("VaultAPI".equals(pluginInfo.getPluginName())) {
        final File pluginRoot = pluginInfo.getPluginRoot();
        final File lib = new File(pluginRoot, "lib");
        return lib.isDirectory() ? lib : pluginRoot;
      }
    }
    return null;
  }

}
