package jetbrains.buildServer.buildTriggers.vcs.vault;

import org.jetbrains.annotations.NotNull;
import org.apache.log4j.Logger;
import VaultClientIntegrationLib.*;
import VaultLib.VaultHistoryItem;
import VaultLib.VaultDate;
import VaultLib.VaultTxHistoryItem;
import jetbrains.buildServer.vcs.VcsException;
import jetbrains.buildServer.util.FileUtil;

import java.io.File;
import java.io.IOException;

import VaultClientOperationsLib.SetFileTimeType;
import VaultClientOperationsLib.VaultClientFolder;

/**
 * User: vbedrosova
 * Date: 22.09.2009
 * Time: 14:18:44
 */
public final class VaultConnection1 {
  private static final Logger LOG = Logger.getLogger(VaultConnection1.class);

  public static final String ROOT = "$";
  public static final String ROOT_PREFIX = "$/";
  public static final String CURRENT = ".";

  private static final int CONNECTION_TRIES_NUMBER = 10;

  private static File myCachesDir = null;

  public static void connect(@NotNull VaultConnectionParameters parameters) throws VcsException {
    for (int i = 1; i <= CONNECTION_TRIES_NUMBER; ++i) {
      try {
        connectNotForce(parameters);
        return;
      } catch (VcsException e) {
        if (("Repository " + parameters.getRepoName() + " not found").equals(e.getMessage())) {
          throw e;
        }
        if (i == CONNECTION_TRIES_NUMBER) {
          throw new VcsException(e.getMessage(), e.getCause());
        }
      }
    }
  }

  public static void connectNotForce(@NotNull VaultConnectionParameters parameters) throws VcsException {
    if (ServerOperations.isConnected()) {
      return;
    }
    try {
      ServerOperations.client.LoginOptions.URL = parameters.getUrl();
      ServerOperations.client.LoginOptions.Repository = parameters.getRepoName();
      ServerOperations.client.LoginOptions.User = parameters.getUser();
      ServerOperations.client.LoginOptions.Password = parameters.getPassword();
      ServerOperations.Login();
    } catch (Exception e) {
      throw new VcsException(e.getMessage(), e);
    }
  }

  public static void disconnect() throws VcsException {
    try {
      assert ServerOperations.isConnected();
      ServerOperations.Logout();
    } catch (Exception e) {
      throw new VcsException(e.getMessage(), e);
    }
  }

  public static void enableCache(File cachesDir) {
    myCachesDir = cachesDir;
  }

  private static boolean cacheEnabled() {
    return myCachesDir != null;
  }

  public static String getCurrentVersion(@NotNull VaultConnectionParameters parameters) throws VcsException {
    try {
      connect(parameters);
      final VaultHistoryItem[] historyItems = ServerOperations.ProcessCommandHistory("$", true, DateSortOption.desc, null, null, null, null, null, null, -1, -1, 1);
      assert historyItems.length == 0;
      return "" + historyItems[0].get_TxID();
    } catch (Exception e) {
      throw new VcsException(e.getMessage(), e);
    } finally {
      disconnect();
    }
  }

  public static void testConnection(@NotNull VaultConnectionParameters parameters) throws VcsException {
    connectNotForce(parameters);
    disconnect();
  }

  public static boolean objectExists(@NotNull String repoPath) throws VcsException {
    try {
      return RepositoryUtil.PathExists(repoPath);
    } catch (Exception e) {
      throw new VcsException(e.getMessage(), e);
    }
  }

  public static boolean objectExists(@NotNull String repoPath, @NotNull String version) throws VcsException {
    return getVersionByTxId(repoPath, VaultUtil.parseLong(version)) != 0;
  }

  public static boolean isFile(@NotNull String repoPath, @NotNull String version) throws VcsException {
    return isFile(repoPath, VaultUtil.parseLong(version));
  }

  private static boolean isFile(@NotNull String repoPath, long version) throws VcsException {
    try {
      if (objectExists(repoPath)) {
        try {
          RepositoryUtil.FindVaultFileAtReposOrLocalPath(repoPath);
          return true;
        } catch (Exception e) {
          return false;
        }
      } else {
        final String parent = getRepoParentPath(repoPath);
        return getObjectFromParent(getName(repoPath), parent, version).isFile();
      }
    } catch (Exception e) {
      throw new VcsException(e.getMessage(), e);
    }
  }

  public static File getObject(@NotNull String path, @NotNull String version) throws VcsException {
    final String repoPath = getRepoPathFromPath(path);
    return getRepoObject(repoPath, VaultUtil.parseLong(version), true);
  }

