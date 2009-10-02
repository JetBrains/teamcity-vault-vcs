package jetbrains.buildServer.buildTriggers.vcs.vault;

import org.jetbrains.annotations.NotNull;
import VaultClientIntegrationLib.*;
import VaultLib.VaultHistoryItem;
import VaultLib.VaultDate;
import VaultLib.VaultTxHistoryItem;
import jetbrains.buildServer.vcs.VcsException;
import jetbrains.buildServer.util.FileUtil;

import java.io.File;

import VaultClientOperationsLib.SetFileTimeType;
import VaultClientOperationsLib.VaultClientFolder;

/**
 * User: vbedrosova
 * Date: 22.09.2009
 * Time: 14:18:44
 */
public final class VaultConnection1 {
  public static final String ROOT = "$";
  public static final String CURRENT = ".";

  public static void connect(@NotNull VaultConnectionParameters parameters) throws VcsException {
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
      throw new VcsException(e);
    }
  }

  public static void disconnect() throws VcsException {
    try {
      assert ServerOperations.isConnected();
      ServerOperations.Logout();
    } catch (Exception e) {
      throw new VcsException(e);
    }
  }

  public static String getCurrentVersion(@NotNull VaultConnectionParameters parameters) throws VcsException {
    try {
      connect(parameters);
      final VaultHistoryItem[] historyItems = ServerOperations.ProcessCommandHistory("$", true, DateSortOption.desc, null, null, null, null, null, null, -1, -1, 1);
      assert historyItems.length == 0;
      return "" + historyItems[0].get_TxID();
    } catch (Exception e) {
      throw new VcsException(e);
    } finally {
      disconnect();
    }
  }

  public static void testConnection(@NotNull VaultConnectionParameters parameters) throws VcsException {
    connect(parameters);
    disconnect();
  }

//  public static String getPreviousVersion(@NotNull String repoPath, @NotNull String version) throws VcsException {
//    final String prevVersion = getNeighbourVersion(repoPath, version, DateSortOption.desc);
//    return (prevVersion == null) ? prevVersion : VaultUtil.getDateString(new Date(VaultUtil.getDate(version).getTime() - 1000)); // default result
//  }
//
//  public static String getNextVersion(@NotNull String repoPath, @NotNull String version) throws VcsException {
//    final String prevVersion = getNeighbourVersion(repoPath, version, DateSortOption.asc);
//    return (prevVersion == null) ? prevVersion : VaultUtil.getDateString(new Date(VaultUtil.getDate(version).getTime() + 1000)); // default result
//  }
//
//  private static String getNeighbourVersion(@NotNull String repoPath, @NotNull String version, int order) throws VcsException {
//    try {
//      final VaultHistoryItem[] historyItems = ServerOperations.ProcessCommandHistory(repoPath, true, order,
//                                              null, null, null, null, null, null, -1, -1, 1000);
//      for (int i = 0; i < historyItems.length; ++i) {
//        if (version.equals(historyItems[i].get_TxDate().ToLongDateString())) { // TODO: choose date format
//          for (int j = i + 1; j < historyItems.length; ++j) {
//            final String v = historyItems[j].get_TxDate().ToLongDateString(); // TODO: choose date format
//            if (!version.equals(v)) {
//              return v;
//            }
//          }
//        }
//      }
//      return null;
//    } catch (Exception e) {
//      throw new VcsException(e);
//    }
//  }

//  private static String getPreviousVersion(@NotNull String repoPath, @NotNull String version) throws VcsException {
////    final String prevVersion = getNeighbourVersion(repoPath, version, DateSortOption.desc);
////    if (prevVersion != null) {
////      return prevVersion;
////    }
////    throw new VcsException("Unable to get previous version for " + version + " for " + repoPath);
//    try {
//      VaultTxHistoryItem[] items = ServerOperations.ProcessCommandVersionHistory(repoPath, 0, VaultDate.EmptyDate(), VaultDate.EmptyDate(), 1000);
//      for (int i = 0; i < items.length; ++i) {
//        if (version.equals("" + items[i].get_TxID())) {
//          return "" + (items[i].get_Version() - 1);
//        }
//      }
//    throw new VcsException("Unable to get previous version for " + version + " for " + repoPath);
//    } catch (Exception e) {
//      throw new VcsException(e);
//    }
//  }
//
//  private static String getNextVersion(@NotNull String repoPath, @NotNull String version) throws VcsException {
////    final String nextVersion = getNeighbourVersion(repoPath, version, DateSortOption.asc);
////    if (nextVersion != null) {
////      return nextVersion;
////    }
////    throw new VcsException("Unable to get next version for " + version + " for " + repoPath);
//    try {
//      VaultTxHistoryItem[] items = ServerOperations.ProcessCommandVersionHistory(repoPath, 0, VaultDate.EmptyDate(), VaultDate.EmptyDate(), 1000);
//      for (int i = 0; i < items.length; ++i) {
//        if (version.equals("" + items[i].get_TxID())) {
//          return "" + (items[i].get_Version() + 1);
//        }
//      }
//    throw new VcsException("Unable to get next version for " + version + " for " + repoPath);
//    } catch (Exception e) {
//      throw new VcsException(e);
//    }
//  }

