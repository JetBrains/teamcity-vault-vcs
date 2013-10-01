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

import java.io.File;
import java.util.*;

import jetbrains.buildServer.util.FileUtil;
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

  @NotNull private final VaultConnection myConnection;
  @NotNull private final String myFromVersion;
  @NotNull private final String myToVersion;
  @NotNull private final String myTargetPath;

  @NotNull private final VaultPathHistory myPathHistory;
  @NotNull private final Map<String, Boolean> myIsFileCache;
  @NotNull private final Map<String, String> myDisplayVersionCache;

  @NotNull private final List<String> mySharedPaths;

  public VaultChangeCollector(@NotNull VaultConnection connection,
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

    for (RawChangeInfo rawChangeInfo : myConnection.getFolderHistory(myTargetPath, myFromVersion, myToVersion)) {
      processRawChangeInfo(changes, rawChangeInfo);
    }

    return changes;
  }

  private void processRawChangeInfo(@NotNull Stack<ChangeInfo> changes, @NotNull RawChangeInfo rawChangeInfo) throws VcsException {

    final String repoPath = VaultUtil.getFullRepoPath(rawChangeInfo.getPath(), myTargetPath);

    if (isSharedPath(repoPath)) {
      LOG.debug("Skipping " + rawChangeInfo + ", " + repoPath + " is shared");
      return;
    }

    LOG.debug(rawChangeInfo);

    final String misc1 = rawChangeInfo.getAdditionalPath1();
    final String misc2 = rawChangeInfo.getAdditionalPath2();
    final String version = rawChangeInfo.getVersion();

    final ModificationInfo mi = new ModificationInfo(version, getDisplayVersion(version), rawChangeInfo.getUser(), rawChangeInfo.getComment(), rawChangeInfo.getDate());

    final String changeName = rawChangeInfo.getChangeName();
    final RawChangeInfo.RawChangeInfoType type = rawChangeInfo.getType();

    switch (type) {

      case ADDED: {
        final String histPath = myPathHistory.getOldPath(repoPath) + "/" + misc1;
        final String currPath = myPathHistory.getNewPath(histPath);
        final boolean isFile = isFile(currPath, histPath, version);

        pushChange(changes, changeName, mi, histPath, type.getChangeInfoType(isFile));

        myPathHistory.delete(histPath);

        myIsFileCache.remove(currPath);
        mySharedPaths.remove(histPath);

        break;
      }

      case DELETED: {
        final String histPath = myPathHistory.getOldPath(repoPath) + "/" + misc1;
        final String currPath = myPathHistory.getNewPath(histPath);
        final boolean isFile = isFile(currPath, histPath, mi.getPrevVersion());

        pushChange(changes, changeName, mi, histPath, type.getChangeInfoType(isFile));

        break;
      }

      case RENAMED: {
        final String histPath = myPathHistory.getOldPath(repoPath);
        final String currPath = myPathHistory.getNewPath(histPath);
        final boolean isFile = isFile(currPath, histPath, version);

        final String histParentPath = getRepoParentPath(histPath);

        if (isFile) {
          pushChange(changes, changeName, mi, histPath, ADDED);
          pushChange(changes, changeName, mi, histParentPath + "/" + misc2, REMOVED);
        } else {
          addFolderContent(histPath, changes, changeName, mi);
          pushChange(changes, changeName, mi, histParentPath + "/" + misc2, DIRECTORY_REMOVED);
        }

        myPathHistory.rename(histParentPath, misc2, misc1);

        myIsFileCache.remove(currPath);
        mySharedPaths.remove(histPath);

        break;
      }

      case RENAMED_ITEM: {
        final String histParentPath = myPathHistory.getOldPath(repoPath);

        final String histPath = histParentPath + "/" + misc1;
        final String currPath = myPathHistory.getNewPath(histPath);

        if (changes.isEmpty() || !changes.peek().getRepoPath().equals(histParentPath + "/" + misc2) || !(DIRECTORY_REMOVED.equals(changes.peek().getChangeType()))) {
          final boolean isFile = isFile(currPath, histPath, version);
          if (isFile) {
            pushChange(changes, changeName, mi, histPath, ADDED);
            pushChange(changes, changeName, mi, histParentPath + "/" + misc2, REMOVED);
          } else {
            addFolderContent(histPath, changes, changeName, mi);
            pushChange(changes, changeName, mi, histParentPath + "/" + misc2, DIRECTORY_REMOVED);
          }

          myPathHistory.rename(histParentPath, misc2, misc1);

          myIsFileCache.remove(currPath);
          mySharedPaths.remove(histPath);
        }
        break;
      }

      case MOVED_TO: {
        final String histParentPath = myPathHistory.getOldPath(repoPath);

        final String histPath = misc2;
        final String currPath = myPathHistory.getNewPath(histPath);
        final boolean isFile = isFile(currPath, histPath, version);

        if (isFile) {
          pushChange(changes, changeName, mi, histPath, ADDED);
          pushChange(changes, changeName, mi, histParentPath + "/" + misc1, REMOVED);
        } else {
          addFolderContent(histPath, changes, changeName, mi);
          pushChange(changes, changeName, mi, histParentPath + "/" + misc1, DIRECTORY_REMOVED);
        }

        myPathHistory.move(histParentPath, getRepoParentPath(misc2), misc1);

        myIsFileCache.remove(currPath);
        mySharedPaths.remove(histPath);

        break;
      }

      case SHARED_TO: {
        final String histPath = misc2;
        final String currPath = myPathHistory.getNewPath(histPath);
        final boolean isFile = isFile(currPath, histPath, version);

        if (isFile) {
          pushChange(changes, changeName, mi, histPath, ADDED);
        } else {
          addFolderContent(histPath, changes, changeName, mi);
        }

        myIsFileCache.remove(currPath);
        mySharedPaths.add(currPath);

        break;
      }

      case CHECK_IN: {
        pushChange(changes, changeName, mi, myPathHistory.getOldPath(repoPath), CHANGED); // it's always a file
        break;
      }

      case UNDELETED: {
        final String histPath = myPathHistory.getOldPath(repoPath) + "/" + misc1;
        final String currPath = myPathHistory.getNewPath(histPath);
        final boolean isFile = isFile(currPath, histPath, version);

        pushChange(changes, changeName, mi, histPath, type.getChangeInfoType(isFile));

        break;
      }

      default:
        final String histPath = myPathHistory.getOldPath(repoPath);
        final String currPath = myPathHistory.getNewPath(histPath);
        final boolean isFile = isFile(currPath, histPath, version);

        pushChange(changes, changeName, mi, histPath, type.getChangeInfoType(isFile));

        break;
    }
  }

  @NotNull
  private String getDisplayVersion(@NotNull String version) throws VcsException {
    String displayVersion = myDisplayVersionCache.get(version);
    if (StringUtil.isEmpty(displayVersion)) {
      displayVersion = String.valueOf(myConnection.getFolderDisplayVersion(ROOT, version));
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

  private boolean isSharedPath(@NotNull String path) {
    for (final String s : mySharedPaths) {
      if (path.startsWith(s)) {
        return true;
      }
    }
    return false;
  }

  private boolean isFile(@NotNull String currentPath, @NotNull String historyPath, @NotNull String version) throws VcsException {
    Boolean isFile = myIsFileCache.get(currentPath);

    if (isFile == null) {
      isFile = myConnection.getExistingObject(historyPath, version).isFile();
      myIsFileCache.put(currentPath, isFile);
    }

    return isFile;
  }

  private void addFolderContent(@NotNull String historyFolderPath,
                                @NotNull Stack<ChangeInfo> changes,
                                @Nullable String actionString,
                                @NotNull ModificationInfo mi) throws VcsException {
    final File folder = myConnection.getExistingObject(historyFolderPath, mi.getVersion());
    final File[] files = folder.listFiles();

    if (files == null) return;

    for (File file : files) {
      final String oldPath = historyFolderPath  + "/" + FileUtil.getRelativePath(folder, file);

      if (file.isFile()) {
        pushChange(changes, actionString, mi, oldPath, ADDED);

      } else if (file.isDirectory()) {

        addFolderContent(oldPath, changes, actionString, mi);
      }
    }
    pushChange(changes, actionString, mi, historyFolderPath, DIRECTORY_ADDED);
  }
}