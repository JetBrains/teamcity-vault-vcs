/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import VaultClientOperationsLib.VaultClientFile;
import VaultClientOperationsLib.VaultClientFileColl;
import VaultClientOperationsLib.VaultClientFolder;
import VaultClientOperationsLib.VaultClientFolderColl;
import VaultLib.VaultDateTime;
import VaultLib.VaultHistoryItem;
import VaultLib.VaultHistoryType;
import java.util.*;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.vcs.VcsChangeInfo;
import jetbrains.buildServer.vcs.VcsException;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.buildTriggers.vcs.vault.VaultUtil.*;
import static jetbrains.buildServer.vcs.VcsChangeInfo.Type.*;


/**
 * User: vbedrosova
 * Date: 22.09.2009
 * Time: 14:14:20
 */
public final class VaultChangeCollector {
  private static final Logger LOG = Logger.getLogger(VaultChangeCollector.class);

  @NotNull private final VaultConnection1 myConnection;
  @NotNull private final String myFromVersion;
  @NotNull private final String myToVersion;
  @NotNull private final String myTargetPath;

  @NotNull private final VaultPathHistory myPathHistory;
  @NotNull private final Map<String, Boolean> myIsFileCache;
  @NotNull private final Map<String, String> myDisplayVersionCache;

  @NotNull private final List<String> mySharedPaths;

  public VaultChangeCollector(@NotNull VaultConnection1 connection,
                              @NotNull String fromVersion,
                              @NotNull String toVersion,
                              @Nullable String targetPath) {
    myConnection = connection;
    myFromVersion = fromVersion;
    myToVersion = toVersion;
    myTargetPath = StringUtil.notNullize(targetPath);

    myPathHistory = new VaultPathHistory();
    myIsFileCache = new HashMap<String, Boolean>();
    myDisplayVersionCache = new HashMap<String, String>();
    mySharedPaths = new ArrayList<String>();
  }

  @NotNull
  public List<ChangeInfo> collectChanges() throws VcsException {
    final ArrayList<ChangeInfo> changes = new ArrayList<ChangeInfo>(buildChangesStack());
    Collections.reverse(changes);
    return changes;
  }

  @NotNull
  private Stack<ChangeInfo> buildChangesStack() throws VcsException {
    final Stack<ChangeInfo> changes = new Stack<ChangeInfo>();

    VaultConnection.doInConnection(myConnection.getParameters().asMap(), new VaultConnection.InConnectionProcessor() {
      public void process() throws Throwable {
        @SuppressWarnings("ConstantConditions")
        final VaultHistoryItem[] items= VaultConnection.collectHistory(myTargetPath, myFromVersion, myToVersion);
        for (final VaultHistoryItem item : items) {
          processHistoryItem(changes, item);
        }
      }
    }, false);

    return changes;
  }

