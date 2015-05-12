/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import java.io.IOException;
import java.util.List;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.vcs.*;
import jetbrains.buildServer.vcs.patches.ChangesPatchBuilder;
import jetbrains.buildServer.vcs.patches.PatchBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 03.07.2009
 * Time: 13:33:39
 */
public final class VaultPatchBuilder {
  @NotNull private final VaultConnection myConnection;
  @NotNull private final PatchBuilder myPatchBuilder;
  @NotNull private final String myTargetPath;

  public VaultPatchBuilder(@NotNull final VaultConnection connection,
                           @NotNull final PatchBuilder patchBuilder,
                           @Nullable final String targetPath) {
    myConnection = connection;
    myPatchBuilder = patchBuilder;
    myTargetPath = StringUtil.notNullize(targetPath);
  }

  public void buildCleanPatch(@NotNull final String toVersion) throws VcsException, IOException {
    VcsSupportUtil.exportFilesFromDisk(myPatchBuilder, myConnection.getExistingObject(myTargetPath, toVersion));
  }

  public void buildIncrementalPatch(@NotNull final String fromVersion, @NotNull final String toVersion) throws VcsException, IOException {
    final List<ChangeInfo> changes = new VaultChangeCollector(myConnection, fromVersion, toVersion, myTargetPath).collectChanges();

    new ChangesPatchBuilder().buildPatch(myPatchBuilder, VaultUtil.toVcsChanges(changes), new ChangesPatchBuilder.FileContentProvider() {
      public File getFile(@NotNull String path, @NotNull String version) throws VcsException {
        return myConnection.getExistingObject(VaultUtil.getFullPath(path, myTargetPath), version);
      }
    }, false);
  }
}
