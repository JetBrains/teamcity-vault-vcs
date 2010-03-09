/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
import jetbrains.buildServer.vcs.patches.ChangesPatchBuilder;
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

  public void dispose() throws VcsException {
  }

  public void buildPatch(@NotNull PatchBuilder builder, final @NotNull IncludeRule includeRule) throws IOException, VcsException {
    VaultUtil.checkIncludeRule(myRoot, includeRule);

    logStartBuildingPatch(includeRule);

    if (myFromVersion == null) {
      buildCleanPatch(builder, includeRule);
    } else {
      buildIncrementalPatch(builder, includeRule);
    }

    logFinishBuildingPatch(includeRule);
  }

  private void buildCleanPatch(final PatchBuilder builder, final IncludeRule includeRule) throws VcsException, IOException {
    logBuildCleanPatch(includeRule);

    VaultConnection.doInConnection(myRoot.getProperties(), new VaultConnection.InConnectionProcessor() {
      public void process() throws Throwable {
        final File root = VaultConnection.getObject(includeRule.getFrom(), myToVersion);
        VcsSupportUtil.exportFilesFromDisk(builder, root);
      }
    });
  }

  private void buildIncrementalPatch(final PatchBuilder builder, final IncludeRule includeRule) throws VcsException, IOException {
    logBuildIncrementalPatch(includeRule);

    final Map<VaultChangeCollector.ModificationInfo, List<VcsChange>> modifications = new VaultChangeCollector(myRoot, myFromVersion, myToVersion).collectModifications(includeRule);
    final List<VcsChange> changes = new LinkedList<VcsChange>();

    for (final VaultChangeCollector.ModificationInfo i : modifications.keySet()) {
      logModificationInfo(i);

      final List<VcsChange> l = modifications.get(i);
      for (final VcsChange c : l) {
        LOG.debug("Vcs change: " + c);
      }
      changes.addAll(l);
    }

    VaultConnection.doInConnection(myRoot.getProperties(), new VaultConnection.InConnectionProcessor() {
      public void process() throws Throwable {
        new ChangesPatchBuilder().buildPatch(builder, changes, new ChangesPatchBuilder.FileContentProvider() {
          public File getFile(@NotNull String s, @NotNull String s1) throws VcsException {
            return VaultConnection.getObject(getPathWithIncludeRule(includeRule, s), s1);
          }
        }, false);
      }
    });
  }

  private String getPathWithIncludeRule(@NotNull IncludeRule includeRule, @NotNull String path) {
    return "".equals(includeRule.getFrom()) ? path : includeRule.getFrom() + "/" + path;
  }

  private void logFinishBuildingPatch(IncludeRule includeRule) {
    LOG.debug("Finish building patch for root " + myRoot + " for rule " + includeRule.toDescriptiveString()
      + " from version " + myFromVersion + " to version " + myToVersion);
  }

  private void logStartBuildingPatch(IncludeRule includeRule) {
    LOG.debug("Start building patch for root " + myRoot + " for rule " + includeRule.toDescriptiveString()
      + " from version " + myFromVersion + " to version " + myToVersion);
  }

  private void logBuildCleanPatch(IncludeRule includeRule) {
    LOG.debug("Perform clean patch for root " + myRoot + " for rule " + includeRule.toDescriptiveString()
    + " to version " + myToVersion);
  }  

  private void logBuildIncrementalPatch(IncludeRule includeRule) {
    LOG.debug("Perform incremental patch for root " + myRoot + " for rule " + includeRule.toDescriptiveString()
    + " from version " + myFromVersion + " to version " + myToVersion + " by collecting changes");
  }  

  private void logModificationInfo(VaultChangeCollector.ModificationInfo i) {
    LOG.debug("Modification info: version=" + i.getVersion() + ", date=" + i.getDate() +
    ", user=" + i.getUser() + ", comment=" + i.getComment());
  }
}
