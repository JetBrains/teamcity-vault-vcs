package jetbrains.buildServer.buildTriggers.vcs.vault;

import VaultLib.VaultHistoryItem;
import VaultLib.VaultHistoryType;
import static jetbrains.buildServer.buildTriggers.vcs.vault.VaultUtil.*;
import jetbrains.buildServer.vcs.*;
import static jetbrains.buildServer.vcs.VcsChangeInfo.Type.*;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import VaultClientOperationsLib.VaultClientFolder;
import VaultClientOperationsLib.VaultClientFileColl;
import VaultClientOperationsLib.VaultClientFile;
import VaultClientOperationsLib.VaultClientFolderColl;


/**
 * User: vbedrosova
 * Date: 22.09.2009
 * Time: 14:14:20
 */
public final class VaultChangeCollector implements IncludeRuleChangeCollector {
  private static final Logger LOG = Logger.getLogger(VaultChangeCollector.class);
  private static final String ROOT_PREFIX = "$/";

  private final VcsRoot myRoot;
  private final String myFromVersion;
  private final String myCurrentVersion;


  public VaultChangeCollector(@NotNull VcsRoot root,
                               @NotNull String fromVersion,
                               @Nullable String currentVersion) {
    myRoot = root;
    myFromVersion = fromVersion;
    myCurrentVersion = currentVersion;
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

    final List<ModificationData> modifications = new ArrayList<ModificationData>();

    final Map<ModificationInfo, List<VcsChange>> map = collectModifications(includeRule);
    for (ModificationInfo info : map.keySet()) {
      modifications.add(new ModificationData(info.getDate(), map.get(info), info.getComment(), info.getUser(), myRoot, info.getVersion(), info.getVersion()));
    }

    LOG.debug("Finish collecting changes for root " + myRoot + " for rule " + includeRule.toDescriptiveString()
      + " from version " + myFromVersion + " to version " + myCurrentVersion);
    return modifications;
  }