  private static File getRepoObject(@NotNull String repoPath, long version, boolean recursive) throws VcsException {
    if (objectExists(repoPath)) {
      long objVersion = getVersionByTxId(repoPath, version);
      long txId = version;
      while ((objVersion == 0) && (txId != 0)) {
        objVersion = getVersionByTxId(repoPath, txId);
        --txId;
      }
      if (txId == 0) {
        throw new VcsException("Unable to get existing object " + repoPath + " at version " + version);
      }
      return getObjectItself(repoPath, objVersion, recursive);
    }
    final String name = getName(repoPath);
    final String parent = getRepoParentPath(repoPath);

    return getObjectFromParent(name, parent, version);
  }

  private static File getObjectItself(@NotNull String repoPath, long version, boolean  recursive) throws VcsException {
    if (isFile(repoPath, version)) {
      if (cacheEnabled()) {
        return getFileUsingCache(repoPath, version);
      }
      return getFileFromVcs(repoPath, version);
    } else if (objectExists(repoPath)) {
      if (cacheEnabled()) {
        return getFolderUsingCache(repoPath, version, recursive);
      }
      return getFolderFromVcs(repoPath, version, recursive);
    }
    throw new VcsException("Object " + repoPath + " is not present at repo");
  }

  private static File getFileUsingCache(@NotNull String repoPath, long version) throws VcsException {
    final File cachedFile = getCache(repoPath, version);
    if (cachedFile.isFile()) {
      LOG.debug("Found cached file " + cachedFile.getAbsolutePath() + " for " + repoPath + " at version " + version);
      System.out.println("Found cached file " + cachedFile.getAbsolutePath() + " for " + repoPath + " at version " + version);
      return cachedFile;
    }
    LOG.debug("Couldn't find cached file " + cachedFile.getAbsolutePath() + " for " + repoPath + " at version " + version + ", will get it from repo");
    System.out.println("Couldn't find cached file " + cachedFile.getAbsolutePath() + " for " + repoPath + " at version " + version + ", will get it from repo");
    LOG.debug("Saving cached file " + cachedFile.getAbsolutePath() + " for " + repoPath + " at version " + version);
    System.out.println("Saving cached file " + cachedFile.getAbsolutePath() + " for " + repoPath + " at version " + version);
    final File getFile = getFileFromVcs(repoPath, version);
    if (!getFile.isFile()) {
      throw new VcsException("Couldn't get file " + repoPath + " at version" + " from repo");      
    }
    try {
      FileUtil.copy(getFile, cachedFile);
    } catch (IOException e) {
      throw new VcsException(e.getMessage(), e);
    }
    return cachedFile;
  }

  private static File getFolderUsingCache(@NotNull String repoPath, long version, boolean  recursive) throws VcsException {
    final File cachedFolder = getCache(repoPath, version);
    if (cachedFolder .isDirectory()) {
      LOG.debug("Found cached folder " + cachedFolder .getAbsolutePath() + " for " + repoPath + " at version " + version);
      System.out.println("Found cached folder " + cachedFolder .getAbsolutePath() + " for " + repoPath + " at version " + version);
      return cachedFolder ;
    }
    LOG.debug("Couldn't find cached folder " + cachedFolder .getAbsolutePath() + " for " + repoPath + " at version " + version + ", will get it from repo");
    System.out.println("Couldn't find cached folder " + cachedFolder .getAbsolutePath() + " for " + repoPath + " at version " + version + ", will get it from repo");
    LOG.debug("Saving cached folder " + cachedFolder .getAbsolutePath() + " for " + repoPath + " at version " + version);
    System.out.println("Saving cached folder " + cachedFolder .getAbsolutePath() + " for " + repoPath + " at version " + version);
    final File getFolder = getFolderFromVcs(repoPath, version, recursive);
    if (!getFolder.isDirectory()) {
      throw new VcsException("Couldn't get folder " + repoPath + " at version" + " from repo");
    }
    try {
      FileUtil.copyDir(getFolder, cachedFolder);
    } catch (IOException e) {
      throw new VcsException(e.getMessage(), e);
    }
    return cachedFolder ;
  }

  private static File getCache(@NotNull String repoPath, long version) throws VcsException {
    if (myCachesDir == null) {
      throw new VcsException("Unable to get cache for " + repoPath + " at version " + version +
                             ", file caching is disabled");
    }
    String cacheName;
    if (ROOT.equals(repoPath)) {
      cacheName = "root";      
    } else if (repoPath.startsWith("$/")) {
      cacheName = repoPath.substring(2).replace("/", "_");       
    } else {
      throw new VcsException("Unable to get cache for " + repoPath + " at version " + version +
                             ", repo path must start with $");
    }
    return new File(myCachesDir, cacheName + "_" + version);
  }

