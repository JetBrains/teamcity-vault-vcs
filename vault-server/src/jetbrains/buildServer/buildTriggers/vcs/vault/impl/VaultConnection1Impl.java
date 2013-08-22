package jetbrains.buildServer.buildTriggers.vcs.vault.impl;

import VaultClientIntegrationLib.*;
import VaultClientOperationsLib.SetFileTimeType;
import VaultLib.VaultDate;
import VaultLib.VaultHistoryItem;
import VaultLib.VaultTxHistoryItem;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection1;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionParameters;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultUtil;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.vcs.VcsException;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

import static jetbrains.buildServer.buildTriggers.vcs.vault.VaultUtil.ROOT;

/**
 * Created by Victory.Bedrosova on 8/19/13.
 */
class VaultConnection1Impl implements VaultConnection1 {
  private static final Logger LOG = Logger.getLogger(VaultConnection1.class);

  @NotNull
  private final VaultConnectionParameters myParameters;
  @NotNull
  private final File myCacheFolder;

  public VaultConnection1Impl(@NotNull final VaultConnectionParameters parameters, @NotNull final  File cacheFolder) {
    myParameters = parameters;
    myCacheFolder = cacheFolder;

    resetCaches();
  }

  @NotNull
  public VaultConnectionParameters getParameters() {
    return myParameters;
  }

  public boolean isAlive() throws VcsException {
    return ServerOperations.isConnected();
  }

  public void resetCaches() {
    FileUtil.delete(myCacheFolder);
  }

  @Nullable
  public File getObject(@NotNull String path, @NotNull String version) throws VcsException {
    final File cached = getCachedFile(path, version);

    if (isFile(path)) {
      if (cached.exists()) return cached;

      final Long fileVersion = getFileVersion(path, version);
      if (fileVersion == null) return null;

      getObject(path, fileVersion, false, cached);

    } else if (isFolder(path)) {
      FileUtil.delete(cached);

      final Long folderVersion = getFolderVersion(path, version);
      if (folderVersion == null) return null;

      getObject(path, folderVersion, true, cached);

    } else {
      getObject(getRepoParentPath(path), version);
    }

    return cached.exists() ? cached : null;
  }

  private void getObject(@NotNull String path, long objectVersion, boolean recursive, @NotNull File dest) {
    FileUtil.createParentDirs(dest);

    final GetOptions getOptions = new GetOptions();
    getOptions.Recursive = recursive;
    getOptions.SetFileTime = SetFileTimeType.Modification;

    GetOperations.ProcessCommandGetVersionToLocationOutsideWorkingFolder(
      ensureRepoPath(path), (int)objectVersion, getOptions, dest.getParentFile().getAbsolutePath()
    );
  }

  @NotNull
  private File getCachedFile(@NotNull String path, @NotNull String version) {
    return new File(myCacheFolder, version + "/" + ensureFileSystemPath(path));
  }

  public boolean objectExists(@NotNull String path) throws VcsException {
    return RepositoryUtil.PathExists(ensureRepoPath(path));
  }

  private boolean isFile(@NotNull String path) throws VcsException {
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

  private boolean isFolder(@NotNull String path) throws VcsException {
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
  private Long getFileVersion(@NotNull String path, @NotNull String version) {
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
  private Long getFolderVersion(@NotNull String path, @NotNull String version) {
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
  public String getCurrentVersion() throws VcsException {
    try {
      final VaultTxHistoryItem[] txHistoryItems = ServerOperations.ProcessCommandVersionHistory(ROOT, 0, VaultDate.EmptyDate(), VaultDate.EmptyDate(), 1);
      return String.valueOf(txHistoryItems[0].get_TxID());
    } catch (Throwable th) {
      throw new VcsException(specifyMessage(th.getMessage()), th);
    }
  }

  @Nullable
  public String getDisplayVersion(@NotNull String version) throws VcsException {
    try {
      final Long folderVersion = getFolderVersion(ROOT, version);
      return folderVersion == null ? null : String.valueOf(folderVersion);
    } catch (Throwable th) {
      throw new VcsException(specifyMessage(th.getMessage()), th);
    }
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

  @NotNull
  private String specifyMessage(@NotNull String message) {
    return String.format("%s: Exception occurred while trying to connect to Vault server. See original message below:%s\n",
                         myParameters.getStringRepresentation(), message);
  }
}
