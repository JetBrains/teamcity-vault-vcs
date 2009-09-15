/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import jetbrains.buildServer.vcs.*;
import static jetbrains.buildServer.buildTriggers.vcs.vault.VaultUtil.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.log4j.Logger;
import org.xml.sax.*;

import java.util.*;
import java.io.File;

import com.intellij.execution.configurations.GeneralCommandLine;

/**
 * User: vbedrosova
 * Date: 03.07.2009
 * Time: 13:26:40
 */
public final class VaultChangeCollector implements IncludeRuleChangeCollector {
  private static final Logger LOG = Logger.getLogger(VaultChangeCollector.class);

  private final VaultConnection myConnection;
  private final VcsRoot myRoot;
  private final String myFromVersion;
  private final String myCurrentVersion;


  public VaultChangeCollector(@NotNull VaultConnection connection,
                              @NotNull VcsRoot root,
                              @NotNull String fromVersion,
                              @Nullable String currentVersion) {
    myConnection = connection;
    myRoot = root;
    myFromVersion = fromVersion;
    myCurrentVersion = currentVersion;
  }

  @NotNull
  public List<ModificationData> collectChanges(@NotNull IncludeRule includeRule) throws VcsException {
    LOG.debug("Start collecting changes for rule " + includeRule.toDescriptiveString());
    final List<ModificationData> modifications = new ArrayList<ModificationData>();

    final Map<ModificationInfo, List<VcsChange>> map = collectModifications(includeRule);
    for (ModificationInfo info : map.keySet()) {
      modifications.add(new ModificationData(info.getDate(), map.get(info), info.getComment(), info.getUser(), myRoot, info.getVersion(), info.getDate().toString()));
    }
    LOG.debug("Finish collecting changes for rule " + includeRule.toDescriptiveString());
    return modifications;
  }

  public void dispose() throws VcsException {
  }

  public Map<ModificationInfo, List<VcsChange>> collectModifications(@NotNull IncludeRule includeRule) throws VcsException {
    final GeneralCommandLine cl = new GeneralCommandLine();
    cl.setExePath(myRoot.getProperty("vault.path"));
    cl.addParameter("history");
    cl.addParameter("-server");
    cl.addParameter(myRoot.getProperty("vault.server"));
    cl.addParameter("-user");
    cl.addParameter(myRoot.getProperty("vault.user"));
    cl.addParameter("-password");
    cl.addParameter(myRoot.getProperty("secure:vault.password"));
    cl.addParameter("-repository");
    cl.addParameter(myRoot.getProperty("vault.repo"));

    cl.addParameter("-begindate");
    cl.addParameter(getNextRootVersion(myFromVersion));
    cl.addParameter("-enddate");
    cl.addParameter(getNextRootVersion(myCurrentVersion));

//      -excludeactions action,action,...
//
//      A comma-separated list of actions that will be excluded from
//      the history query. Valid actions to exclude are:
//      add, branch, checkin, create, delete, label, move, obliterate, pin,
//      propertychange, rename, rollback, share, snapshot, undelete.

// TODO: add this
//    cl.addParameter("-excludeactions");
//    cl.addParameter("label,obliterate,pin,propertychange");
//    cl.addParameter("add,branch,label,obliterate,pin,propertychange,snapshot");

    cl.addParameter("$");
    final HistoryHandler handler = new HistoryHandler();
    myConnection.runCommand(cl, handler);
    return handler.getModifications();
  }

  private String getPreviousRootVersion(@NotNull String version) throws VcsException {
    final String defaultResult = VaultUtil.getDateString(new Date(VaultUtil.getDate(version).getTime() - 1000));
    return getRootVersion(new PreviousVersionHandler(version, defaultResult));
  }

  private String getNextRootVersion(@NotNull String version) throws VcsException {
    final String defaultResult = VaultUtil.getDateString(new Date(VaultUtil.getDate(version).getTime() + 1000));
    return getRootVersion(new NextVersionHandler(version, defaultResult));
  }

