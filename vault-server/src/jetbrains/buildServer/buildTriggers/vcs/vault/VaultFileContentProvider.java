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

import java.io.IOException;

/**
 * User: vbedrosova
 * Date: 03.07.2009
 * Time: 12:19:39
 */
public final class VaultFileContentProvider implements VcsFileContentProvider {
  private static final Logger LOG = Logger.getLogger(VaultFileContentProvider.class);

  private final VaultConnection myConnection;
//  private File myTmpDir;

  public VaultFileContentProvider(@NotNull VaultConnection connection) {
//    try {
//      myTmpDir = FileUtil.createTempDirectory("vaultFileContentProvider", "");
//    } catch (IOException e) {
//      LOG.debug("Unable to create temp directory");
//    }
    myConnection = connection;
  }

  @NotNull
  public byte[] getContent(@NotNull VcsModification vcsModification, @NotNull VcsChangeInfo change, @NotNull VcsChangeInfo.ContentType contentType, @NotNull VcsRoot vcsRoot) throws VcsException {
//    final String actionString = change.getChangeTypeName();
//    if ((actionString != null) && actionString.startsWith("Renamed from")) {
//      final String name = change.getRelativeFileName().substring(change.getRelativeFileName().lastIndexOf(File.separator) + 1);
//      return getContent(change.getRelativeFileName().replace(name, VaultUtil.getRenamedToName(actionString)), vcsRoot,
//      contentType == VcsChangeInfo.ContentType.BEFORE_CHANGE ? change.getBeforeChangeRevisionNumber()
//                                                             : change.getAfterChangeRevisionNumber());
//    }
    return getContent(change.getRelativeFileName(), vcsRoot,
      contentType == VcsChangeInfo.ContentType.BEFORE_CHANGE ? change.getBeforeChangeRevisionNumber()
                                                             : change.getAfterChangeRevisionNumber());
  }

  @NotNull
  public byte[] getContent(@NotNull String filePath, @NotNull VcsRoot root, @NotNull String version) throws VcsException {
    try {
//      return FileUtil.loadFileBytes(getFile(filePath, root, version));
      return FileUtil.loadFileBytes(myConnection.getCache(root, filePath, version));
    } catch (IOException e) {
      throw new VcsException(e);
    }
  }

//  public File getFile(@NotNull String filePath, @NotNull VcsRoot root, final @NotNull String version) throws VcsException {
//    final File filePathFile = new File(filePath);
//    final String parentPath = filePathFile.getParent();
//    final File parentDir = new File(myTmpDir.getAbsolutePath() + File.separator + version.replace(":", "."), parentPath == null ? "" : parentPath);
//    final File file = new File(parentDir.getAbsolutePath(), filePathFile.getName());
//
//    LOG.debug("Getting file content for " + filePath + " at root " + root + " at version " + version);
//    FileUtil.delete(myTmpDir);
//    if (!myTmpDir.mkdirs()) {
//      LOG.debug("Unable to create tmp directory " + myTmpDir.getAbsolutePath());
//    }
//
//    GeneralCommandLine cl = new GeneralCommandLine();
//    cl.setExePath(root.getProperty("vault.path"));
//    cl.addParameter("history");
//    cl.addParameter("-server");
//    cl.addParameter(root.getProperty("vault.server"));
//    cl.addParameter("-user");
//    cl.addParameter(root.getProperty("vault.user"));
//    cl.addParameter("-password");
//    cl.addParameter(root.getProperty("secure:vault.password"));
//    cl.addParameter("-repository");
//    cl.addParameter(root.getProperty("vault.repo"));
//
//    final String repoPath = "$" + ((filePath.length() == 0) ? "" : "/") + filePath.replace(File.separator, "/");
//    cl.addParameter(repoPath);
//
//    final FileVerionHandler handler = new FileVerionHandler(filePath, repoPath, version);
//    VaultConnection.getConnection().runCommand(cl, handler);
//
//    if (handler.getFileVerion() == null) {
//      return getFileFromParent(filePath, root, version);
//    }
//
//    VaultConnection.getConnection().setWorkingFolder(root, myTmpDir.getAbsolutePath() + File.separator + version.replace(":", "."));
//    cl = new GeneralCommandLine();
//    cl.setExePath(root.getProperty("vault.path"));
//    cl.addParameter("getversion");
//    cl.addParameter("-server");
//    cl.addParameter(root.getProperty("vault.server"));
//    cl.addParameter("-user");
//    cl.addParameter(root.getProperty("vault.user"));
//    cl.addParameter("-password");
//    cl.addParameter(root.getProperty("secure:vault.password"));
//    cl.addParameter("-repository");
//    cl.addParameter(root.getProperty("vault.repo"));
//    cl.addParameter("-setfiletime");
//    cl.addParameter("modification");
//
//    cl.addParameter(handler.getFileVerion());
//
//    cl.addParameter(repoPath);
//
//    VaultConnection.getConnection().runCommand(cl, null);
//
//    if (!file.exists() && (filePath.length() != 0)) {
//      //return getFileFromParent(filePath, root, version);
//      LOG.debug("File " + file + "with version " + version + " should exist in repo");
//    }
//    return file;
//  }
//
//  public File getFileFromParent(@NotNull String filePath, @NotNull VcsRoot root, @NotNull String version) throws VcsException {
//    final File filePathFile = new File(filePath);
//    final String parentPath = filePathFile.getParent();
//    return new File(getFile(parentPath == null ? "" : parentPath, root, version), filePathFile.getName());
//  }
//
//  private final class FileVerionHandler extends VaultConnection.Handler {
//    private final String myPath;
//    private final String myRepoPath;
//    private final String myVersion;
//    private String myFileVerion = null;
//
//    public FileVerionHandler(@NotNull String path, @NotNull String repoPath, @NotNull String version) {
//      myPath = path;
//      myRepoPath = repoPath;
//      myVersion = version;
//    }
//
//    public void startElement (String uri, String localName,
//                  String qName, Attributes attributes)
//    throws SAXException
//    {
//      if ("item".equals(localName)) {
//        final String name = attributes.getValue("name");
//        if (myVersion.equals(attributes.getValue("date")) && (myRepoPath.equals(name) || myPath.equals(name))) {
//          myFileVerion = attributes.getValue("version");
//          throw VaultConnection.SUCCESS;
//        }
//      }
//    }
//
//    public String getFileVerion() {
//      return myFileVerion;
//    }
//  }
}