  public Map<ModificationInfo, List<VcsChange>> collectModifications(@NotNull IncludeRule includeRule) throws VcsException {
    if (myCurrentVersion.equals(myFromVersion)) {
      LOG.debug("Will not collect changes for root " + myRoot + " for rule " + includeRule.toDescriptiveString()
        + " from version " + myFromVersion + " to version " + myCurrentVersion + ", from equals to");
      return Collections.emptyMap();
    }
    final Map<ModificationInfo, List<VcsChange>> modifications = new LinkedHashMap<ModificationInfo, List<VcsChange>>();
    final VaultHistoryItem[] items = VaultConnection1.collectChanges(includeRule.getFrom(), myFromVersion, myCurrentVersion);
    for (int i = 0; i < items.length; ++i) {
      final VaultHistoryItem item = items[i];

      final int type = item.get_HistItemType();
      final String typeStr = VaultHistoryType.GetHistoryTypeName(type);
      if (NOT_CHANGED_CHANGE_TYPES.contains(typeStr)) {
        continue;
      }
      final String version = "" + item.get_TxID();

      String comment = item.get_Comment();
      if (comment == null) {
        comment = "No comment";
      }

      final ModificationInfo modificationInfo = new ModificationInfo(version, item.get_UserLogin(), comment, VaultUtil.getDate(item.get_TxDate().toString()));
      final List<VcsChange> changes;
      if (!modifications.containsKey(modificationInfo)) {
        changes = new ArrayList<VcsChange>();
      } else {
        changes = modifications.get(modificationInfo);
      }

      final String repoPath = item.get_Name().startsWith(VaultConnection1.ROOT) ? item.get_Name() : ROOT_PREFIX + item.get_Name();
      final String histRepoPath = item.get_HistoricName().startsWith(VaultConnection1.ROOT) ? item.get_HistoricName() : ROOT_PREFIX + item.get_HistoricName();
      final String misc1 = item.get_MiscInfo1();
      final String misc2 = item.get_MiscInfo2();

      final String prevVersion = "" + (item.get_TxID() - 1);
      final String actionString = item.GetActionString();

      if ("Created".equals(typeStr)) {
        if (VaultConnection1.isFile(repoPath, version)) {
          collectChange(includeRule, changes, histRepoPath, version, prevVersion, actionString, ADDED);
        } else {
          collectChange(includeRule, changes, histRepoPath, version, prevVersion, actionString, DIRECTORY_ADDED);
        }
      } else if ("Deleted".equals(typeStr)) {
        collectChange(includeRule, changes, histRepoPath + "/" + misc1, version, prevVersion, actionString,
                      getType(repoPath + "/" + misc1, version, "Deleted"));
      } else if ("Renamed".equals(typeStr)) {
        final String name = VaultConnection1.getName(repoPath);
        final String newRepoPath = replaceLast(repoPath, name, misc1);
        final String oldRepoPath = replaceLast(repoPath, name, misc2);
        if (VaultConnection1.isFile(repoPath, version)) {
          collectChange(includeRule, changes, oldRepoPath, version, prevVersion, actionString, REMOVED);
          collectChange(includeRule, changes, newRepoPath,  version, prevVersion, actionString, ADDED);
        } else {
          collectChange(includeRule, changes, oldRepoPath, version, prevVersion, actionString, DIRECTORY_REMOVED);
          addFolderContent(includeRule, repoPath, repoPath, newRepoPath, changes, actionString, version, prevVersion);
        }
      } else if ("SharedTo".equals(typeStr)) {
        if (VaultConnection1.isFile(misc2.replace(histRepoPath, repoPath), version) || VaultConnection1.isFile(misc1, version)) {
          collectChange(includeRule, changes, misc2, version, prevVersion, actionString, ADDED);
        } else {
          addFolderContent(includeRule, misc2, repoPath, histRepoPath, changes, actionString, version, prevVersion);
        }
      } else if ("MovedFrom".equals(typeStr)) {
        final String objRepoPath = repoPath + "/" + misc1;
        final String objHistRepoPath = histRepoPath + "/" + misc1;
        if (VaultConnection1.isFile(objRepoPath, version)) {
          collectChange(includeRule, changes, misc2, version, prevVersion, actionString, REMOVED);
          collectChange(includeRule, changes, objHistRepoPath, version, prevVersion, actionString, ADDED);
        } else {
          collectChange(includeRule, changes, misc2, version, prevVersion, actionString, DIRECTORY_REMOVED);
          addFolderContent(includeRule, objRepoPath, repoPath, histRepoPath, changes, actionString, version, prevVersion);
        }
      } else if ("CheckIn".equals(typeStr)) {
        if (VaultConnection1.isFile(repoPath, version)) {
          collectChange(includeRule, changes, histRepoPath, version, prevVersion, actionString, CHANGED);
        } else {
          collectChange(includeRule, changes, histRepoPath, version, prevVersion, actionString, DIRECTORY_CHANGED);
        }
      } else {
        final VcsChangeInfo.Type changeType = getType(repoPath, version, typeStr);
        collectChange(includeRule, changes, repoPath, version, prevVersion, actionString, changeType);
      }
      modifications.put(modificationInfo, changes);
    }

    return modifications;
  }

  private void collectChange(@NotNull IncludeRule includeRule,
                             @NotNull List<VcsChange> changes, @NotNull String repoPath,
                             @NotNull String version, @NotNull String prevVersion, @NotNull String actionString,
                             @NotNull VcsChangeInfo.Type type) {
    if (VaultConnection1.ROOT.equals(repoPath)) {
      return;
    }
    String relativePath = VaultConnection1.getPathFromRepoPath(repoPath);

    final String from = includeRule.getFrom();
    final String to = includeRule.getTo();
    if (!relativePath.equals(from) && relativePath.startsWith(from) && !"".equals(from)) {
        relativePath = replaceFirst(relativePath, from, to);
    }

    changes.add(new VcsChange(type, actionString, relativePath, relativePath, prevVersion, version));
  }

