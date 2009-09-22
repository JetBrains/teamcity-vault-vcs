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
import java.util.List;
import java.util.Map;

/**
 * User: vbedrosova
 * Date: 03.07.2009
 * Time: 13:33:39
 */
public final class VaultPatchBuilder implements IncludeRulePatchBuilder {
  private static final Logger LOG = Logger.getLogger(VaultPatchBuilder.class);

  private final VaultConnection myConnection;
  private final VcsRoot myRoot;
  private final String myFromVersion;
  private final String myToVersion;

  public VaultPatchBuilder(@NotNull VaultConnection connection,
                           @NotNull VcsRoot root,
                           @Nullable String fromVersion,
                           @NotNull String toVersion) {

    myConnection = connection;
    myRoot = root;
    myFromVersion = fromVersion;
    myToVersion = toVersion;
  }

  public void buildPatch(@NotNull PatchBuilder builder, @NotNull IncludeRule includeRule) throws IOException, VcsException {
    LOG.debug("Start building patch");
    if (myFromVersion == null) {
      LOG.debug("Perform clean patch");
      VcsSupportUtil.exportFilesFromDisk(builder, myConnection.getObject(myRoot, "", false, myToVersion));
    } else {
      LOG.debug("Perform incremental patch");
      final Map<VaultChangeCollector.ModificationInfo, List<VcsChange>> modifications = new VaultChangeCollector(myConnection, myRoot, myFromVersion, myToVersion).collectModifications(includeRule);
      for (final VaultChangeCollector.ModificationInfo m : modifications.keySet()) {
        for (final VcsChange c : modifications.get(m)) {
          final File relativeFile = new File(c.getRelativeFileName());
          File f;
          switch (c.getType()) {
            case CHANGED:
              f = myConnection.getObject(myRoot, c.getRelativeFileName(), true, c.getAfterChangeRevisionNumber());
              builder.changeOrCreateBinaryFile(relativeFile, null, new FileInputStream(f), f.length());
              break;
            case DIRECTORY_CHANGED:
              builder.createDirectory(relativeFile);
              break;
            case ADDED:
              f = myConnection.getObject(myRoot, c.getRelativeFileName(), true, c.getAfterChangeRevisionNumber());
              builder.createBinaryFile(relativeFile, null, new FileInputStream(f), f.length());
              break;
            case DIRECTORY_ADDED:
              builder.createDirectory(relativeFile);
              break;
            case REMOVED:
              builder.deleteFile(relativeFile, false);
              break;
            case DIRECTORY_REMOVED:
              builder.deleteDirectory(relativeFile, false);
              break;
            case NOT_CHANGED:
          }
        }
      }
    }
    LOG.debug("Finish building patch");
  }

  public void dispose() throws VcsException {
  }
}
