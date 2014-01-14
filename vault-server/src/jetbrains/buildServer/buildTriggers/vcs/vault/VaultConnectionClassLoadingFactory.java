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

package jetbrains.buildServer.buildTriggers.vcs.vault;

import com.intellij.util.containers.HashMap;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by Victory.Bedrosova on 8/19/13.
 */
public class VaultConnectionClassLoadingFactory implements VaultConnectionFactory {
  private static final Logger LOG = Logger.getLogger(VaultConnectionClassLoadingFactory.class);

  @NotNull
  private final VaultApiConnector myVaultApiConnector;
  @NotNull
  private final Map<VaultConnectionParameters, VaultConnection> myConnections = new HashMap<VaultConnectionParameters, VaultConnection>();

  private static final ReentrantReadWriteLock CONNECTIONS_LOCK = new ReentrantReadWriteLock();

  public VaultConnectionClassLoadingFactory(@NotNull VaultApiConnector vaultApiConnector) {
    myVaultApiConnector = vaultApiConnector;
  }

  @NotNull
  public VaultConnection getOrCreateConnection(@NotNull final VaultConnectionParameters parameters) {
    CONNECTIONS_LOCK.readLock().lock();
    try {
      if (myConnections.containsKey(parameters)) {
        return myConnections.get(parameters);
      }
    } finally {
      CONNECTIONS_LOCK.readLock().unlock();
    }

    CONNECTIONS_LOCK.writeLock().lock();
    try {
      if (myConnections.containsKey(parameters)) {
        return myConnections.get(parameters);
      }

      final VaultConnection connection1 = createConnection(parameters);
      myConnections.put(parameters, connection1);
      return connection1;

    } finally {
      CONNECTIONS_LOCK.writeLock().unlock();
    }
  }

  @NotNull
  private VaultConnection createConnection(@NotNull final VaultConnectionParameters parameters) {
    return getFactory(parameters).getOrCreateConnection(parameters);
  }

  @NotNull
  private VaultConnectionFactory getFactory(@NotNull final VaultConnectionParameters parameters) {
    try {
      return (VaultConnectionFactory) myVaultApiConnector.getVaultApiClassLoader(parameters).loadClass("jetbrains.buildServer.buildTriggers.vcs.vault.impl.VaultConnectionFactoryImpl").newInstance();
    } catch (InstantiationException e) {
      LOG.warn(e.getMessage(), e);
    } catch (IllegalAccessException e) {
      LOG.warn(e.getMessage(), e);
    } catch (ClassNotFoundException e) {
      LOG.warn(e.getMessage(), e);
    }
    // not expected
    throw new IllegalStateException();
  }
}