  public void dispose() throws VcsException {
    VaultConnection1.disconnect();
  }

  private VcsChangeInfo.Type getType(@NotNull String repoPath, @NotNull String version, String type) throws VcsException {
    if (ADDED_CHANGE_TYPES.contains(type)) {
      if (VaultConnection1.isFile(repoPath, version)) {
        return ADDED;
      } else if (VaultConnection1.objectExists(repoPath)) {
        return DIRECTORY_ADDED;
      }
    } else if (CHANGED_CHANGE_TYPES.contains(type)) {
      if (VaultConnection1.isFile(repoPath, version)) {
        return CHANGED;
      } else if (VaultConnection1.objectExists(repoPath)) {
        return DIRECTORY_CHANGED;
      }
    } else if (REMOVED_CHANGE_TYPES.contains(type)) {
      if (VaultConnection1.isFile(repoPath, version)) {
        return REMOVED;
      } else {
        return DIRECTORY_REMOVED;
      }
    }
    throw new VcsException("Couldn't get object type(file/folder) for repo object: " + repoPath);
  }

  private void addFolderContent(@NotNull IncludeRule includeRule,
                                @NotNull String repoFolderPath, @NotNull String currentRepoPath, @NotNull String histRepoPath,
                                @NotNull List<VcsChange> changes,
                                @NotNull String actionString,
                                @NotNull String version,
                                @NotNull String prevVersion) throws VcsException {
    collectChange(includeRule, changes, repoFolderPath.replace(currentRepoPath, histRepoPath), version, prevVersion, actionString, DIRECTORY_ADDED);

    final VaultClientFolder fold = VaultConnection1.listFolder(repoFolderPath);

    final VaultClientFileColl files = fold.get_Files();
    for (int i = 0; i < files.get_Count(); ++i) {
      final String fileRepoPath = (repoFolderPath + "/" + ((VaultClientFile) files.get_Item(i)).get_Name());
      if (!VaultConnection1.objectExists(fileRepoPath, version)) {
        continue;
      }
      collectChange(includeRule, changes, fileRepoPath.replace(currentRepoPath, histRepoPath), version, prevVersion, actionString, ADDED);
    }

    final VaultClientFolderColl folders = fold.get_Folders();
    for (int i = 0; i < folders.get_Count(); ++i) {
      final String folderRepoPath = (repoFolderPath + "/" + ((VaultClientFolder) folders.get_Item(i)).get_Name());
      if (!VaultConnection1.objectExists(folderRepoPath, version)) {
        continue;
      }
      addFolderContent(includeRule, folderRepoPath, currentRepoPath, histRepoPath, changes, actionString, version, prevVersion);
    }
  }

  public static final class ModificationInfo {
    private final String myVersion;
    private final String myUser;
    private final String myComment;
    private final Date myDate;

    public ModificationInfo(String version, String user, String comment, Date date) {
      myVersion = version;
      myUser = user;
      myComment = comment;
      myDate = date;
    }

    public String getVersion() {
      return myVersion;
    }

    public String getUser() {
      return myUser;
    }

    public Date getDate() {
      return myDate;
    }

    public String getComment() {
      return myComment;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof ModificationInfo)) return false;

      ModificationInfo that = (ModificationInfo) o;

      if (myComment != null ? !myComment.equals(that.myComment) : that.myComment != null) return false;
      if (myDate != null ? !myDate.equals(that.myDate) : that.myDate != null) return false;
      if (myUser != null ? !myUser.equals(that.myUser) : that.myUser != null) return false;
      if (myVersion != null ? !myVersion.equals(that.myVersion) : that.myVersion != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = myVersion != null ? myVersion.hashCode() : 0;
      result = 31 * result + (myUser != null ? myUser.hashCode() : 0);
      result = 31 * result + (myComment != null ? myComment.hashCode() : 0);
      result = 31 * result + (myDate != null ? myDate.hashCode() : 0);
      return result;
    }
  }
}
