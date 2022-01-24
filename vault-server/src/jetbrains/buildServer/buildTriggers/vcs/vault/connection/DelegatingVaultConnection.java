/*
 * Copyright 2000-2022 JetBrains s.r.o.
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
 * @User Victory.Bedrosova
 * 2/18/14.
 */
abstract class DelegatingVaultConnection implements VaultConnection {
  
  @NotNull
  public VaultConnectionParameters getParameters() throws VcsException {
    return getConnection().getParameters();
  }

  public boolean isAlive() throws VcsException {
    return getConnection().isAlive();
  }

  public void resetCaches() throws VcsException {
    getConnection().resetCaches();
  }

  public void login() throws VcsException {
    getConnection().login();
  }

  public void logout() throws VcsException {
    getConnection().logout();
  }

  public void refresh() throws VcsException {
    getConnection().refresh();
  }

  @NotNull
  public List<RepositoryInfo> getRepositories() throws VcsException {
    return getConnection().getRepositories();
  }

  @Nullable
  public File getObject(@NotNull final String path, @NotNull final String version) throws VcsException {
    return getConnection().getObject(path, version);
  }

  @NotNull
  public File getExistingObject(@NotNull final String path, @NotNull final String version) throws VcsException {
    return getConnection().getExistingObject(path, version);
  }

  public boolean objectExists(@NotNull final String path, @Nullable final String version) throws VcsException {
    return getConnection().objectExists(path, version);
  }

  @NotNull
  public String getFolderVersion(@NotNull final String path) throws VcsException {
    return getConnection().getFolderVersion(path);
  }

  @Nullable
  public Long getFolderDisplayVersion(@NotNull final String path, @NotNull final String version) throws VcsException {
    return getConnection().getFolderDisplayVersion(path, version);
  }

  public void labelFolder(@NotNull final String path, @NotNull final String version, @NotNull final String label) throws VcsException {
    getConnection().labelFolder(path, version, label);
  }

  @NotNull
  public List<RawChangeInfo> getFolderHistory(@NotNull final String path, @NotNull final String fromVersion, @NotNull final String toVersion) throws VcsException {
    return getConnection().getFolderHistory(path, fromVersion, toVersion);
  }
  
  @NotNull protected abstract VaultConnection getConnection() throws VcsException;
}
