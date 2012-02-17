/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import jetbrains.buildServer.vcs.*;
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
public final class VaultChangeCollector implements IncludeRuleChangeCollector {
  private static final Logger LOG = Logger.getLogger(VaultChangeCollector.class);

  @NotNull private final VcsRoot myRoot;
  @NotNull private final String myFromVersion;
  @Nullable private String myCurrentVersion;

  private final VaultPathHistory myPathHistory;
  private final Map<String, Boolean> myObjectTypesCache;

  private final List<String> mySharedPaths;

  public VaultChangeCollector(@NotNull VcsRoot root,
                              @NotNull String fromVersion,
                              @Nullable String currentVersion) {
    myRoot = root;
    myFromVersion = fromVersion;
    myCurrentVersion = currentVersion;

    myPathHistory = new VaultPathHistory();
    myObjectTypesCache = new HashMap<String, Boolean>();
    mySharedPaths = new ArrayList<String>();
  }

  @SuppressWarnings("RedundantThrowsDeclaration")
  public void dispose() throws VcsException {
  }

  @NotNull
  public List<ModificationData> collectChanges(@NotNull IncludeRule includeRule) throws VcsException {
    VaultUtil.checkIncludeRule(myRoot, includeRule);

    logStartCollectingChanges(includeRule);

    final List<ModificationData> changes = new LinkedList<ModificationData>();

    final Map<ModificationInfo, List<VcsChange>> modifications = collectModifications(includeRule);
    if (!modifications.isEmpty()) {
      for (ModificationInfo mi : modifications.keySet()) {
        changes.add(new ModificationData(mi.getDate(), modifications.get(mi), mi.getComment(), mi.getUser(),
          myRoot, mi.getVersion(), VaultConnection.getDisplayVersion(mi.getVersion(), myRoot.getProperties())));
      }
    }

    logFinishCollectingChanges(includeRule);

    clear();

    return changes;
  }

  @NotNull
  public Map<ModificationInfo, List<VcsChange>> collectModifications(@NotNull IncludeRule includeRule) throws VcsException {
    prepareCurrentVersion();

    if (myFromVersion.equals(myCurrentVersion)) {
      logWillNotCollectChanges(includeRule);
      return Collections.emptyMap();
    }

    final Map<ModificationInfo, List<VcsChange>> modifications = new LinkedHashMap<ModificationInfo, List<VcsChange>>();
    final Stack<ChangeInfo> changesStack = buildChangesStack(includeRule.getFrom());

    while (!changesStack.isEmpty()) {
      final ChangeInfo ci = changesStack.pop();
      final ModificationInfo mi = ci.getModificationInfo();

      final List<VcsChange> changes;
      if (modifications.containsKey(mi)) {
        changes = modifications.get(mi);
      } else {
        changes = new LinkedList<VcsChange>();
      }
      collectChange(changes, ci.getRepoPath(), includeRule.getFrom(), mi.getVersion(), ci.getChangeName(), ci.getChangeType());

      modifications.put(mi, changes);
    }
    return modifications;
  }

  private Stack<ChangeInfo> buildChangesStack(final String includeRuleFrom) throws VcsException {
    final Stack<ChangeInfo> changes = new Stack<ChangeInfo>();

    VaultConnection.doInConnection(myRoot.getProperties(), new VaultConnection.InConnectionProcessor() {
      public void process() throws Throwable {
        @SuppressWarnings("ConstantConditions")
        final VaultHistoryItem[] items= VaultConnection.collectHistory(includeRuleFrom, myFromVersion, myCurrentVersion);
        for (final VaultHistoryItem item : items) {
          processHistoryItem(changes, item, includeRuleFrom);
        }
      }
    }, false);

    return changes;
  }

  private void collectChange(List<VcsChange> changes,
                             String repoPath, String includeRuleFrom,
                             String version, String changeName, VcsChangeInfo.Type type) throws VcsException {
    String relativePath = getPathFromRepoPath(repoPath);

    if (!"".equals(includeRuleFrom)) {
      if (relativePath.startsWith(includeRuleFrom)) {
        relativePath = relativePath.substring(includeRuleFrom.length() + 1);
      } else {
        LOG.debug("Relative path " + relativePath + " in repo doesn't start with include rule \"from\" " + includeRuleFrom);
         return;
      }
    }

    changes.add(new VcsChange(type, changeName, relativePath, relativePath, "" + (VaultUtil.parseLong(version) - 1), version));
  }

