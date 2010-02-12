/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;
import org.apache.log4j.Logger;
import VaultClientIntegrationLib.*;
import VaultLib.VaultHistoryItem;
import VaultLib.VaultDate;
import VaultLib.VaultTxHistoryItem;
import jetbrains.buildServer.vcs.VcsException;
import jetbrains.buildServer.vcs.VcsRoot;
import jetbrains.buildServer.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import VaultClientOperationsLib.SetFileTimeType;
import VaultClientOperationsLib.VaultClientFolder;

/**
 * User: vbedrosova
 * Date: 22.09.2009
 * Time: 14:18:44
 */
public final class VaultConnection {
  public static final Object LOCK = new Object();

  private static final Logger LOG = Logger.getLogger(VaultConnection.class);

  public static final String ROOT = "$";
  public static final String ROOT_PREFIX = "$/";
  public static final String SEPARATOR = "/";
  public static final String CURRENT = ".";

  private static final int CONNECTION_TRIES_NUMBER = 10;

  private static final List<File> ourTempFiles = new ArrayList<File>();

  public static void connect(@NotNull Map<String, String> parameters) throws VcsException {
    for (int i = 1; i <= CONNECTION_TRIES_NUMBER; ++i) {
      try {
        connectNotForce(parameters);
        return;
      } catch (NoClassDefFoundError e) {
        throw new VcsException(VaultUtil.NO_API_FOUND_MESSAGE, e);
      } catch (Throwable e) {
        disconnect();
        if (i == CONNECTION_TRIES_NUMBER) {
          throw new VcsException(specifyMessage(e.getMessage()), e);
        }
      }
    }
  }

  private static void connectNotForce(Map<String, String> parameters) throws Throwable {
    ServerOperations.client.LoginOptions.URL = parameters.get(VaultUtil.SERVER);
    ServerOperations.client.LoginOptions.Repository = parameters.get(VaultUtil.REPO);
    ServerOperations.client.LoginOptions.User = parameters.get(VaultUtil.USER);
    ServerOperations.client.LoginOptions.Password = parameters.get(VaultUtil.PASSWORD);
    ServerOperations.Login();
  }

  public static void disconnect() {
    try {
      if (ServerOperations.isConnected()) {
        ServerOperations.Logout();
      }
    } catch (Exception e) {
      LOG.error("Exception occurred when disconnecting from Vault server", e);
    } finally {
      removeTempFiles();
    }
  }

  public static String getCurrentVersion(@NotNull Map<String, String> parameters) throws VcsException {
    synchronized (LOCK) {
      connect(parameters);
      try {
        final VaultHistoryItem[] historyItems = ServerOperations.ProcessCommandHistory("$", true, DateSortOption.desc, null, null, null, null, null, null, -1, -1, 1);
        return "" + historyItems[0].get_TxID();
      } catch (Exception e) {
        throw new VcsException(specifyMessage(e.getMessage()), e);
      } finally {
        VaultConnection.disconnect();
      }
    }
  }