  private String getRootVersion(@NotNull VersionHandler handler)
    throws VcsException {
    final GeneralCommandLine cl = new GeneralCommandLine();
    cl.setExePath(myRoot.getProperty("vault.path"));
    cl.addParameter("history");
    cl.addParameter("-server");
    cl.addParameter(myRoot.getProperty("vault.server"));
    cl.addParameter("-user");
    cl.addParameter(myRoot.getProperty("vault.user"));
    cl.addParameter("-password");
    cl.addParameter(myRoot.getProperty("secure:vault.password"));
    cl.addParameter("-repository");
    cl.addParameter(myRoot.getProperty("vault.repo"));

    cl.addParameter("$");
    myConnection.runCommand(cl, handler);

    return handler.getDesiredVersion();
  }

  private class VersionHandler extends VaultConnection.Handler {
    protected final String myVersion;
    protected String myDesiredVersion;

    public VersionHandler(String version, String defaultResult) {
      myVersion = version;
      myDesiredVersion = defaultResult;
    }

    public String getDesiredVersion() {
      return myDesiredVersion;
    }
  }

  private class PreviousVersionHandler extends VersionHandler {
    private boolean myCameAcrossCurrentVersion = false;

    public PreviousVersionHandler(String version, String defaultResult) {
      super(version, defaultResult);
    }

    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
      throws SAXException {
      if ("item".equals(localName)) {
        final String date = attributes.getValue("date");
        if (myCameAcrossCurrentVersion && !date.equals(myVersion)) {
          myDesiredVersion = date;
          throw VaultConnection.SUCCESS;
        } else if (myVersion.equals(date)) {
          myCameAcrossCurrentVersion = true;
        }
      }
    }
  }

  private class NextVersionHandler extends VersionHandler {
    protected String myCurrentVersion;

    public NextVersionHandler(String version, String defaultResult) {
      super(version, defaultResult);
      myCurrentVersion = defaultResult;
    }

    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
      throws SAXException {
      if ("item".equals(localName)) {
        final String version = attributes.getValue("date");
        if (myVersion.equals(version)) {
          myDesiredVersion = myCurrentVersion;
          throw VaultConnection.SUCCESS;
        } else {
          myCurrentVersion = version;
        }
      }
    }
  }

  private final class HistoryHandler extends VaultConnection.Handler {
//        public static final byte Added = 10;
//        public static final byte BranchedFrom = 20;
//        public static final byte BranchedFromItem = 30;
//        public static final byte BranchedFromShare = 40;
//        public static final byte BranchedFromShareItem = 50;
//        public static final byte CheckIn = 60;
//        public static final byte Created = 70;
//        public static final byte Deleted = 80;
//        public static final byte Label = 90;
//        public static final byte MovedFrom = 120;
//        public static final byte MovedTo = -126;
//        public static final byte Obliterated = -116;
//        public static final byte Pinned = -106;
//        public static final byte PropertyChange = -96;
//        public static final byte Renamed = -86;
//        public static final byte RenamedItem = -76;
//        public static final byte SharedTo = -66;
//        public static final byte Snapshot = -56;
//        public static final byte SnapshotFrom = -55;
//        public static final byte SnapshotItem = -54;
//        public static final byte Undeleted = -46;
//        public static final byte UnPinned = -36;
    //TODO: process rollback
//        public static final byte Rollback = -26;

    private final Map<ModificationInfo, List<VcsChange>> myModifications
      = new HashMap<ModificationInfo, List<VcsChange>>();

