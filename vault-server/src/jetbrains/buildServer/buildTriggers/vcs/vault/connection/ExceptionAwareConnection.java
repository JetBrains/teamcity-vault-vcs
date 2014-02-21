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

import jetbrains.buildServer.buildTriggers.vcs.vault.RawChangeInfo;
import jetbrains.buildServer.buildTriggers.vcs.vault.RepositoryInfo;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionParameters;
import jetbrains.buildServer.vcs.VcsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * Created by Victory.Bedrosova on 8/23/13.
 */
class ExceptionAwareConnection implements VaultConnection {
  @NotNull
  private final VaultConnection myConnection;

  public ExceptionAwareConnection(@NotNull VaultConnection connection) {
    myConnection = connection;
  }

  @NotNull
  private VcsException toVcsException(@NotNull Throwable t) throws VcsException {
    throw (t instanceof VcsException) ? (VcsException)t : new VcsException(specifyMessage(t.getMessage()), t);
  }

  @NotNull
  private String specifyMessage(@Nullable String message) throws VcsException {
    return String.format("%s: Exception occurred while trying to connect to Vault server. See original message below:\n%s",
      getParameters().getStringRepresentation(), message);
  }

  @NotNull
  public VaultConnectionParameters getParameters() throws VcsException {
    return myConnection.getParameters();
  }

  public boolean isAlive() throws VcsException {
    try {
      return myConnection.isAlive();
    } catch (Throwable t) {
      throw toVcsException(t);
    }
  }

  public void resetCaches() throws VcsException {
    myConnection.resetCaches();
  }

  @Nullable
  public File getObject(@NotNull String path, @NotNull String version) throws VcsException {
    try {
      return myConnection.getObject(path, version);
    } catch (Throwable t) {
      throw toVcsException(t);
    }
  }

  @NotNull
  public File getExistingObject(@NotNull String path, @NotNull String version) throws VcsException {
    try {
      return myConnection.getExistingObject(path, version);
    } catch (Throwable t) {
      throw toVcsException(t);
    }
  }

  public boolean objectExists(@NotNull String path, @Nullable String version) throws VcsException {
    try {
      return myConnection.objectExists(path, version);
    } catch (Throwable t) {
      throw toVcsException(t);
    }
  }

  @NotNull
  public String getFolderVersion(@NotNull String path) throws VcsException {
    try {
      return myConnection.getFolderVersion(path);
    } catch (Throwable t) {
      throw toVcsException(t);
    }
  }

  @Nullable
  public Long getFolderDisplayVersion(@NotNull String path, @NotNull String version) throws VcsException {
    try {
      return myConnection.getFolderDisplayVersion(path, version);
    } catch (Throwable t) {
      throw toVcsException(t);
    }
  }

  public void login() throws VcsException {
    try {
      myConnection.login();
    } catch (Throwable t) {
      throw toVcsException(t);
    }
  }

  public void logout() throws VcsException {
    try {
      myConnection.logout();
    } catch (Throwable t) {
      throw toVcsException(t);
    }
  }

  public void labelFolder(@NotNull String path, @NotNull String version, @NotNull String label) throws VcsException {
    try {
      myConnection.labelFolder(path, version, label);
    } catch (Throwable t) {
      throw toVcsException(t);
    }
  }

  @NotNull
  public List<RawChangeInfo> getFolderHistory(@NotNull String path, @NotNull String fromVersion, @NotNull String toVersion) throws VcsException {
    try {
      return myConnection.getFolderHistory(path, fromVersion, toVersion);
    } catch (Throwable t) {
      throw toVcsException(t);
    }
  }

  @NotNull
  public List<RepositoryInfo> getRepositories() throws VcsException {
    try {
      return myConnection.getRepositories();
    } catch (Throwable t) {
      throw toVcsException(t);
    }
  }
}