  public static void testConnection(@NotNull Map<String, String> parameters) throws VcsException {
    synchronized (LOCK) {
      try {
        connect(parameters);
      } finally {
        disconnect();
      }
    }
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

  public static boolean isFileForExistingObject(@NotNull String repoPath) throws VcsException {
    try {
      RepositoryUtil.FindVaultFileAtReposOrLocalPath(repoPath);
      return true;
    } catch (Exception e) {
      return false;
    }
}

  public static boolean isFileForUnxistingObject(@NotNull String repoPath, @NotNull String version) throws VcsException {
    return isFileForUnexistingObject(repoPath, VaultUtil.parseLong(version));
  }

  private static boolean isFileForUnexistingObject(@NotNull String repoPath, long version) throws VcsException {
    final String parent = getRepoParentPath(repoPath);
    return getObjectFromParent(getName(repoPath), parent, version).isFile();
  }

  public static File getObject(@NotNull String path, @NotNull String version) throws VcsException {
    final String repoPath = getRepoPathFromPath(path);
    return getRepoObject(repoPath, VaultUtil.parseLong(version), true);
  }

  private static File getRepoObject(@NotNull String repoPath, long version, boolean recursive) throws VcsException {
    LOG.debug("Getting repo object: " + repoPath + ", version: " + version + ", rec: " + recursive);
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
    if (isFileForExistingObject(repoPath)) {
      if (VaultCache.cacheEnabled()) {
        return getFileUsingCache(repoPath, version);
      }
      return getFileFromVcs(repoPath, version);
    } else if (objectExists(repoPath)) {
      if (VaultCache.cacheEnabled()) {
        return getFolderUsingCache(repoPath, version, recursive);
      }
      return getFolderFromVcs(repoPath, version, recursive);
    }
    throw new VcsException("Object " + repoPath + " is not present at repo");
  }

  private static File getFileUsingCache(@NotNull String repoPath, long version) throws VcsException {
    final File cachedFile = VaultCache.getCache(repoPath, version);
    if (cachedFile.isFile()) {
      LOG.debug("Found cached file " + cachedFile.getAbsolutePath() + " for " + repoPath + " at version " + version);
      return cachedFile;
    }

    LOG.debug("Couldn't find cached file " + cachedFile.getAbsolutePath() + " for " + repoPath + " at version " + version + ", will get it from repo");
    LOG.debug("Saving cached file " + cachedFile.getAbsolutePath() + " for " + repoPath + " at version " + version);

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
    final File cachedFolder = VaultCache.getCache(repoPath, version);
    if (cachedFolder .isDirectory()) {
      LOG.debug("Found cached folder " + cachedFolder .getAbsolutePath() + " for " + repoPath + " at version " + version);
      return cachedFolder ;
    }

    LOG.debug("Couldn't find cached folder " + cachedFolder .getAbsolutePath() + " for " + repoPath + " at version " + version + ", will get it from repo");
    LOG.debug("Saving cached folder " + cachedFolder .getAbsolutePath() + " for " + repoPath + " at version " + version);

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

  private static File getFileFromVcs(@NotNull String repoPath, long version) throws VcsException {
    return new File(getObjectToDirFromVcs(repoPath, version, false), getName(repoPath));
  }

  private static File getFolderFromVcs(@NotNull String repoPath, long version, boolean  recursive) throws VcsException {
    return getObjectToDirFromVcs(repoPath, version, recursive);
  }

  private static File getObjectToDirFromVcs(@NotNull String repoPath, long version, boolean  recursive) throws VcsException {
    try {
      VaultUtil.TEMP_DIR.mkdirs();
      final File destDir = FileUtil.createTempDirectory("vault_", "", VaultUtil.TEMP_DIR);
      ourTempFiles.add(destDir);
      final GetOptions getOptions = new GetOptions();
      getOptions.Recursive = recursive;
      getOptions.SetFileTime = SetFileTimeType.Modification;
      GetOperations.ProcessCommandGetVersionToLocationOutsideWorkingFolder(repoPath, (int) version, getOptions, destDir.getAbsolutePath());
      return destDir;
    } catch (Exception e) {
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

  public static String getRepoParentPath(@NotNull String repoPath) {
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
        return 0;
      }
      if (isFileForExistingObject(repoPath)) {
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
                                           @NotNull String fromVersion,
                                           @NotNull String toVersion) throws VcsException {
    final String repoPath = path.startsWith(ROOT_PREFIX) ? path : getRepoPathFromPath(path);
    final long objectFromVersion = getVersionByTxId(repoPath, VaultUtil.parseLong(fromVersion)) + 1;
    final long objectToVersion = getVersionByTxId(repoPath, VaultUtil.parseLong(toVersion));
    if (objectFromVersion > objectToVersion) {
      return new VaultHistoryItem[0];
    }
    return ServerOperations.ProcessCommandHistory(repoPath, true, DateSortOption.desc,
                                                  null, null/*"label,obliterate,pin,propertychange"*/ ,
                                                  null, null, null, null,
                                                  objectFromVersion, objectToVersion, 1000);
  }

  public static VaultClientFolder listFolder(@NotNull String repoPath) {
    return ServerOperations.ProcessCommandListFolder(repoPath, true);
  }

  private static String specifyMessage(String message) {
    return "Exception occured while trying to connect to Vault server. See original message below:\n" + message;   
  }

  private static void removeTempFiles() {
    final List<File> toDelete = new ArrayList<File>();
    for (final File f : ourTempFiles) {
      if (hardDelete(f)) {
        toDelete.add(f);
      }
    }
    ourTempFiles.removeAll(toDelete);
  }

  private static boolean hardDelete(File f) {
    for (int i = 0; i < 10; ++i) {
      if (FileUtil.delete(f)) {
        return true;
      }
    }
    return false;
  }
}
