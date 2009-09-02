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
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

import com.intellij.execution.configurations.GeneralCommandLine;

/**
 * User: vbedrosova
 * Date: 03.07.2009
 * Time: 12:19:39
 */
public final class VaultFileContentProvider implements VcsFileContentProvider {
  private static final Logger LOG = Logger.getLogger(VaultFileContentProvider.class);

  private File myTmpDir;

  public VaultFileContentProvider() {
    try {
      myTmpDir = FileUtil.createTempDirectory("vaultFileContentProvider", "");
    } catch (IOException e) {
      LOG.debug("Unable to create temp directory");
    }
  }

  @NotNull
  public byte[] getContent(@NotNull VcsModification vcsModification, @NotNull VcsChangeInfo change, @NotNull VcsChangeInfo.ContentType contentType, @NotNull VcsRoot vcsRoot) throws VcsException {
    return getContent(change.getRelativeFileName(), vcsRoot,
      contentType == VcsChangeInfo.ContentType.BEFORE_CHANGE ? change.getBeforeChangeRevisionNumber()
                                                             : change.getAfterChangeRevisionNumber());    
  }

  @NotNull
  public byte[] getContent(@NotNull String filePath, @NotNull VcsRoot root, @NotNull String version) throws VcsException {
    try {
      return FileUtil.loadFileBytes(getFile(filePath, root, version));
    } catch (IOException e) {
      throw new VcsException(e);
    }
  }

  public File getFile(@NotNull String filePath, @NotNull VcsRoot root, @NotNull String version) throws VcsException {
    final File filePathFile = new File(filePath);
    final String parentPath = filePathFile.getParent();
    final File parentDir = new File(myTmpDir.getAbsolutePath() + File.separator + version, parentPath == null ? "" : parentPath);
    final File file = new File(parentDir.getAbsolutePath(), filePathFile.getName());
//    final String separatorStartedFilePath = File.separator + filePath;
//    final String separatorStartedParentPath = separatorStartedFilePath.substring(0, separatorStartedFilePath.lastIndexOf(File.separator));
//    final File parentDir = new File(myTmpDir.getAbsolutePath() + File.separator + version + separatorStartedParentPath);
//    final File file = new File(parentDir, filePath.substring(filePath.lastIndexOf(File.separator) + 1));

    LOG.debug("Getting file content for " + filePath + " at root " + root + " at version " + version);
    FileUtil.delete(myTmpDir);
    if (!myTmpDir.mkdirs()) {
      LOG.debug("Unable to create tmp directory " + myTmpDir.getAbsolutePath());
    }

    VaultConnection.getConnection().setWorkingFolder(root, myTmpDir.getAbsolutePath() + File.separator + version);
    final GeneralCommandLine cl = new GeneralCommandLine();
    cl.setExePath(root.getProperty("vault.path"));
    cl.addParameter("getversion");
    cl.addParameter("-server");
    cl.addParameter(root.getProperty("vault.server"));
    cl.addParameter("-user");
    cl.addParameter(root.getProperty("vault.user"));
    cl.addParameter("-password");
    cl.addParameter(root.getProperty("secure:vault.password"));
    cl.addParameter("-repository");
    cl.addParameter(root.getProperty("vault.repo"));
    cl.addParameter("-setfiletime");
    cl.addParameter("modification");

    cl.addParameter(version);

    cl.addParameter("$" + ((filePath.length() == 0) ? "" : "/") + filePath.replace(File.separator, "/"));

//    cl.addParameter(parentDir.getAbsolutePath());

    VaultConnection.getConnection().runCommand(cl, null);

    if (!file.exists() && (filePath.length() != 0)) {
      return getFileFromParent(filePath, root, version);
    }
    return file;
  }

  public File getFileFromParent(@NotNull String filePath, @NotNull VcsRoot root, @NotNull String version) throws VcsException {
    final File filePathFile = new File(filePath);
    final String parentPath = filePathFile.getParent(); 
    final File rootFile = getFile(parentPath == null ? "" : parentPath, root, version);
    return new File(rootFile, filePathFile.getName());
  }
}