  private void processHistoryItem(Stack<ChangeInfo> changes, VaultHistoryItem item, String includeRuleFrom) throws VcsException {
    logHistoryItem(item);

    final int type = item.get_HistItemType();
    final String typeStr = VaultHistoryType.GetHistoryTypeName(type);
    if (NOT_CHANGED_CHANGE_TYPES.contains(typeStr)) {
      LOG.debug("Skipping " + typeStr + " command in history");
      return;
    }
    final String repoPath = getFullPath(item.get_Name(), includeRuleFrom);
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
    final ModificationInfo mi = new ModificationInfo(version, item.get_UserLogin(), comment, date);

    if ("Added".equals(typeStr)) {
      final String oldPath = myPathHistory.getOldPath(repoPath) + "/" + misc1;
      final String newPath = myPathHistory.getNewPath(repoPath + "/" + misc1);
      pushChange(changes, item.GetActionString(), mi, oldPath, isFile(oldPath, newPath, version) ? ADDED : DIRECTORY_ADDED);
      myObjectTypesCache.remove(newPath);
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

  private String getFullPath(String path, String includeRuleFrom) {
    if (path.startsWith(ROOT)) {
      return path;
    }
    includeRuleFrom = includeRuleFrom.replace('\\', '/');
    if (includeRuleFrom.endsWith("/")) {
      includeRuleFrom = includeRuleFrom.substring(0, includeRuleFrom.length() - 1);
    }
    if (includeRuleFrom.contains("/")) {
      includeRuleFrom = includeRuleFrom.substring(0, includeRuleFrom.lastIndexOf("/") + 1);
    } else {
      includeRuleFrom = "";
    }
    return ROOT_PREFIX + includeRuleFrom + path;
  }

  private void pushChange(Stack<ChangeInfo> changes, String actionString, ModificationInfo mi, String path, VcsChangeInfo.Type type) {
    if (ROOT.equals(path) || isSharedPath(path)) {
      return;
    }
    changes.push(new ChangeInfo(actionString, path, mi, type));
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
    if (myObjectTypesCache.containsKey(newRepoPath)) {
      return myObjectTypesCache.get(newRepoPath);
    }
    Boolean isFile;
    if (VaultConnection.objectExists(newRepoPath)) {
      isFile = VaultConnection.isFileForExistingObject(newRepoPath);
    } else {
      isFile = VaultConnection.isFileForUnxistingObject(repoPath, version);
    }
    myObjectTypesCache.put(newRepoPath, isFile);
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
      changes.push(new ChangeInfo(actionString, oldFileRepoPath, mi, ADDED));
    }

    final VaultClientFolderColl folders = fold.get_Folders();
    for (int i = 0; i < folders.get_Count(); ++i) {
      final String folderRepoPath = repoFolderPath + "/" + ((VaultClientFolder) folders.get_Item(i)).get_Name();
      if (!VaultConnection.objectExists(folderRepoPath, mi.getVersion())) {
        continue;
      }
      addFolderContent(folderRepoPath, changes, actionString, mi);
    }

    changes.push(new ChangeInfo(actionString, myPathHistory.getOldPath(repoFolderPath), mi, DIRECTORY_ADDED));
  }

  private void prepareCurrentVersion() throws VcsException {
    if (myCurrentVersion == null) {
      LOG.debug("Current version for change collecting is null, so need to get current version");
      myCurrentVersion = VaultConnection.getCurrentVersion(myRoot.getProperties());
    }
  }

  private void logFinishCollectingChanges(IncludeRule includeRule) {
    LOG.debug("Finish collecting changes for root " + myRoot + " for rule " + includeRule.toDescriptiveString()
      + " from version " + myFromVersion + " to version " + myCurrentVersion);
  }

  private void logStartCollectingChanges(IncludeRule includeRule) {
    LOG.debug("Start collecting changes for root " + myRoot + " for rule " + includeRule.toDescriptiveString()
      + " from version " + myFromVersion + " to version " + myCurrentVersion);
  }

  private void logWillNotCollectChanges(IncludeRule includeRule) {
    LOG.debug("Will not collect changes for root " + myRoot + " for rule " + includeRule.toDescriptiveString()
      + " from version " + myFromVersion + " to version " + myCurrentVersion + ", from equals to");
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

  private void clear() {
    myPathHistory.clear();
    myObjectTypesCache.clear();
    mySharedPaths.clear();
  }  

  private static class ChangeInfo {
    @NotNull
    private final String myChangeName;
    @NotNull
    private final String myRepoPath;
    @NotNull
    private final ModificationInfo myModificationInfo;
    @NotNull
    private final VcsChangeInfo.Type myChangeType;

    public ChangeInfo(@NotNull String changeName,
                      @NotNull String repoPath,
                      @NotNull ModificationInfo modificationInfo,
                      @NotNull VcsChangeInfo.Type changeType) {
      myChangeName = changeName;
      myRepoPath = repoPath;
      myModificationInfo = modificationInfo;
      myChangeType = changeType;
    }

    @NotNull
    public String getChangeName() {
      return myChangeName;
    }

    @NotNull
    public String getRepoPath() {
      return myRepoPath;
    }

    @NotNull
    public ModificationInfo getModificationInfo() {
      return myModificationInfo;
    }

    @NotNull
    public VcsChangeInfo.Type getChangeType() {
      return myChangeType;
    }
  }

  public static final class ModificationInfo {
    @NotNull
    private final String myVersion;
    @NotNull
    private final String myUser;
    @NotNull
    private final String myComment;
    @NotNull
    private final Date myDate;

    public ModificationInfo(@NotNull String version, @NotNull String user, @NotNull String comment, @NotNull Date date) {
      myVersion = version;
      myUser = user;
      myComment = comment;
      myDate = date;
    }

    @NotNull
    public String getVersion() {
      return myVersion;
    }

    @NotNull
    public String getUser() {
      return myUser;
    }

    @NotNull
    public Date getDate() {
      return myDate;
    }

    @NotNull
    public String getComment() {
      return myComment;
    }


    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ModificationInfo that = (ModificationInfo) o;

      if (!myComment.equals(that.myComment)) return false;
      if (!myDate.equals(that.myDate)) return false;
      if (!myUser.equals(that.myUser)) return false;
      return myVersion.equals(that.myVersion);
    }

    @Override
    public int hashCode() {
      int result = myVersion.hashCode();
      result = 31 * result + myUser.hashCode();
      result = 31 * result + myComment.hashCode();
      result = 31 * result + myDate.hashCode();
      return result;
    }
  }
}
