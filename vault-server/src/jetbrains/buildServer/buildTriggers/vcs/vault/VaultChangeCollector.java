package jetbrains.buildServer.buildTriggers.vcs.vault;

import VaultLib.VaultHistoryItem;
import VaultLib.VaultHistoryType;
import VaultLib.VaultDateTime;
import static jetbrains.buildServer.buildTriggers.vcs.vault.VaultUtil.*;
import static jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection1.ROOT;
import static jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection1.ROOT_PREFIX;
import static jetbrains.buildServer.vcs.VcsChangeInfo.Type.*;
import jetbrains.buildServer.vcs.*;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import VaultClientOperationsLib.*;


/**
 * User: vbedrosova
 * Date: 22.09.2009
 * Time: 14:14:20
 */
public final class VaultChangeCollector implements IncludeRuleChangeCollector {
  private static final Logger LOG = Logger.getLogger(VaultChangeCollector.class);

  private final VcsRoot myRoot;
  private final String myFromVersion;
  private final String myCurrentVersion;


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

    try {
      VaultConnection1.connect(new VaultConnectionParameters(myRoot));
    } catch (VcsException e) {
      LOG.error("Unable to connect to Vault", e);
    }
  }

  @NotNull
  public List<ModificationData> collectChanges(@NotNull IncludeRule includeRule) throws VcsException {
    LOG.debug("Start collecting changes for root " + myRoot + " for rule " + includeRule.toDescriptiveString()
      + " from version " + myFromVersion + " to version " + myCurrentVersion);

    final List<ModificationData> modifications = new LinkedList<ModificationData>();
    final Map<ModificationInfo, List<VcsChange>> map = collectModifications(includeRule);
    for (ModificationInfo mi : map.keySet()) {
      modifications.add(new ModificationData(mi.getDate(), map.get(mi), mi.getComment(), mi.getUser(), myRoot, mi.getVersion(), mi.getVersion()));
    }

    LOG.debug("Finish collecting changes for root " + myRoot + " for rule " + includeRule.toDescriptiveString()
      + " from version " + myFromVersion + " to version " + myCurrentVersion);
    return modifications;
  }

  public Map<ModificationInfo, List<VcsChange>> collectModifications(@NotNull IncludeRule includeRule) throws VcsException {
    final Map<ModificationInfo, List<VcsChange>> map = new LinkedHashMap<ModificationInfo, List<VcsChange>>();
    final Stack<ChangeInfo> changeStack = collectCanges(includeRule);
    while (!changeStack.isEmpty()) {
      final ChangeInfo ci = changeStack.pop();
      final ModificationInfo mi = ci.getModificationInfo();

      List<VcsChange> changes;
      if (map.containsKey(mi)) {
        changes = map.get(mi);
      } else {
        changes = new LinkedList<VcsChange>();
      }

      final String version = "" + mi.getVersion();
      final String prevVersion = "" + (VaultUtil.parseLong(version) - 1);
      String repoPath = ci.getRepoPath();

      collectChange(includeRule, changes, repoPath, version, prevVersion, ci.getChangeName(), ci.getChangeType());
      map.put(mi, changes);
    }
    return map;
  }

  private void collectChange(@NotNull IncludeRule includeRule,
                             @NotNull List<VcsChange> changes, @NotNull String repoPath,
                             @NotNull String version, @NotNull String prevVersion, @NotNull String changeName,
                             @NotNull VcsChangeInfo.Type type) {
    if (ROOT.equals(repoPath)) {
      return;
    }
    String relativePath = VaultConnection1.getPathFromRepoPath(repoPath);

    final String from = includeRule.getFrom();
    final String to = includeRule.getTo();
    if (!relativePath.equals(from) && relativePath.startsWith(from) && !"".equals(from)) {
        relativePath = replaceFirst(relativePath, from, to);
    }

    changes.add(new VcsChange(type, changeName, relativePath, relativePath, prevVersion, version));
  }  

  public Stack<ChangeInfo> collectCanges(@NotNull IncludeRule includeRule) throws VcsException {
    final Stack<ChangeInfo> changes = new Stack<ChangeInfo>();
    if (myCurrentVersion.equals(myFromVersion)) {
      LOG.debug("Will not collect changes for root " + myRoot + " for rule " + includeRule.toDescriptiveString()
        + " from version " + myFromVersion + " to version " + myCurrentVersion + ", from equals to");
      return changes;
    }

    final VaultHistoryItem[] items = VaultConnection1.collectChanges(includeRule.getFrom(), myFromVersion, myCurrentVersion);

    for (final VaultHistoryItem item : items) {
      final int type = item.get_HistItemType();
      final String typeStr = VaultHistoryType.GetHistoryTypeName(type);
      if (NOT_CHANGED_CHANGE_TYPES.contains(typeStr)) {
        LOG.debug("Skipping " + typeStr + " command in history");
        continue;
      }
      final String repoPath = item.get_Name().startsWith(ROOT) ? item.get_Name() : ROOT_PREFIX + item.get_Name();
      if (isSharedPath(repoPath)) {
        LOG.debug("Skipping " + typeStr + " command for " + repoPath + " in history, path is shared");
        continue;        
      }
      final String misc1 = item.get_MiscInfo1();
      final String misc2 = item.get_MiscInfo2();

      String comment = item.get_Comment();
      if (comment == null) {
        comment = "No comment";
      }

      final VaultDateTime txDate = item.get_TxDate();
      final Date date = new GregorianCalendar(txDate.get_Year(), txDate.get_Month(),
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
        continue;
      }
      if ("Deleted".equals(typeStr)) {
        final String oldPath = myPathHistory.getOldPath(repoPath) + "/" + misc1;
        pushChange(changes, item.GetActionString(), mi, oldPath, isFile(oldPath, myPathHistory.getNewPath(oldPath), version) ? REMOVED : DIRECTORY_REMOVED);
        continue;
      }
      if ("Renamed".equals(typeStr)) {
        final String oldRepoParentPath = VaultConnection1.getRepoParentPath(myPathHistory.getOldPath(repoPath));
        final String oldPath = oldRepoParentPath + "/" + misc1;
        final String newPath = myPathHistory.getNewPath(oldPath);
        final boolean isFile = isFile(oldPath, newPath, version);
        if (isFile) {
          pushChange(changes, item.GetActionString(), mi, oldPath, ADDED);
          pushChange(changes, item.GetActionString(), mi, oldRepoParentPath + "/" + misc2, REMOVED);
        } else {
          if (!isSharedPath(newPath)) {
            addFolderContent(includeRule, repoPath,  changes, item.GetActionString(), mi);
            pushChange(changes, item.GetActionString(), mi, oldRepoParentPath + "/" + misc2, DIRECTORY_REMOVED);
          }
        }
        myPathHistory.rename(oldRepoParentPath, misc2, misc1);
        continue;
      }
      if ("RenamedItem".equals(typeStr)) {
        final String oldRepoParentPath = myPathHistory.getOldPath(repoPath);
        final String oldPath = oldRepoParentPath + "/" + misc1;
        if (changes.isEmpty() || !changes.peek().getRepoPath().equals(oldRepoParentPath + "/" + misc2) || !(DIRECTORY_REMOVED.equals(changes.peek().getChangeType()))) {
          final boolean isFile = isFile(oldPath, myPathHistory.getNewPath(oldPath), version);
          if (isFile) {
            pushChange(changes, item.GetActionString(), mi, oldPath, ADDED);
            pushChange(changes, item.GetActionString(), mi, oldRepoParentPath + "/" + misc2, REMOVED);
          } else {
            addFolderContent(includeRule, repoPath, changes, item.GetActionString(), mi);
            pushChange(changes, item.GetActionString(), mi, oldRepoParentPath + "/" + misc2, DIRECTORY_REMOVED);
          }
          myPathHistory.rename(oldRepoParentPath, misc2, misc1);
        }
        continue;
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
          addFolderContent(includeRule, newPath, changes, item.GetActionString(), mi);
          pushChange(changes, item.GetActionString(), mi, oldPath, DIRECTORY_REMOVED);
        }
        myPathHistory.move(oldRepoParentPath, VaultConnection1.getRepoParentPath(misc2), misc1);
        continue;
      }
      if ("SharedTo".equals(typeStr)) {
        final String newPath = myPathHistory.getNewPath(misc2);
        final boolean isFile = isFile(misc2, newPath, version);
        if (isFile) {
          pushChange(changes, item.GetActionString(), mi, misc2, ADDED);
        } else {
          addFolderContent(includeRule, newPath, changes, item.GetActionString(), mi);
        }
        mySharedPaths.add(newPath);
        continue;
      }
      if ("CheckIn".equals(typeStr)) {
        pushChange(changes, item.GetActionString(), mi, myPathHistory.getOldPath(repoPath), CHANGED);
        continue;
      }
      final String oldPath = myPathHistory.getOldPath(repoPath);
      changes.push(new ChangeInfo(item.GetActionString(), oldPath, mi, getType(typeStr, oldPath, version)));
    }
    return changes;
  }

  private void pushChange(@NotNull Stack<ChangeInfo> changes, String actionString, ModificationInfo mi, String path, VcsChangeInfo.Type type) throws VcsException {
    changes.push(new ChangeInfo(actionString, path, mi, type));
  }

  private boolean isSharedPath(@NotNull String path) {
    for (final String s : mySharedPaths) {
      if (path.startsWith(s)) {
        return true;
      }
    }
    return false;
  }

  private boolean isFile(@NotNull String repoPath, @NotNull String newRepoPath, @NotNull String version) throws VcsException {
    if (myObjectTypesCache.containsKey(newRepoPath)) {
      return myObjectTypesCache.get(newRepoPath);
    }
    Boolean isFile;
    if (VaultConnection1.objectExists(newRepoPath)) {
      isFile = VaultConnection1.isFileForExistingObject(newRepoPath);
    } else {
      isFile = VaultConnection1.isFileForUnxistingObject(repoPath, version);
    }
    myObjectTypesCache.put(newRepoPath, isFile);
    return isFile;
  }

  private VcsChangeInfo.Type getType(@NotNull String typeStr, @NotNull String repoPath, @NotNull String version) throws VcsException {
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
    throw new VcsException("Couldn't get one of types (ADDED, DELETED, REMOVED) for " + typeStr);
  }

  private static class ChangeInfo {
    @NotNull private final String myChangeName;
    @NotNull private final String myRepoPath;
    @NotNull private final ModificationInfo myModificationInfo;
    @NotNull private final VcsChangeInfo.Type myChangeType;

    public ChangeInfo(@NotNull String changeName,
                       @NotNull String repoPath,
                       @NotNull ModificationInfo modificationInfo,
                       @NotNull VcsChangeInfo.Type changeType) {
      myChangeName = changeName;
      myRepoPath = repoPath;
      myModificationInfo = modificationInfo;
      myChangeType = changeType;
    }

    @NotNull public String getChangeName() {
      return myChangeName;
    }

    @NotNull public String getRepoPath() {
      return myRepoPath;
    }

    @NotNull public ModificationInfo getModificationInfo() {
      return myModificationInfo;
    }

    @NotNull public VcsChangeInfo.Type getChangeType() {
      return myChangeType;
    }
  }

  public void dispose() throws VcsException {
    VaultConnection1.disconnect();
  }

  private void addFolderContent(@NotNull IncludeRule includeRule,
                                @NotNull String repoFolderPath,
                                @NotNull Stack<ChangeInfo> changes,
                                @NotNull String actionString,
                                @NotNull ModificationInfo mi) throws VcsException {
    if (!VaultConnection1.objectExists(repoFolderPath, mi.getVersion())) {
      return;
    }

    changes.push(new ChangeInfo(actionString, myPathHistory.getOldPath(repoFolderPath), mi, DIRECTORY_ADDED));

    final VaultClientFolder fold = VaultConnection1.listFolder(repoFolderPath);

    final VaultClientFileColl files = fold.get_Files();
    for (int i = 0; i < files.get_Count(); ++i) {
      final String fileRepoPath = myPathHistory.getOldPath(repoFolderPath + "/" + ((VaultClientFile) files.get_Item(i)).get_Name());
      if (!VaultConnection1.objectExists(fileRepoPath, mi.getVersion())) {
        continue;
      }
      changes.push(new ChangeInfo(actionString, fileRepoPath, mi, ADDED));
    }

    final VaultClientFolderColl folders = fold.get_Folders();
    for (int i = 0; i < folders.get_Count(); ++i) {
      final String folderRepoPath = (repoFolderPath + "/" + ((VaultClientFolder) folders.get_Item(i)).get_Name());
      final String oldFolderRepoPath = myPathHistory.getOldPath(folderRepoPath);
      if (!VaultConnection1.objectExists(oldFolderRepoPath, mi.getVersion())) {
        continue;
      }
      addFolderContent(includeRule, folderRepoPath, changes, actionString, mi);
    }
  }


  public static final class ModificationInfo {
    @NotNull private final String myVersion;
    @NotNull private final String myUser;
    @NotNull private final String myComment;
    @NotNull private final Date myDate;

    public ModificationInfo(@NotNull String version, @NotNull String user, @NotNull String comment, @NotNull Date date) {
      myVersion = version;
      myUser = user;
      myComment = comment;
      myDate = date;
    }

    @NotNull public String getVersion() {
      return myVersion;
    }

    @NotNull public String getUser() {
      return myUser;
    }

    @NotNull public Date getDate() {
      return myDate;
    }

    @NotNull public String getComment() {
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
      if (!myVersion.equals(that.myVersion)) return false;

      return true;
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
