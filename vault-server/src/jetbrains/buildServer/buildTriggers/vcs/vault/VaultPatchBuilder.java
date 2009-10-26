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
import jetbrains.buildServer.vcs.patches.PatchBuilder;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * User: vbedrosova
 * Date: 03.07.2009
 * Time: 13:33:39
 */
public final class VaultPatchBuilder implements IncludeRulePatchBuilder {
  private static final Logger LOG = Logger.getLogger(VaultPatchBuilder.class);

  private final VcsRoot myRoot;
  private final String myFromVersion;
  private final String myToVersion;

  public VaultPatchBuilder(@NotNull VcsRoot root,
                           @Nullable String fromVersion,
                           @NotNull String toVersion) {

    myRoot = root;
    myFromVersion = fromVersion;
    myToVersion = toVersion;
  }

  public void buildPatch(@NotNull PatchBuilder builder, @NotNull IncludeRule includeRule) throws IOException, VcsException {
    LOG.debug("Start building patch for root " + myRoot + " for rule " + includeRule.toDescriptiveString()
      + " from version " + myFromVersion + " to version " + myToVersion);
    if (myFromVersion == null) {
      LOG.debug("Perform clean patch for root " + myRoot + " for rule " + includeRule.toDescriptiveString()
        + " to version " + myToVersion);
      VaultConnection1.connect(new VaultConnectionParameters(myRoot));
      VcsSupportUtil.exportFilesFromDisk(builder, VaultConnection1.getObject(includeRule.getFrom(), myToVersion));
      VaultConnection1.disconnect();
    } else {
      LOG.debug("Perform incremental patch for root " + myRoot + " for rule " + includeRule.toDescriptiveString()
        + " from version " + myFromVersion + " to version " + myToVersion + " by collecting changes");
      final Map<VaultChangeCollector.ModificationInfo, List<VcsChange>> modifications = new VaultChangeCollector(myRoot, myFromVersion, myToVersion).collectModifications(includeRule);
      patch(modifications, builder);
    }
    LOG.debug("Finish building patch for root " + myRoot + " for rule " + includeRule.toDescriptiveString()
      + " from version " + myFromVersion + " to version " + myToVersion);
  }

  public void dispose() throws VcsException {
  }

