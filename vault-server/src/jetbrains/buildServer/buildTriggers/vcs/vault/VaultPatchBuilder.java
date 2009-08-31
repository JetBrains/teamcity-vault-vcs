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
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.buildTriggers.vcs.vault.process.VaultProcessExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.*;

import com.intellij.execution.configurations.GeneralCommandLine;

/**
 * User: vbedrosova
 * Date: 03.07.2009
 * Time: 13:33:39
 */
public class VaultPatchBuilder implements IncludeRulePatchBuilder {
  private static final Logger LOG = Logger.getLogger(VaultPatchBuilder.class);  

  private final VcsRoot myRoot;
  private final String myFromVersion;
  private final String myToVersion;

  private File myTmpDir;

  public VaultPatchBuilder(@NotNull VcsRoot root, @Nullable String fromVersion, @NotNull String toVersion) {
    myRoot = root;
    myFromVersion = fromVersion;
    myToVersion = toVersion;
  }

  public void buildPatch(@NotNull PatchBuilder builder, @NotNull IncludeRule includeRule) throws IOException, VcsException {
    LOG.debug("Start building patch");
    myTmpDir = FileUtil.createTempDirectory("vault", "");
    final VaultFileContentProvider provider = new VaultFileContentProvider();
    if (myFromVersion == null) {
      LOG.debug("Perform clean patch");

      final GeneralCommandLine cl = new GeneralCommandLine();
      cl.setExePath(myRoot.getProperty("vault.path"));
      cl.addParameter("getversion");
      cl.addParameter("-server");
      cl.addParameter(myRoot.getProperty("vault.server"));
      cl.addParameter("-user");
      cl.addParameter(myRoot.getProperty("vault.user"));
      cl.addParameter("-password");
      cl.addParameter(myRoot.getProperty("secure:vault.password"));
      cl.addParameter("-repository");
      cl.addParameter(myRoot.getProperty("vault.repo"));
      cl.addParameter("-setfiletime");
      cl.addParameter("modification");

      cl.addParameter(VaultConnection.getConnection().getVersionByDate(myRoot, myToVersion));
      cl.addParameter("$");
      cl.addParameter(myTmpDir.getAbsolutePath());

      VaultConnection.getConnection().runCommand(cl, null);

      VcsSupportUtil.exportFilesFromDisk(builder, myTmpDir);
      System.out.println(myTmpDir.getAbsolutePath());
    } else {
      LOG.debug("Perform incremental patch");

//      final List<VcsChange> changes = new ArrayList<VcsChange>();
      final Map<VaultChangeCollector.ModificationInfo, List<VcsChange>> modifications = new VaultChangeCollector(myRoot, myFromVersion, myToVersion).collectModifications(includeRule);
      for (final VaultChangeCollector.ModificationInfo m : modifications.keySet()) {
//        changes.addAll(modifications.get(m));
        for (final VcsChange c : modifications.get(m)) {
          final File f = provider.getFile(c.getRelativeFileName(), myRoot, c.getAfterChangeRevisionNumber());
          final File relativeFile = new File(c.getRelativeFileName());
          switch (c.getType()) {
            case CHANGED:
              builder.changeOrCreateBinaryFile(relativeFile, null, new FileInputStream(f), f.length());
              break;
            case ADDED:
              if (f.isFile()) {
                builder.createBinaryFile(relativeFile, null, new FileInputStream(f), f.length());
              } else if (f.isDirectory()) {
                builder.createDirectory(relativeFile);
              }
              break;
            case REMOVED:
              builder.deleteFile(relativeFile, false);
              break;
            case NOT_CHANGED:
//            case DIRECTORY_CHANGED:
//            case DIRECTORY_ADDED:
//            case DIRECTORY_REMOVED:
          }
        }
      }
    }
    LOG.debug("Finish building patch");
  }

  public void dispose() throws VcsException {
//    if (myTmpDir != null) {
//      FileUtil.delete(myTmpDir);
//      myTmpDir = null;
//    }
  }
}