  private static File getFileFromVcs(@NotNull String repoPath, long version) throws VcsException {
    return new File(getObjectToDirFromVcs(repoPath, version, false), getName(repoPath));
  }

  private static File getFolderFromVcs(@NotNull String repoPath, long version, boolean  recursive) throws VcsException {
    return getObjectToDirFromVcs(repoPath, version, recursive);
  }

  private static File getObjectToDirFromVcs(@NotNull String repoPath, long version, boolean  recursive) throws VcsException {
    try {
      final File destDir = FileUtil.createTempDirectory("vault_" + version + "_", "");
      final GetOptions getOptions = new GetOptions();
      getOptions.Recursive = recursive;
      getOptions.SetFileTime = SetFileTimeType.Modification;
      GetOperations.ProcessCommandGetVersionToLocationOutsideWorkingFolder(repoPath, (int) version, getOptions, destDir.getAbsolutePath());
      return destDir;
    } catch (IOException e) {
      throw new VcsException(e.getMessage(), e);
    }
  }

  private static File getObjectFromParent(@NotNull String name, @NotNull String parent, long version) throws VcsException {
    File f =  new File(getRepoObject(parent, version, true), name);
    long txId = version;
    while (!f.exists() && (txId != 1)) {
      --txId;
      f = new File(getRepoObject(parent, txId, true), name);
    }
    if (txId == 1) {
      throw new VcsException("Unable to get " + name + " at version "  + version + " from parent " + parent);      
    }
    return f;
  }

  private static String getRepoParentPath(@NotNull String repoPath) {
    return ROOT.equals(repoPath) ? "" : repoPath.substring(0, repoPath.lastIndexOf("/"));
  }

  public static String getName(@NotNull String repoPath) {
    return ROOT.equals(repoPath) ? ROOT : repoPath.substring(repoPath.lastIndexOf("/") + 1);
  }

  private static String getRepoPathFromPath(@NotNull String path) {
    return ("".equals(path) || CURRENT.equals(path)) ? ROOT : ROOT + "/" + path.replace("\\", "/");
  }

  public static String getPathFromRepoPath(@NotNull String repoPath) {
    return ROOT.equals(repoPath) ? "" : repoPath.substring(2);
  }

  private static long getVersionByTxId(@NotNull String repoPath, long txId) throws VcsException {
    try {
      if (!objectExists(repoPath)) {
        throw new VcsException("Can't get version by txId " + txId + " for " + repoPath + ": not in repo");
      }
      if (isFile(repoPath, txId)) {
        final VaultHistoryItem[] historyItems = ServerOperations.ProcessCommandHistory(repoPath, true, DateSortOption.desc,
                                                null, null, null, null, null, null, -1, -1, 1000);

        for (final VaultHistoryItem i : historyItems) {
          if (i.get_TxID() <= txId) {
            return i.get_Version(); // TODO: int - long
          }
        }
      } else {
        final VaultTxHistoryItem[] txHistoryItems = ServerOperations.ProcessCommandVersionHistory(repoPath, 0, VaultDate.EmptyDate(), VaultDate.EmptyDate(), 1000);
        for (final VaultTxHistoryItem i : txHistoryItems) {
          if (i.get_TxID() <= txId) {
            return i.get_Version(); // TODO: int - long
          }
        }
      }
    } catch (Exception e) {
      throw new VcsException(e.getMessage(), e);
    }
    return 0;
  }

  public static VaultHistoryItem[] collectChanges(@NotNull String path,
                                                  String fromVersion,
                                                  String toVersion) throws VcsException {
    final String repoPath = path.startsWith(ROOT_PREFIX) ? path : getRepoPathFromPath(path);
    final long objectFromVersion = (fromVersion == null) ? -1 : getVersionByTxId(repoPath, VaultUtil.parseLong(fromVersion)) + 1;
    final long objectToVersion = (toVersion == null) ? -1 : getVersionByTxId(repoPath, VaultUtil.parseLong(toVersion));
    if (objectFromVersion > objectToVersion) {
      return new VaultHistoryItem[0];

    }
    return ServerOperations.ProcessCommandHistory(repoPath, true, DateSortOption.asc,
                                                  null, null/*"label,obliterate,pin,propertychange"*/ ,
                                                  null, null, null, null,
                                                  objectFromVersion, objectToVersion, 1000);
  }

  public static VaultClientFolder listFolder(@NotNull String repoPath) {
    return ServerOperations.ProcessCommandListFolder(repoPath, true);
  }
}