    public Map<ModificationInfo, List<VcsChange>> getModifications() {
      return myModifications;
    }

    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
      throws SAXException {
      if ("item".equals(localName)) {
        final String typeName = attributes.getValue("typeName");
        if (NOT_CHANGED_CHANGE_TYPES.contains(typeName)) {
          return;
        }
        final String version = attributes.getValue("date");
        final Date date;
        try {
          date = VaultUtil.getDate(version);
        } catch (VcsException e) {
          throw new SAXException(e);
        }
        String comment = attributes.getValue("comment");
        if (comment == null) {
          comment = "No comment";
        }
        final ModificationInfo modificationInfo = new ModificationInfo(version, attributes.getValue("user"), comment, date);
        final List<VcsChange> changes;
        if (!myModifications.containsKey(modificationInfo)) {
          changes = new ArrayList<VcsChange>();
        } else {
          changes = myModifications.get(modificationInfo);
        }
        final String actionString = attributes.getValue("actionString");
        String relativeRepoPath = attributes.getValue("name");
        if ("Deleted".equals(typeName)) {
          relativeRepoPath += getDeletedName(actionString);
        } else if ("MovedTo".equals(typeName)) {
          relativeRepoPath = getMovedToName(actionString);
        } else if ("MovedFrom".equals(typeName)) {
          relativeRepoPath = getMovedFromName(actionString);
        } else if ("SharedTo".equals(typeName)) {
          relativeRepoPath = getSharedToName(actionString);
        }
        String relativePath = getPathFromRepoPath(relativeRepoPath);
        if (relativePath.length() == 0) {
          return;
        }
        final String name = relativePath.substring(relativePath.lastIndexOf(File.separator) + 1);
        try {
          final String prevVersion = getPreviousRootVersion(version);
          if ("Renamed".equals(typeName)) {
            renameFolderContent(relativeRepoPath, relativeRepoPath.replace(name, getRenamedFromName(actionString)), version, prevVersion, actionString, changes);
          } else if ("SharedTo".equals(typeName)) {
            addFolderContent(relativeRepoPath, version, prevVersion, actionString, changes);
          } else {
            final VcsChangeInfo.Type type = getType(relativePath, typeName, version, prevVersion);
            changes.add(new VcsChange(type, actionString, relativePath, relativePath, prevVersion, version));
          }
        } catch (VcsException e) {
          throw new SAXException(e);
        }
        myModifications.put(modificationInfo, changes);
      }
    }

    private String getPathFromRepoPath(@NotNull String relativeRepoPath) {
      String relativePath = relativeRepoPath;
      if (relativePath.startsWith("$")) {
        relativePath = relativePath.substring(1);
      }
      relativePath = relativePath.replace("/", File.separator).replace("\\", File.separator);
      if (relativePath.startsWith(File.separator)) {
        relativePath = relativePath.substring(1);
      }
      return relativePath;
    }

    private VcsChangeInfo.Type getType(@NotNull String repoPath, @NotNull String typeName, @NotNull String version, @NotNull String prevVersion) throws VcsException {
      if (ADDED_CHANGE_TYPES.contains(typeName)) {
        final File f = myConnection.getCache(myRoot, repoPath, version);
        if (f.isFile()) {
          return VcsChangeInfo.Type.ADDED;
        } else if (f.isDirectory()) {
          return VcsChangeInfo.Type.DIRECTORY_ADDED;
        }
      } else if (CHANGED_CHANGE_TYPES.contains(typeName)) {
        final File f = myConnection.getCache(myRoot, repoPath, version);
        if (f.isFile()) {
          return VcsChangeInfo.Type.CHANGED;
        } else if (f.isDirectory()) {
          return VcsChangeInfo.Type.DIRECTORY_CHANGED;
        }
      } else if (REMOVED_CHANGE_TYPES.contains(typeName)) {
        final File f = myConnection.getCache(myRoot, repoPath, prevVersion);
        if (f.isFile()) {
          return VcsChangeInfo.Type.REMOVED;
        } else if (f.isDirectory()) {
          return VcsChangeInfo.Type.DIRECTORY_REMOVED;
        }
      }
      return VcsChangeInfo.Type.NOT_CHANGED;
    }

