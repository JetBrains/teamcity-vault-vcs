/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionFactory;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionParameters;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultUtil;
import jetbrains.buildServer.vcs.VcsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @User Victory.Bedrosova
 * 2/18/14.
 */
class ClassLoadingVaultConnection extends DelegatingVaultConnection {
  @Nullable private VaultConnection myConnection;
  @NotNull private final VaultConnectionParameters myParameters;
  @NotNull private final ClassLoader myClassLoader;

  public ClassLoadingVaultConnection(@NotNull final VaultConnectionParameters parameters, @NotNull final ClassLoader classLoader) {
    myParameters = parameters;
    myClassLoader = classLoader;
  }

  @NotNull
  @Override
  protected VaultConnection getConnection() throws VcsException {
    if (myConnection == null) {
      try {

        myClassLoader.loadClass("VaultClientIntegrationLib.ServerOperations"); // check if Vault API is present

        myConnection = ((VaultConnectionFactory)myClassLoader.loadClass("jetbrains.buildServer.buildTriggers.vcs.vault.impl.VaultConnectionFactoryImpl").newInstance()).getOrCreateConnection(myParameters);

      } catch (ClassNotFoundException e) {
        throw new VcsException(VaultUtil.NO_API_FOUND_EXCEPTION);
      } catch (NoClassDefFoundError e) {
        throw new VcsException(VaultUtil.NO_API_FOUND_EXCEPTION);

      } catch (InstantiationException e) {
        throw new VcsException(e);
      } catch (IllegalAccessException e) {
        throw new VcsException(e);
      }
    }
    return myConnection;
  }
}