  private void patch(@NotNull Map<VaultChangeCollector.ModificationInfo, List<VcsChange>> modifications,
                     @NotNull PatchBuilder builder) throws IOException, VcsException {
    final Set<File> deletedFiles = new LinkedHashSet<File>();
    final Set<File> deletedDirs = new LinkedHashSet<File>();
    final Set<File> addedDirs = new LinkedHashSet<File>();
    final Map<File, String> addedFiles = new LinkedHashMap<File, String>();
    final Map<File, String> modifiedFiles = new LinkedHashMap<File, String>();

    for (final VaultChangeCollector.ModificationInfo m : modifications.keySet()) {
      for (final VcsChange c : modifications.get(m)) {
        final File f = new File(c.getRelativeFileName());
        final String version = c.getAfterChangeRevisionNumber();
        final File addedAncestor = containsAncestor(addedDirs, f);
        final File deletedAncestor = containsAncestor(deletedDirs, f);

        switch (c.getType()) {
          case ADDED:
            if ((addedAncestor == null) && (deletedAncestor != null)) {
              throw new VcsException("Incorrect change set: the parent directory " + deletedAncestor + " has been deleted, " +
                "can't create a file " + f + " there");
            }
            if (addedFiles.containsKey(f) || modifiedFiles.containsKey(f)) {
              throw new VcsException("Incorrect change set: file " + f.getPath() + " has already been added, can't add a file again");
            }
            if (deletedFiles.contains(f)) {
              deletedFiles.remove(f);
              modifiedFiles.put(f, version);
            } else {
              addedFiles.put(f, version);
            }
            break;
          case DIRECTORY_ADDED:
            if ((addedAncestor == null) && (deletedAncestor != null)) {
              throw new VcsException("Incorrect change set: the parent directory " + deletedAncestor + " has been deleted, " +
                "can't create a directory " + f + " there");
            }
            if (addedDirs.contains(f)) {
              throw new VcsException("Incorrect change set: directory " + f.getPath() + " has already been added, can't add a directory again");
            }
            if (deletedDirs.contains(f)) {
              deletedDirs.remove(f);
            } else {
              addedDirs.add(f);
            }
            break;
          case REMOVED:
            if ((addedAncestor == null) && (deletedAncestor != null)) {
              throw new VcsException("Incorrect change set: the parent directory " + deletedAncestor + " has been deleted, " +
                "can't deleteFile it's child file " + f);
            }
            if (deletedFiles.contains(f)) {
              throw new VcsException("Incorrect change set: file " + f.getPath() + " has already been deleted, can't delete a file again");
            }
            if (addedFiles.containsKey(f)) {
              addedFiles.remove(f);
            } else {
              deletedFiles.add(f);
            }
            modifiedFiles.remove(f);
            break;
          case DIRECTORY_REMOVED:
            if ((addedAncestor == null) && (deletedAncestor != null)) {
              throw new VcsException("Incorrect change set: the parent directory " + deletedAncestor + " has been deleted, " +
                "can't deleteFile it's child directory " + f);
            }
            if (deletedDirs.contains(f)) {
              throw new VcsException("Incorrect change set: directory " + f.getPath() + " has already been deleted, can't delete a directory again");
            }
            if (addedDirs.contains(f)) {
              addedDirs.remove(f);
            } else {
              deletedDirs.add(f);
            }
            deleteChildren(addedFiles, f);
            deleteChildren(modifiedFiles, f);
            deleteChildren(deletedFiles, f);
            deleteChildren(addedDirs, f);
            deleteChildren(deletedDirs, f);
            break;
          case CHANGED:
            if ((addedAncestor == null) && (deletedAncestor != null)) {
              throw new VcsException("Incorrect change set: the parent directory " + deletedAncestor + " has been deleted, " +
                "can't modify it's child file " + f);
            }
            if (deletedFiles.contains(f)) {
              throw new VcsException("Incorrect change set: file " + f.getPath() + " has already been deleted, can't modify a deleted file");
            }
            modifiedFiles.put(f, version);
            break;
          default:
            throw new VcsException("Unable to add object " + f.getPath() + " to patch - uncknown type " + c.getType());
        }
      }
    }

    for (final File f : deletedFiles) {
      builder.deleteFile(f, false);
    }
    for (final File d : deletedDirs) {
      builder.deleteDirectory(d, false);
    }
    for (final File d : addedDirs) {
      builder.createDirectory(d);
    }
    for (final File f : addedFiles.keySet()) {
      final File content = VaultConnection1.getObject(f.getPath(), addedFiles.get(f));
      builder.createBinaryFile(f, null, new FileInputStream(content), content.length());
    }
    for (final File f : modifiedFiles.keySet()) {
      final File content = VaultConnection1.getObject(f.getPath(), modifiedFiles.get(f));
      builder.changeOrCreateBinaryFile(f, null, new FileInputStream(content), content.length());
    }
  }

  private File containsAncestor(@NotNull Set<File> files, @NotNull File f) {
    File parent = f.getParentFile();
    while (parent != null) {
      if (files.contains(parent)) {
        return parent;
      }
      parent = parent.getParentFile();
    }
    return null;
  }

  private void deleteChildren(@NotNull Set<File> files, @NotNull File f) {
    files.removeAll(collectChildren(files, f));
  }

  private void deleteChildren(@NotNull Map<File, String> files, @NotNull File f) {
    for (final File c : collectChildren(files.keySet(), f)) {
      files.remove(c);
    }
  }

  private Set<File> collectChildren(@NotNull Set<File> files, @NotNull File f) {
    final Set<File> toDelete = new HashSet<File>();
    for (final File c : files) {
      if (c.getPath().startsWith(f.getPath()) && !c.getPath().equals(f.getPath())) {
        toDelete.add(c);
      }
    }
    return toDelete;
  }
}
