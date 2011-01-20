/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package jetbrains.buildServer.vcs.patches;

import jetbrains.buildServer.vcs.VcsChange;
import jetbrains.buildServer.vcs.VcsChangeInfo;
import jetbrains.buildServer.vcs.VcsException;
import jetbrains.buildServer.vcs.patches.fs.FileSystemException;
import jetbrains.buildServer.vcs.patches.fs.MemoryFileSystem;
import jetbrains.buildServer.vcs.patches.util.Assert;
import jetbrains.buildServer.vcs.patches.util.AssertionFailedException;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * User: vbedrosova
 * Date: 22.12.2009
 * Time: 13:10:20
 */
public class ChangesPatchBuilder {
  public static interface FileContentProvider {

    public abstract File getFile(@NotNull String path, @NotNull String version)
      throws VcsException;
  }


  public ChangesPatchBuilder() {
    myVersions = new HashMap<String, String>();
  }

  public void buildPatch(@NotNull PatchBuilder builder,
                         @NotNull List<VcsChange> changes,
                         @NotNull FileContentProvider provider,
                         boolean strictErrorChecking)
    throws IOException, VcsException {
    LOG.debug("Start building minimal patch for collected changes");

    myStrict = strictErrorChecking;

    final MemoryFileSystem positive = new MemoryFileSystem();
    final MemoryFileSystem negative = new MemoryFileSystem();

    for (final VcsChange change : changes) {
      Assert.isNotNull(change, "Change is null");
      LOG.debug("Vcs change" + change);

      final String path = change.getFileName();
      if (!MemoryFileSystem.checkPath(path))
        throw new VcsException((new StringBuilder()).append("Incorrect path ").append(path).toString());
      final VcsChangeInfo.Type type = change.getType();

      try {
        switch (type)
        {
          case ADDED:
            if (!positive.containsAncestor(path) && negative.containsAncestor(path)) {
              fail((new StringBuilder()).append("Parent directory has been deleted, can't create a file ").append(path).append(" there").toString());
            } else {
              if (negative.containsFile(path)) {
                negative.deleteFile(path);
                positive.writeFile(path);
              } else {
                positive.createFile(path);
              }
              myVersions.put(path, change.getAfterChangeRevisionNumber());
            }
            break;

          case CHANGED:
            if (!positive.containsAncestor(path) && negative.containsAncestor(path))
              fail((new StringBuilder()).append("Parent directory has been deleted, can't modify a file ").append(path).append(" there").toString());
            else if (negative.containsFile(path)) {
              fail((new StringBuilder()).append("Cannot modify a deleted file ").append(path).toString());
            } else {
              if (!positive.containsFile(path))
                positive.writeFile(path);
              myVersions.put(path, change.getAfterChangeRevisionNumber());
            }
            break;

          case REMOVED:
            if (!positive.containsAncestor(path) && negative.containsAncestor(path)) {
              fail((new StringBuilder()).append("Parent directory for ").append(path).append(" has already been deleted").toString());
            } else {
              if (!positive.containsNewFile(path))
                negative.createFile(path);
              if (positive.containsFile(path))
                positive.deleteFile(path);
              myVersions.remove(path);
            }
            break;

          case DIRECTORY_ADDED:
            if (!positive.containsAncestor(path) && negative.containsAncestor(path))
              fail((new StringBuilder()).append("Parent directory has been deleted, can't create a directory ").append(path).append(" there").toString());
            else
              positive.createDirectory(path);
            break;

          case DIRECTORY_REMOVED:
            if (positive.containsDirectory(path)) {
              positive.deleteDirectory(path);
            } else {
              if (positive.containsNode(path)) {
                positive.deleteDirectory(path);
              } else {
                if (negative.containsAncestor(path))
                  fail((new StringBuilder()).append("Parent directory for ").append(path).append(" has already been deleted").toString());
                if (negative.containsDirectory(path))
                  fail((new StringBuilder()).append("Directory ").append(path).append(" has already been deleted").toString());
              }
              if (negative.containsNode(path))
                negative.deleteDirectory(path);
              negative.createDirectory(path);
            }
            break;

          default:
            fail((new StringBuilder()).append("Unexpected VCS change type: ").append(type).toString());
            break;
        }
      }
      catch (FileSystemException e) {
        fail(e);
      }
      catch (AssertionFailedException e) {
        fail(e);
      }      
    }

    final ArrayList<String> newFiles = new ArrayList<String>();
    final ArrayList<String> modifiedFiles = new ArrayList<String>();
    final ArrayList<String> newDirectories = new ArrayList<String>();
    positive.toCollections(newFiles, modifiedFiles, newDirectories);

    final ArrayList<String> deletedFiles = new ArrayList<String>();
    final ArrayList<String> deletedDirectories = new ArrayList<String>();
    negative.toCollections(deletedFiles, deletedFiles, deletedDirectories);

    for (String path : deletedFiles) {
      LOG.debug("Delete file in patch: " + path);
      builder.deleteFile(new File(path), false);
    }
    for (String path : deletedDirectories) {
      LOG.debug("Delete folder in patch: " + path);
      builder.deleteDirectory(new File(path), false);
    }
    for (String path : newDirectories) {
      LOG.debug("Create folder in patch: " + path);
      builder.createDirectory(new File(path));
    }
    for (String path : newFiles) {
      final String version = myVersions.get(path);
      LOG.debug("Create file in patch: " + path + " version: " + version);
      if (version == null)
        throw new VcsException((new StringBuilder()).append("Unexpected error: No version for ").append(path).append(" prepared").toString());
      final File content = provider.getFile(path, version);
      builder.createBinaryFile(new File(path), version, new FileInputStream(content), content.length());
    }
    for (String path : modifiedFiles) {
      final String version = myVersions.get(path);
      LOG.debug("Changed file in patch: " + path + " version: " + version);
      if (version == null)
        throw new VcsException((new StringBuilder()).append("Unexpected error: No version for ").append(path).append(" prepared").toString());
      final File content = provider.getFile(path, version);
      builder.changeOrCreateBinaryFile(new File(path), version, new FileInputStream(content), content.length());
    }    
  }

  private void fail(Exception e)
    throws VcsException {
    String message = (new StringBuilder()).append("Incorrect change set: ").append(e.getMessage()).toString();
    LOG.warn(message, e);
    if (myStrict)
      throw new VcsException(message, e);
  }

  private void fail(String message)
    throws VcsException {
    message = (new StringBuilder()).append("Incorrect change set: ").append(message).toString();
    LOG.warn(message);
    if (myStrict)
      throw new VcsException(message);
  }

  private static final Logger LOG = Logger.getLogger(ChangesPatchBuilder.class);
  private Map<String, String> myVersions;
  private boolean myStrict;

}