  private void processHistoryItem(@NotNull Stack<ChangeInfo> changes, @NotNull VaultHistoryItem item) throws VcsException {
    logHistoryItem(item);

    final int type = item.get_HistItemType();
    final String typeStr = VaultHistoryType.GetHistoryTypeName(type);
    if (NOT_CHANGED_CHANGE_TYPES.contains(typeStr)) {
      LOG.debug("Skipping " + typeStr + " command in history");
      return;
    }
    final String repoPath = VaultUtil.getFullRepoPath(item.get_Name(), myTargetPath);
    if (isSharedPath(repoPath)) {
      LOG.debug("Skipping " + typeStr + " command for " + repoPath
        + " in history, path " + repoPath + " is shared");
      return;
    }
    final String misc1 = item.get_MiscInfo1();
    final String misc2 = item.get_MiscInfo2();

    String comment = item.get_Comment();
    if (comment == null) {
      comment = "No comment";
    }

    final VaultDateTime txDate = item.get_TxDate();
    final Date date = new GregorianCalendar(txDate.get_Year(), txDate.get_Month() - 1,
      txDate.get_Day(), txDate.get_Hour(),
      txDate.get_Minute(), txDate.get_Second()).getTime();
    final String version = "" + item.get_TxID();
    final ModificationInfo mi = new ModificationInfo(version, getDisplayVersion(version), item.get_UserLogin(), comment, date);

    if ("Added".equals(typeStr)) {
      final String oldPath = myPathHistory.getOldPath(repoPath) + "/" + misc1;
      final String newPath = myPathHistory.getNewPath(repoPath + "/" + misc1);
      pushChange(changes, item.GetActionString(), mi, oldPath, isFile(oldPath, newPath, version) ? ADDED : DIRECTORY_ADDED);
      myIsFileCache.remove(newPath);
      myPathHistory.delete(oldPath + "/" + misc1);
      mySharedPaths.remove(newPath);
      return;
    }
    if ("Deleted".equals(typeStr)) {
      final String oldPath = myPathHistory.getOldPath(repoPath) + "/" + misc1;
      pushChange(changes, item.GetActionString(), mi, oldPath, isFile(oldPath, myPathHistory.getNewPath(oldPath), version) ? REMOVED : DIRECTORY_REMOVED);
      return;
    }
    if ("Renamed".equals(typeStr)) {
      final String oldRepoParentPath = getRepoParentPath(myPathHistory.getOldPath(repoPath));
      final String oldPath = oldRepoParentPath + "/" + misc1;
      final String newPath = myPathHistory.getNewPath(oldPath);
      final boolean isFile = isFile(oldPath, newPath, version);
      if (isFile) {
        pushChange(changes, item.GetActionString(), mi, oldPath, ADDED);
        pushChange(changes, item.GetActionString(), mi, oldRepoParentPath + "/" + misc2, REMOVED);
      } else {
        addFolderContent(repoPath, changes, item.GetActionString(), mi);
        pushChange(changes, item.GetActionString(), mi, oldRepoParentPath + "/" + misc2, DIRECTORY_REMOVED);
      }
      myPathHistory.rename(oldRepoParentPath, misc2, misc1);
      return;
    }
    if ("RenamedItem".equals(typeStr)) {
      final String oldRepoParentPath = myPathHistory.getOldPath(repoPath);
      final String oldPath = oldRepoParentPath + "/" + misc1;
      final String newPath = myPathHistory.getNewPath(oldPath);
      if (changes.isEmpty() || !changes.peek().getRepoPath().equals(oldRepoParentPath + "/" + misc2) || !(DIRECTORY_REMOVED.equals(changes.peek().getChangeType()))) {
        final boolean isFile = isFile(oldPath, newPath, version);
        if (isFile) {
          pushChange(changes, item.GetActionString(), mi, oldPath, ADDED);
          pushChange(changes, item.GetActionString(), mi, oldRepoParentPath + "/" + misc2, REMOVED);
        } else {
          addFolderContent(newPath, changes, item.GetActionString(), mi);
          pushChange(changes, item.GetActionString(), mi, oldRepoParentPath + "/" + misc2, DIRECTORY_REMOVED);
        }
        myPathHistory.rename(oldRepoParentPath, misc2, misc1);
      }
      return;
    }
    if ("MovedTo".equals(typeStr)) {
      final String oldRepoParentPath = myPathHistory.getOldPath(repoPath);
      final String oldPath = oldRepoParentPath + "/" + misc1;
      final String newPath = myPathHistory.getNewPath(misc2);
      final boolean isFile = isFile(misc2, newPath, version);
      if (isFile) {
        pushChange(changes, item.GetActionString(), mi, misc2, ADDED);
        pushChange(changes, item.GetActionString(), mi, oldPath, REMOVED);
      } else {
        addFolderContent(newPath, changes, item.GetActionString(), mi);
        pushChange(changes, item.GetActionString(), mi, oldPath, DIRECTORY_REMOVED);
      }
      myPathHistory.move(oldRepoParentPath, getRepoParentPath(misc2), misc1);
      return;
    }
    if ("SharedTo".equals(typeStr)) {
      final String newPath = myPathHistory.getNewPath(misc2);
      final boolean isFile = isFile(misc2, newPath, version);
      if (isFile) {
        pushChange(changes, item.GetActionString(), mi, misc2, ADDED);
      } else {
        addFolderContent(newPath, changes, item.GetActionString(), mi);
      }
      mySharedPaths.add(newPath);
      return;
    }
    if ("CheckIn".equals(typeStr)) {
      pushChange(changes, item.GetActionString(), mi, myPathHistory.getOldPath(repoPath), CHANGED);
      return;
    }
    if ("Undeleted".equals(typeStr)) {
      final String oldPath = myPathHistory.getOldPath(repoPath) + "/" + misc1;
      pushChange(changes, item.GetActionString(), mi, oldPath, isFile(oldPath, myPathHistory.getNewPath(oldPath), version) ? ADDED : DIRECTORY_ADDED);
      return;
    }
    final String oldPath = myPathHistory.getOldPath(repoPath);
    pushChange(changes, item.GetActionString(), mi, oldPath, getType(typeStr, oldPath, version));
  }

