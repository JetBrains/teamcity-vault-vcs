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
import java.util.Arrays;
import java.util.List;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionParameters;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.filters.Filter;
import org.jetbrains.annotations.NotNull;

/**
 * @User Victory.Bedrosova
 * 2/19/14.
 */
class SmartClassLoadingVaultConnection extends ClassLoadingVaultConnection {
  public SmartClassLoadingVaultConnection(@NotNull final VaultConnectionParameters parameters, @NotNull final List<File> jars) {
    super(parameters, getClassLoader(jars));
  }

  @NotNull
  private static ClassLoader getClassLoader(@NotNull final List<File> jars) {
    return new VaultApiJarClassLoader(getParentClassLoader(jars), CollectionsUtil.filterCollection(jars, new Filter<File>() {
      public boolean accept(@NotNull final File data) {
        return !isCommonLib(data);
      }
    }));
  }

  @NotNull
  private static ClassLoader getParentClassLoader(@NotNull final List<File> jars) {
    return new VaultApiJarClassLoader(SmartClassLoadingVaultConnection.class.getClassLoader(), CollectionsUtil.filterCollection(jars, new Filter<File>() {
      public boolean accept(@NotNull final File data) {
        return isCommonLib(data);
      }
    }));
  }

  private static final List<String> COMMON_VAULT_LIBS =
    Arrays.asList("Accessibility.jar",
                  "J2EE.Helpers.jar",
                  "J2SE.Helpers.jar",
                  "Microsoft.VisualBasic.jar",
                  "mscorlib.jar",
                  "Novell.Directory.Ldap.jar",
                  "res.xml",
                  "System.jar",
                  "System.Configuration.jar",
                  "System.Data.jar",
                  "System.Data.OracleClient.jar",
                  "System.Deployment.jar",
                  "System.Design.jar",
                  "System.DirectoryServices.jar",
                  "System.Drawing.jar",
                  "System.EnterpriseServices.jar",
                  "System.Runtime.Remoting.jar",
                  "System.Runtime.Serialization.Formatters.Soap.jar",
                  "System.Web.jar",
                  "System.Web.Extensions.jar",
                  "System.Web.Extensions.Design.jar",
                  "System.Web.Mobile.jar",
                  "System.Web.Services.jar",
                  "System.Windows.Forms.jar",
                  "System.Xml.jar");

  private static boolean isCommonLib(@NotNull final File jar) {
    return COMMON_VAULT_LIBS.contains(jar.getName());
  }
}
