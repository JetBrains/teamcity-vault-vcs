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

package jetbrains.buildServer.buildTriggers.vcs.vault.impl;

import VaultClientIntegrationLib.*;
import VaultClientOperationsLib.SetFileTimeType;
import VaultLib.*;
import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import jetbrains.buildServer.buildTriggers.vcs.vault.*;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.vcs.VcsException;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Victory.Bedrosova on 8/19/13.
 */
public class VaultConnectionImpl implements VaultConnection {
  private static final Logger LOG = Logger.getLogger(VaultConnection.class);

  @NotNull
  private final VaultConnectionParameters myParameters;

  public VaultConnectionImpl(@NotNull final VaultConnectionParameters parameters) {
    myParameters = parameters;

    resetCaches();
  }

  @NotNull
  public VaultConnectionParameters getParameters() {
    return myParameters;
  }

  public boolean isAlive() {
    return ServerOperations.isConnected();
  }

  public void resetCaches() {
    FileUtil.delete(myParameters.getConnectionCacheFolder());
  }

  @NotNull
  public File getExistingObject(@NotNull String path, @NotNull String version) throws VcsException {
    final File object = getObject(path, version);
    if (object == null) throw new VcsException("No object " + path + " found at revision " + version);
    return object;
  }

  @Nullable
  public File getObject(@NotNull String path, @NotNull String version) throws VcsException {
    final File cached = getCachedFile(path, version);

    if (cached.isFile()) {
      return cached;
    } else {

      FileUtil.delete(cached);

      if (isExistingFile(path)) {

        final Long fileVersion = getFileDisplayVersion(path, version);

        if (fileVersion == null) {
          return getObjectFromParent(cached.getName(), getObject(getRepoParentPath(path), version));
        } else {
          getObject(path, fileVersion, false, cached);
          return cached.exists() ? cached : null;
        }
      } else if (isExistingFolder(path)) {

        final Long folderVersion = getFolderDisplayVersion(path, version);

        if (folderVersion == null) {
          return getObjectFromParent(cached.getName(), getObject(getRepoParentPath(path), version));
        } else {
          getObject(path, folderVersion, true, cached);
          return cached.exists() ? cached : null;
        }
      } else {
        return getObjectFromParent(cached.getName(), getObject(getRepoParentPath(path), version));
      }
    }
  }

  private void getObject(@NotNull String path, long objectVersion, boolean isFolder, @NotNull File dest) {
    FileUtil.createParentDirs(dest);

    final GetOptions getOptions = new GetOptions();
    getOptions.Recursive = isFolder;
    getOptions.SetFileTime = SetFileTimeType.Modification;

    final File destPath = isFolder || isRoot(path) ? dest : dest.getParentFile();

    GetOperations.ProcessCommandGetVersionToLocationOutsideWorkingFolder(
      ensureRepoPath(path), (int)objectVersion, getOptions, destPath.getAbsolutePath()
    );
  }

  @Nullable
  private File getObjectFromParent(@NotNull final String name, @Nullable File parent) {
    if (parent == null) return null;
    final File[] files = parent.listFiles(new FilenameFilter() {
      public boolean accept(final File d, final String n) {
        return name.equals(n);
      }
    });
    return files == null ||  files.length == 0 ? null : files[0];
  }

  private boolean isRoot(@NotNull String path) {
    return VaultUtil.ROOT.equals(ensureRepoPath(path));
  }

  @NotNull
  private File getCachedFile(@NotNull String path, @NotNull String version) {
    return new File(myParameters.getConnectionCacheFolder(), version + "/" + shortenParentPathToHash(ensureFileSystemPath(path)));
  }

  @NotNull
  private String shortenParentPathToHash(@NotNull String path) {
    final File file = new File(path);
    final String parent = file.getParent();
    return (StringUtil.isEmptyOrSpaces(parent) ? StringUtil.EMPTY : String.valueOf(parent.hashCode()) + "/") + file.getName();
  }

  private boolean objectExists(@NotNull String path) {
    return RepositoryUtil.PathExists(ensureRepoPath(path));
  }

  private boolean isExistingFile(@NotNull String path) {
    if (objectExists(path)) {
      try {
        RepositoryUtil.FindVaultFileAtReposOrLocalPath(ensureRepoPath(path));
        return true;
      } catch (Throwable th) {
        // continue
      }
    }
    return false;
  }

  private boolean isExistingFolder(@NotNull String path) {
    if (objectExists(path)) {
      try {
        RepositoryUtil.FindVaultFolderAtReposOrLocalPath(ensureRepoPath(path));
        return true;
      } catch (Throwable th) {
        // continue
      }
    }
    return false;
  }

  @Nullable
  private Long getFileDisplayVersion(@NotNull String path, @NotNull String version) {
    final long txId = Long.parseLong(version);

    final VaultHistoryItem[] historyItems =
      ServerOperations.ProcessCommandHistory(ensureRepoPath(path), true, DateSortOption.desc, null, null, null, null, null, null, -1, -1, 1000);

    for (final VaultHistoryItem i : historyItems) {
      if (i.get_TxID() > 0 &&  i.get_TxID() <= txId) {
        return i.get_Version();
      }
    }
    return null;
  }

  @Nullable
  public Long getFolderDisplayVersion(@NotNull String path, @NotNull String version) {
    final long txId = Long.parseLong(version);

    final VaultTxHistoryItem[] txHistoryItems =
      ServerOperations.ProcessCommandVersionHistory(ensureRepoPath(path), 0, VaultDate.EmptyDate(), VaultDate.EmptyDate(), 1000);

    for (final VaultTxHistoryItem i : txHistoryItems) {
      if (i.get_TxID() > 0 &&  i.get_TxID() <= txId) {
        return i.get_Version();
      }
    }
    return null;
  }

