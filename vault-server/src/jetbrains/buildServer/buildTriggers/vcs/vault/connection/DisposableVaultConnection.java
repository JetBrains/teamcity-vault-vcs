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
class DisposableVaultConnection implements VaultConnection {
  @NotNull
  private final VaultConnection myConnection;

  public DisposableVaultConnection(@NotNull final VaultConnection connection) {
    myConnection = connection;
  }

  @NotNull
  public VaultConnectionParameters getParameters() throws VcsException {
    return myConnection.getParameters();
  }

  public boolean isAlive() throws VcsException {
    return myConnection.isAlive();
  }

  public void resetCaches() throws VcsException {
    myConnection.resetCaches();
  }

  public void login() throws VcsException {
    myConnection.login();
  }

  public void logout() throws VcsException {
    myConnection.logout();
  }

  @NotNull
  public List<RepositoryInfo> getRepositories() throws VcsException {
    return doInLoginLogout(new VcsConnectionCallable<List<RepositoryInfo>>() {
      @NotNull
      public List<RepositoryInfo> call() throws VcsException {
        return myConnection.getRepositories();
      }
    });
  }

  @Nullable
  public File getObject(@NotNull final String path, @NotNull final String version) throws VcsException {
    return doInLoginLogout(new VcsConnectionCallable<File>() {
      @Nullable
      public File call() throws VcsException {
        return myConnection.getObject(path, version);
      }
    });
  }

  @NotNull
  public File getExistingObject(@NotNull final String path, @NotNull final String version) throws VcsException {
    return doInLoginLogout(new VcsConnectionCallable<File>() {
      @NotNull
      public File call() throws VcsException {
        return myConnection.getExistingObject(path, version);
      }
    });
  }

  public boolean objectExists(@NotNull final String path, @Nullable final String version) throws VcsException {
    return doInLoginLogout(new VcsConnectionCallable<Boolean>() {
      @NotNull
      public Boolean call() throws VcsException {
        return myConnection.objectExists(path, version);
      }
    });
  }

  @NotNull
  public String getFolderVersion(@NotNull final String path) throws VcsException {
    return doInLoginLogout(new VcsConnectionCallable<String>() {
      @NotNull
      public String call() throws VcsException {
        return myConnection.getFolderVersion(path);
      }
    });
  }

  @Nullable
  public Long getFolderDisplayVersion(@NotNull final String path, @NotNull final String version) throws VcsException {
    return doInLoginLogout(new VcsConnectionCallable<Long>() {
      @Nullable
      public Long call() throws VcsException {
        return myConnection.getFolderDisplayVersion(path, version);
      }
    });
  }

  public void labelFolder(@NotNull final String path, @NotNull final String version, @NotNull final String label) throws VcsException {
    doInLoginLogout(new VcsConnectionCallable<Object>() {
      @Nullable
      public Object call() throws VcsException {
        myConnection.labelFolder(path, version, label);
        return null;
      }
    });
  }

  @NotNull
  public List<RawChangeInfo> getFolderHistory(@NotNull final String path, @NotNull final String fromVersion, @NotNull final String toVersion) throws VcsException {
    return doInLoginLogout(new VcsConnectionCallable<List<RawChangeInfo>>() {
      @NotNull
      public List<RawChangeInfo> call() throws VcsException {
        return myConnection.getFolderHistory(path, fromVersion, toVersion);
      }
    });
  }

  private<T> T doInLoginLogout(@NotNull VcsConnectionCallable<T> action) throws VcsException {
    try {
      myConnection.login();
      return action.call();
    } finally {
      myConnection.logout();
    }
  }

  private static interface VcsConnectionCallable<T> {
    T call() throws VcsException;
  }
}
