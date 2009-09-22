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

import static jetbrains.buildServer.buildTriggers.vcs.vault.VaultUtil.*;
import static jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection.getRepoPathFromPath;
import jetbrains.buildServer.vcs.*;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.File;
import java.util.*;

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
    LOG.debug("Start collecting changes for root " + myRoot + " for rule " + includeRule.toDescriptiveString());

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
    final String repoPath = getRepoPathFromPath(includeRule.getFrom());
    final HistoryHandler handler = new HistoryHandler(repoPath, includeRule.getTo());
    myConnection.runHistoryCommand(repoPath, myRoot.getProperties(),
                                   getNextVersion(repoPath, myFromVersion),
                                   getNextVersion(repoPath, myCurrentVersion), handler);
    return handler.getModifications();
  }

  private final class HistoryHandler extends VaultConnection.Handler {
    private final String myRepoParent;
    private final String myDestPath;
    private final Map<ModificationInfo, List<VcsChange>> myModifications
      = new HashMap<ModificationInfo, List<VcsChange>>();

    public HistoryHandler(@NotNull String repoPath, @NotNull String destPath) {
      myRepoParent = getParent(repoPath);
      myDestPath = destPath;
    }

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
        String repoPath = ((myRepoParent.length() == 0) ? "" : (myRepoParent + "/")) + attributes.getValue("name");
        if ("Deleted".equals(typeName)) {
          repoPath += getDeletedName(actionString);
        } else if ("MovedTo".equals(typeName)) {
          repoPath = getMovedToName(actionString);
        } else if ("MovedFrom".equals(typeName)) {
          repoPath = getMovedFromName(actionString);
        } else if ("SharedTo".equals(typeName)) {
          repoPath = getSharedToName(actionString);
        }
        String relativePath = getPathFromRepoPath(repoPath);
        if (relativePath.length() == 0) {
          return;
        }
        final String name = getName(repoPath);
        try {
          final String prevVersion = getPreviousVersion((myRepoParent.length() == 0) ? VaultConnection.ROOT : myRepoParent, version);
          if ("Renamed".equals(typeName)) {
            renameFolderContent(repoPath, repoPath.replace(name, getRenamedFromName(actionString)), version, prevVersion, actionString, changes);
          } else if ("SharedTo".equals(typeName)) {
            addFolderContent(repoPath, version, prevVersion, actionString, changes);
          } else {
            final VcsChangeInfo.Type type = getType(repoPath, typeName, prevVersion);
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

    private VcsChangeInfo.Type getType(@NotNull String repoPath, @NotNull String typeName, @NotNull String prevVersion) throws VcsException {
      final Type type = getObjectType(repoPath, prevVersion);
      if (ADDED_CHANGE_TYPES.contains(typeName)) {
        if (type == Type.FILE) {
          return VcsChangeInfo.Type.ADDED;
        } else if (type == Type.FOLDER) {
          return VcsChangeInfo.Type.DIRECTORY_ADDED;

        }
      } else if (CHANGED_CHANGE_TYPES.contains(typeName)) {
        if (type == Type.FILE) {
          return VcsChangeInfo.Type.CHANGED;
        } else if (type == Type.FOLDER) {
          return VcsChangeInfo.Type.DIRECTORY_CHANGED;
        }
      } else if (REMOVED_CHANGE_TYPES.contains(typeName)) {
        if (type == Type.FILE) {
          return VcsChangeInfo.Type.REMOVED;
        } else if (type == Type.FOLDER) {
          return VcsChangeInfo.Type.DIRECTORY_REMOVED;
        }
      }
      throw new VcsException("Couldn't get object type(file/folder) for repo object: " + repoPath);
    }

    private Type getObjectType(@NotNull String repoPath, @NotNull String prevVersion) throws VcsException {
      TypeHandler handler = new TypeHandler(repoPath);
      myConnection.runListFolderCommand(getParent(repoPath), myRoot.getProperties(), true, handler);
      final Type type = handler.getResult();
      if (!(type == Type.UNKNOWN)) {
        return type;
      }
      return getObjectTypeByGettingContent(repoPath, prevVersion);
    }

    private String getParent(@NotNull String repoPath) {
      return VaultConnection.ROOT.equals(repoPath) ? "" : repoPath.substring(0, repoPath.lastIndexOf("/"));     
    }

    private String getName(@NotNull String repoPath) {
      return repoPath.substring(repoPath.lastIndexOf("/") + 1);      
    }

    private class TypeHandler extends VaultConnection.Handler {
      private final String myRepoPath;
      private String myCurrentFolder;
      private Type myResult;

      public TypeHandler(@NotNull String repoPath) {
        myCurrentFolder = VaultConnection.ROOT;
        myRepoPath = repoPath;
        myResult = Type.UNKNOWN;
      }

      public void startElement (String uri, String localName,
                    String qName, Attributes attributes)
      throws SAXException
      {
        if ("folder".equals(localName)) {
          myCurrentFolder = attributes.getValue("name");
          if (myRepoPath.equals(myCurrentFolder)) {
            myResult = Type.FOLDER;
            throw VaultConnection.SUCCESS;
          }
        } else if ("file".equals(localName)) {
          if (myRepoPath.equals(myCurrentFolder + "/" + attributes.getValue("name"))) {
            myResult = Type.FILE;
            throw VaultConnection.SUCCESS;
          }
        }
      }

      public Type getResult() {
        return myResult;
      }
    }

    private Type getObjectTypeByGettingContent(@NotNull String repoPath, @NotNull String prevVersion) throws VcsException {
      final File object = new File(myConnection.getObject(myRoot, getParent(repoPath), true, prevVersion), getName(repoPath));
      if (object.isFile()) {
        return Type.FILE;       
      } else if (object.isDirectory()) {
        return Type.FOLDER;
      } else {
        return Type.UNKNOWN;        
      }
    }

    private void renameFolderContent(@NotNull final String path, @NotNull final String oldPath,
                                     @NotNull final String version, @NotNull final String prevVersion,
                                     @NotNull final String actionString,
                                     @NotNull final List<VcsChange> changes) throws VcsException {
      myConnection.runListFolderCommand(path, myRoot.getProperties(), false, new VaultConnection.Handler() {
        private String myCurrentFolder;

        public void startElement(String uri, String localName,
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

    private void addFolderContent(@NotNull final String path,
                                  @NotNull final String version, @NotNull final String prevVersion,
                                  @NotNull final String actionString,
                                  @NotNull final List<VcsChange> changes) throws VcsException {
      myConnection.runListFolderCommand(path, myRoot.getProperties(), false, new VaultConnection.Handler() {
        private String myCurrentFolder;

        public void startElement(String uri, String localName,
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

  private static enum Type {
    FILE,
    FOLDER,
    UNKNOWN
  }  

  private String getPreviousVersion(@NotNull String repoPath, @NotNull String version) throws VcsException {
    String defaultResult = VaultUtil.getDateString(new Date(VaultUtil.getDate(version).getTime() - 1000)); // default result
    return getVersion(repoPath, new PreviousVersionHandler(version, defaultResult));
  }

  private String getNextVersion(@NotNull String repoPath, @NotNull String version) throws VcsException {
    String defaultResult = VaultUtil.getDateString(new Date(VaultUtil.getDate(version).getTime() + 1000)); // default result
    return getVersion(repoPath, new NextVersionHandler(version, defaultResult));
  }

  private String getVersion(@NotNull String repoPath, @NotNull VersionHandler handler) throws VcsException {
    myConnection.runHistoryCommand(repoPath, myRoot.getProperties(), null, null, handler);
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