  @NotNull
  private String getDisplayVersion(@NotNull String version) throws VcsException {
    String displayVersion = myDisplayVersionCache.get(version);
    if (StringUtil.isEmpty(displayVersion)) {
      displayVersion = String.valueOf(VaultConnection.getVersionByTxIdForFolder(ROOT, VaultUtil.parseLong(version)));
      myDisplayVersionCache.put(version, displayVersion);
    }
    return displayVersion;
  }

  private void pushChange(Stack<ChangeInfo> changes, String actionString, ModificationInfo mi, String path, VcsChangeInfo.Type type) {
    if (ROOT.equals(path) || isSharedPath(path)) {
      return;
    }

    final String relativePath = VaultUtil.getRelativePath(path, myTargetPath);
    if (StringUtil.isNotEmpty(relativePath)) {
      //noinspection ConstantConditions
      changes.push(new ChangeInfo(actionString, path, relativePath, mi, type));
    }
  }

  private boolean isSharedPath(String path) {
    for (final String s : mySharedPaths) {
      if (path.startsWith(s)) {
        return true;
      }
    }
    return false;
  }

  private boolean isFile(String repoPath, String newRepoPath, String version) throws VcsException {
    if (myIsFileCache.containsKey(newRepoPath)) {
      return myIsFileCache.get(newRepoPath);
    }
    Boolean isFile;
    if (VaultConnection.objectExists(newRepoPath)) {
      isFile = VaultConnection.isFileForExistingObject(newRepoPath);
    } else {
      isFile = VaultConnection.isFileForUnxistingObject(repoPath, version);
    }
    myIsFileCache.put(newRepoPath, isFile);
    return isFile;
  }

  private VcsChangeInfo.Type getType(String typeStr, String repoPath, String version) throws VcsException {
    final String newPath = myPathHistory.getNewPath(repoPath);
    if (ADDED_CHANGE_TYPES.contains(typeStr)) {
      return isFile(repoPath, newPath, version) ? ADDED : DIRECTORY_ADDED;
    }
    if (REMOVED_CHANGE_TYPES.contains(typeStr)) {
      return isFile(repoPath, newPath, version) ? REMOVED : DIRECTORY_REMOVED;
    }
    if (CHANGED_CHANGE_TYPES.contains(typeStr)) {
      return CHANGED;
    }
    throw new VcsException("Couldn't reduce " + typeStr + " to one of types (ADDED, DIRECTORY_ADDED, REMOVED, DIRECTORY_REMOVED, CHANGED)");
  }

  private void addFolderContent(String repoFolderPath,
                                Stack<ChangeInfo> changes,
                                String actionString,
                                ModificationInfo mi) throws VcsException {
    if (!VaultConnection.objectExists(repoFolderPath, mi.getVersion())) {
      return;
    }

    final VaultClientFolder fold = VaultConnection.listFolder(repoFolderPath);

    final VaultClientFileColl files = fold.get_Files();
    for (int i = 0; i < files.get_Count(); ++i) {
      final String fileRepoPath = repoFolderPath + "/" + ((VaultClientFile) files.get_Item(i)).get_Name();
      final String oldFileRepoPath = myPathHistory.getOldPath(repoFolderPath + "/" + ((VaultClientFile) files.get_Item(i)).get_Name());
      if (!VaultConnection.objectExists(fileRepoPath, mi.getVersion())) {
        continue;
      }
      pushChange(changes, actionString, mi, oldFileRepoPath, ADDED);
    }

    final VaultClientFolderColl folders = fold.get_Folders();
    for (int i = 0; i < folders.get_Count(); ++i) {
      final String folderRepoPath = repoFolderPath + "/" + ((VaultClientFolder) folders.get_Item(i)).get_Name();
      if (!VaultConnection.objectExists(folderRepoPath, mi.getVersion())) {
        continue;
      }
      addFolderContent(folderRepoPath, changes, actionString, mi);
    }

    pushChange(changes, actionString, mi, myPathHistory.getOldPath(repoFolderPath), DIRECTORY_ADDED);
  }

  private void logHistoryItem(VaultHistoryItem item) {
    LOG.debug("History item: name=" + item.get_Name()
      + ", type=" + item.get_HistItemType()
      + ", misc1=" + item.get_MiscInfo1()
      + ", misc2=" + item.get_MiscInfo2()
      + ", txID=" + item.get_TxID()
      + ", txDate=" + item.get_TxDate()
      + ", user=" + item.get_UserLogin()
      + ", action=" + item.GetActionString()
      + ", comment=" + item.get_Comment());
  }
}