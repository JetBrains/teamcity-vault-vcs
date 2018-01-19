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
import java.util.List;
import jetbrains.buildServer.buildTriggers.vcs.vault.RawChangeInfo;
import jetbrains.buildServer.buildTriggers.vcs.vault.RepositoryInfo;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionParameters;
import jetbrains.buildServer.vcs.VcsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Victory.Bedrosova on 8/19/13.
 */
class SynchronizedVaultConnection implements VaultConnection {
  @NotNull
  private final VaultConnection myConnection;

  public SynchronizedVaultConnection(@NotNull final VaultConnection connection) {
    myConnection = connection;
  }

  @NotNull
  public synchronized VaultConnectionParameters getParameters() throws VcsException {
    return myConnection.getParameters();
  }

  public synchronized boolean isAlive() throws VcsException {
    return myConnection.isAlive();
  }

  public synchronized void resetCaches() throws VcsException {
    myConnection.resetCaches();
  }

  @Nullable
  public synchronized File getObject(@NotNull String path, @NotNull String version) throws VcsException {
    return myConnection.getObject(path, version);
  }

  @NotNull
  public synchronized File getExistingObject(@NotNull String path, @NotNull String version) throws VcsException {
    return myConnection.getExistingObject(path, version);
  }

  public synchronized boolean objectExists(@NotNull String path, @Nullable String version) throws VcsException {
    return myConnection.objectExists(path, version);
  }

  public synchronized void login() throws VcsException {
    myConnection.login();
  }

  public synchronized void logout() throws VcsException {
    myConnection.logout();
  }

  public synchronized void refresh() throws VcsException {
    myConnection.refresh();
  }

  @NotNull
  public synchronized String getFolderVersion(@NotNull String path) throws VcsException {
    return myConnection.getFolderVersion(path);
  }

  @Nullable
  public synchronized Long getFolderDisplayVersion(@NotNull String path, @NotNull String version) throws VcsException {
    return myConnection.getFolderDisplayVersion(path, version);
  }

  public synchronized void labelFolder(@NotNull String path, @NotNull String version, @NotNull String label) throws VcsException {
    myConnection.labelFolder(path, version, label);
  }

  @NotNull
  public synchronized List<RawChangeInfo> getFolderHistory(@NotNull String path, @NotNull String fromVersion, @NotNull String toVersion) throws VcsException {
    return myConnection.getFolderHistory(path, fromVersion, toVersion);
  }

  @NotNull
  public synchronized List<RepositoryInfo> getRepositories() throws VcsException {
    return myConnection.getRepositories();
  }
}