//  private static String getNeighbourVersion(@NotNull String repoPath, @NotNull String version, int order) throws VcsException {
//    try {
//      final VaultHistoryItem[] historyItems = ServerOperations.ProcessCommandHistory(repoPath, true, order,
//                                              null, null, null, null, null, null, -1, -1, 1000);
//      for (int i = 0; i < historyItems.length; ++i) {
//        if (version.equals("" + historyItems[i].get_TxID())) { // TODO: choose date format
//          for (int j = i + 1; j < historyItems.length; ++j) {
//            if (!version.equals("" + historyItems[j].get_TxID()) && repoPath.equals(historyItems[j].get_Name())) {
//              return "" + historyItems[j].get_Version();
//            }
//          }
//        }
//      }
//      return null;
//    } catch (Exception e) {
//      throw new VcsException(e);
//    }
//  }

  public static boolean objectExists(@NotNull String repoPath) throws VcsException {
    try {
      return RepositoryUtil.PathExists(repoPath);
    } catch (Exception e) {
      throw new VcsException(e);
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
//        final VaultClientFolder fold = ServerOperations.ProcessCommandListFolder(parent, false);
//        final VaultClientFileColl files = fold.get_Files();
//        for (int i = 0; i < files.get_Count(); ++i) {
//          if (((VaultClientFile) files.get_Item(i)).get_Name().equals(name)) {
//            return true;
//          }
//        }
//        return false;
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
      throw new VcsException(e);
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
    try {
      final File destDir = FileUtil.createTempDirectory("vault_" + version + "_", "");
      final GetOptions getOptions = new GetOptions();
      getOptions.Recursive = recursive;
      getOptions.SetFileTime = SetFileTimeType.Modification;
      GetOperations.ProcessCommandGetVersionToLocationOutsideWorkingFolder(repoPath, (int) version, getOptions, destDir.getAbsolutePath());
      if (isFile(repoPath, version)) {
        return new File(destDir, getName(repoPath));
      } else if (objectExists(repoPath)) {
        return destDir;        
      }
    } catch (Exception e) {
      throw new VcsException(e);
    }    
    throw new VcsException("Object " + repoPath + " is not present at repo");
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

  private static long getVersionByTxId(@NotNull String repoPath, long txIdStr) throws VcsException {
    try {
      if (!objectExists(repoPath)) {
        throw new VcsException("Can't get version by txId " + txIdStr + " for " + repoPath + ": not in repo");
      }
      final long txId = txIdStr;
      if (isFile(repoPath, txIdStr)) {
        final VaultHistoryItem[] historyItems = ServerOperations.ProcessCommandHistory(repoPath, true, DateSortOption.desc,
                                                null, null, null, null, null, null, -1, -1, 1000);

        for (final VaultHistoryItem i : historyItems) {
          if (i.get_TxID() <= txId
              /*&& repoPath.equals(i.get_Name())*/) { // TODO: remove $ at start
            return i.get_Version(); // TODO: int - long
          }
        }
      } else {
        final VaultTxHistoryItem[] txHistoryItems = ServerOperations.ProcessCommandVersionHistory(repoPath, 0, VaultDate.EmptyDate(), VaultDate.EmptyDate(), 1000);
        for (final VaultTxHistoryItem i : txHistoryItems) {
          if (i.get_TxID() <= txId
              /*&& repoPath.equals(i.get_Name())*/) { // TODO: remove $ at start
            return i.get_Version(); // TODO: int - long
          }
        }
      }
    } catch (Exception e) {
      throw new VcsException(e);
    }
    return 0;
  }

  public static VaultHistoryItem[] collectChanges(@NotNull String path,
                                                  @NotNull String fromVersion,
                                                  @NotNull String toVersion) throws VcsException {
    final String repoPath = getRepoPathFromPath(path);
    return ServerOperations.ProcessCommandHistory(repoPath, true, DateSortOption.asc,
                                                  null, null/*"label,obliterate,pin,propertychange"*/ ,
                                                  null, null, null, null,
                                                  getVersionByTxId(repoPath, VaultUtil.parseLong(fromVersion)) + 1,
                                                  getVersionByTxId(repoPath, VaultUtil.parseLong(toVersion)), 1000);
  }

  public static VaultClientFolder listFolder(@NotNull String repoPath) {
    return ServerOperations.ProcessCommandListFolder(repoPath, true);
  }

//  public static TxInfo getTxInfo(long tx) {
//    return ServerOperations.ProcessCommandTxDetail(tx);
//  }
}
