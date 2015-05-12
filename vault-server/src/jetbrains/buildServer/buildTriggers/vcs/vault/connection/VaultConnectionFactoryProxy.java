/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.util.containers.HashMap;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionFactory;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionParameters;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Victory.Bedrosova on 8/19/13.
 */
public abstract class VaultConnectionFactoryProxy implements VaultConnectionFactory {

  @NotNull
  private final Map<VaultConnectionParameters, VaultConnection> myConnections = new HashMap<VaultConnectionParameters, VaultConnection>();
  private static final ReentrantReadWriteLock CONNECTIONS_LOCK = new ReentrantReadWriteLock();

  @NotNull protected abstract File getVaultConnectionJar();
  @Nullable protected abstract File getVaultApiFolder();

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

      final VaultConnection connection = createConnection(parameters);
      myConnections.put(parameters, connection);
      return connection;

    } finally {
      CONNECTIONS_LOCK.writeLock().unlock();
    }
  }

  @NotNull
  public VaultConnection createConnection(@NotNull final VaultConnectionParameters parameters) {
    final String p = TeamCityProperties.getProperty("teamcity.vcs.vault.classloading", "smart");
    if ("full".equals(p)) {
      return makeSynchronized(makeEternal(makeExceptionAware(new FullClassLoadingVaultConnection(parameters, getJars()))));
    } else if ("smart".equals(p)) {
      return makeSynchronized(makeEternal(makeExceptionAware(new SmartClassLoadingVaultConnection(parameters, getJars()))));
    }
    return makeSynchronized(makeDisposable(makeExceptionAware(new FullClassLoadingVaultConnection(parameters, getJars()))));
  }

  @NotNull
  public static ExceptionAwareConnection makeExceptionAware(@NotNull VaultConnection connection) {
    return new ExceptionAwareConnection(connection);
  }

  @NotNull
  public static SynchronizedVaultConnection makeSynchronized(@NotNull VaultConnection connection) {
    return new SynchronizedVaultConnection(connection);
  }

  @NotNull
  public static EternalVaultConnection makeEternal(@NotNull VaultConnection connection) {
    return new EternalVaultConnection(connection);
  }

  @NotNull
  public static DisposableVaultConnection makeDisposable(@NotNull VaultConnection connection) {
    return new DisposableVaultConnection(connection);
  }

  @NotNull
  private List<File> getJars() {
    final List<File> jars = new ArrayList<File>();

    jars.add(getVaultConnectionJar());

    final File vaultApiFolder = getVaultApiFolder();
    if (vaultApiFolder != null) {
      final File[] files = vaultApiFolder.listFiles();
      if (files != null) {
        jars.addAll(Arrays.asList(files));
      }
    }

    return jars;
  }
}