    private void renameFolderContent(@NotNull final String path, @NotNull final String oldPath,
                                     @NotNull final String version, @NotNull final String prevVersion,
                                     @NotNull final String actionString,
                                     @NotNull final List<VcsChange> changes) throws VcsException {
      final GeneralCommandLine cl = createListFolderCommandLine(path);

      myConnection.runCommand(cl, new VaultConnection.Handler() {
        private String myCurrentFolder;

        public void startElement (String uri, String localName,
                      String qName, Attributes attributes)
        throws SAXException {
          if ("error".equals(localName)) {
            changes.add(new VcsChange(VcsChangeInfo.Type.ADDED, actionString, getPathFromRepoPath(path), getPathFromRepoPath(path), prevVersion, version));
            changes.add(new VcsChange(VcsChange.Type.REMOVED, actionString, getPathFromRepoPath(oldPath), getPathFromRepoPath(oldPath), prevVersion, version));
            throw VaultConnection.SUCCESS;
          } else if ("folder".equals(localName)) {
            final String newSubfolderPath = attributes.getValue("name");
            final String oldSubfolderPath = newSubfolderPath.replace(path, oldPath);
            changes.add(new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, actionString, getPathFromRepoPath(newSubfolderPath), getPathFromRepoPath(newSubfolderPath), prevVersion, version));
            changes.add(new VcsChange(VcsChange.Type.DIRECTORY_REMOVED, actionString, getPathFromRepoPath(oldSubfolderPath), getPathFromRepoPath(oldSubfolderPath), prevVersion, version));
            myCurrentFolder = newSubfolderPath;
          } else if ("file".equals(localName)) {
            final String newFileName = myCurrentFolder + "/" + attributes.getValue("name");
            final String oldFileName = newFileName.replace(path, oldPath);
            changes.add(new VcsChange(VcsChangeInfo.Type.ADDED, actionString, getPathFromRepoPath(newFileName), getPathFromRepoPath(newFileName), prevVersion, version));
            changes.add(new VcsChange(VcsChange.Type.REMOVED, actionString, getPathFromRepoPath(oldFileName), getPathFromRepoPath(oldFileName), prevVersion, version));
          }
        }
      });
    }

    private GeneralCommandLine createListFolderCommandLine(String path) {
      final GeneralCommandLine cl = new GeneralCommandLine();
      cl.setExePath(myRoot.getProperty("vault.path"));
      cl.addParameter("listfolder");
      cl.addParameter("-server");
      cl.addParameter(myRoot.getProperty("vault.server"));
      cl.addParameter("-user");
      cl.addParameter(myRoot.getProperty("vault.user"));
      cl.addParameter("-password");
      cl.addParameter(myRoot.getProperty("secure:vault.password"));
      cl.addParameter("-repository");
      cl.addParameter(myRoot.getProperty("vault.repo"));

      cl.addParameter(path);
      return cl;
    }

    private void addFolderContent(@NotNull final String path,
                                  @NotNull final String version, @NotNull final String prevVersion,
                                  @NotNull final String actionString,
                                  @NotNull final List<VcsChange> changes) throws VcsException {
      final GeneralCommandLine cl = createListFolderCommandLine(path);

      myConnection.runCommand(cl, new VaultConnection.Handler() {
        private String myCurrentFolder;

        public void startElement (String uri, String localName,
                      String qName, Attributes attributes)
        throws SAXException {
          if ("error".equals(localName)) {
            changes.add(new VcsChange(VcsChangeInfo.Type.ADDED, actionString, getPathFromRepoPath(path), getPathFromRepoPath(path), prevVersion, version));
            throw VaultConnection.SUCCESS;
          } else if ("folder".equals(localName)) {
            final String newSubfolderPath = attributes.getValue("name");
            changes.add(new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, actionString, getPathFromRepoPath(newSubfolderPath), getPathFromRepoPath(newSubfolderPath), prevVersion, version));
            myCurrentFolder = newSubfolderPath;
          } else if ("file".equals(localName)) {
            final String newFileName = myCurrentFolder + "/" + attributes.getValue("name");
            changes.add(new VcsChange(VcsChangeInfo.Type.ADDED, actionString, getPathFromRepoPath(newFileName), getPathFromRepoPath(newFileName), prevVersion, version));
          }
        }
      });
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