  public boolean objectExists(@NotNull String path, @Nullable String version) throws VcsException {
    if (StringUtil.isEmpty(version)) {
      return objectExists(path);
    }

    @SuppressWarnings("ConstantConditions")
    final File object = getObject(path, version);
    return object != null && object.exists();
  }

  @NotNull
  private String ensureRepoPath(@NotNull String path) {
    return VaultUtil.getRepoPathFromPath(path);
  }

  @NotNull
  private String ensureFileSystemPath(@NotNull String path) {
    return VaultUtil.getPathFromRepoPath(path);
  }

  @NotNull
  private String getRepoParentPath(@NotNull String path) {
    return VaultUtil.getRepoParentPath(ensureRepoPath(path));
  }

  @NotNull
  public String getFolderVersion(@NotNull String path) {
    final VaultTxHistoryItem[] txHistoryItems = ServerOperations.ProcessCommandVersionHistory(ensureRepoPath(path), 0, VaultDate.EmptyDate(), VaultDate.EmptyDate(), 1);
    return String.valueOf(txHistoryItems[0].get_TxID());
  }

  public void login() throws VcsException {
    login(10);
  }

  private void login(int attempts) throws VcsException {
    for (int i = 1; i <= attempts; ++i) {
      try {

        ServerOperations.client.LoginOptions.URL = myParameters.getURL();
        ServerOperations.client.LoginOptions.Repository = myParameters.getRepository();
        ServerOperations.client.LoginOptions.User = myParameters.getUser();
        ServerOperations.client.LoginOptions.Password = myParameters.getPassword();

        ServerOperations.Login();

        return;

      } catch (NoClassDefFoundError e) {
        throw new VcsException(VaultUtil.NO_API_FOUND_EXCEPTION, e);

      } catch (Throwable th) {
        logout();
        if (i == attempts) {
          throw new VcsException(specifyMessage(th.getMessage()), th);
        }
      }
    }
  }

  public void logout() {
    try {
      if (ServerOperations.isConnected()) {
        ServerOperations.Logout();
      }
    } catch (Throwable th) {
      LOG.error("Exception occurred when disconnecting from Vault server", th);
    } finally {
      resetCaches();
    }
  }

  public void refresh() throws VcsException {
    VaultClientIntegrationLib.RepositoryUtil.Refresh();
  }

  @NotNull
  private String specifyMessage(@Nullable String message) {
    return String.format("%s: Exception occurred while trying to connect to Vault server. See original message below:\n%s",
                         myParameters.getStringRepresentation(), message);
  }

  public void labelFolder(@NotNull String path, @NotNull String version, @NotNull String label) throws VcsException {
    final Long folderVersion = getFolderDisplayVersion(path, version);

    if (folderVersion == null) return;

    deleteLabel(path, label);
    ServerOperations.ProcessCommandLabel(ensureRepoPath(path), label, folderVersion);
  }

  private void deleteLabel(@NotNull String path, @NotNull String label) throws VcsException {
    try {
      ServerOperations.ProcessCommandDeleteLabel(ensureRepoPath(path), label);
    } catch (Exception e) {
      if ((e.getMessage() != null) && e.getMessage().contains("Could not find label")) {
        return;
      }
      throw new VcsException(e);
    }
  }

  @NotNull
  public List<RawChangeInfo> getFolderHistory(@NotNull String path, @NotNull String fromVersion, @NotNull String toVersion) {
    final Long objectFromVersion = getFolderDisplayVersion(path, fromVersion);
    final Long objectToVersion = getFolderDisplayVersion(path, toVersion);

    if (objectFromVersion == null || objectToVersion == null || objectFromVersion >= objectToVersion) return Collections.emptyList();

    final VaultHistoryItem[] vaultHistoryItems =
      ServerOperations.ProcessCommandHistory(ensureRepoPath(path), true, DateSortOption.desc,
        null, null/*"label,obliterate,pin,propertychange"*/,
        null, null, null, null,
        objectFromVersion + 1, objectToVersion, 1000);

    return CollectionsUtil.convertAndFilterNulls(Arrays.asList(vaultHistoryItems), new Converter<RawChangeInfo, VaultHistoryItem>() {
      public RawChangeInfo createFrom(@NotNull VaultHistoryItem source) {

        final RawChangeInfo.RawChangeInfoType type = RawChangeInfo.RawChangeInfoType.getType(VaultHistoryType.GetHistoryTypeName(source.get_HistItemType()));

        if (type == RawChangeInfo.RawChangeInfoType.NOT_CHANGED) return null;

        final String name = source.get_Name();
        final String miscInfo1 = source.get_MiscInfo1();
        final String miscInfo2 = source.get_MiscInfo2();
        final Long txId = source.get_TxID();
        final VaultDateTime txDate = source.get_TxDate();
        final Date date = new GregorianCalendar(txDate.get_Year(), txDate.get_Month() - 1, txDate.get_Day(), txDate.get_Hour(), txDate.get_Minute(), txDate.get_Second()).getTime();
        final String user = source.get_UserLogin();
        final String actionString = source.GetActionString();
        final String comment = source.get_Comment();

        return new RawChangeInfo(name, miscInfo1, miscInfo2, String.valueOf(txId), date, user, actionString, comment, type);
      }
    });
  }

  @NotNull
  public List<RepositoryInfo> getRepositories() {
    final VaultRepositoryInfo[] repos = ServerOperations.ProcessCommandListRepositories();
    if (repos.length == 0) return Collections.emptyList();

    final List<RepositoryInfo> res = new ArrayList<RepositoryInfo>(repos.length);
    for (VaultLib.VaultRepositoryInfo info : repos) {
      res.add(new RepositoryInfo(info.get_RepID(), info.get_RepName()));
    }
    return res;
  }
}
