/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.vcs.*;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 03.07.2009
 * Time: 12:19:39
 */
public final class VaultFileContentProvider implements VcsFileContentProvider {
  @NotNull
  public byte[] getContent(@NotNull VcsModification vcsModification, @NotNull VcsChangeInfo change, @NotNull VcsChangeInfo.ContentType contentType, @NotNull VcsRoot vcsRoot) throws VcsException {
    return getContent(change.getRelativeFileName(), vcsRoot, getVersion(change, contentType));
  }

  @NotNull
  public byte[] getContent(@NotNull String path, @NotNull VcsRoot root, @NotNull String version) throws VcsException {
    final GetObjectProcessor processor = new GetObjectProcessor(path, version);
    VaultConnection.doInConnection(root.getProperties(), processor, true);
    try {
      return FileUtil.loadFileBytes(processor.getObject());
    } catch (IOException e) {
      throw new VcsException(e);
    }
  }

  @NotNull
  private static String getVersion(@NotNull VcsChangeInfo change, @NotNull VcsChangeInfo.ContentType contentType) {
    return contentType == VcsChangeInfo.ContentType.BEFORE_CHANGE ? change.getBeforeChangeRevisionNumber() : change.getAfterChangeRevisionNumber();
  }

  private static final class GetObjectProcessor implements VaultConnection.InConnectionProcessor {
    private final String myPath;
    private final String myVersion;
    private File myObject;

    private GetObjectProcessor(String path, String version) {
      myPath = path;
      myVersion = version;
    }

    public void process() throws Throwable {
      myObject = VaultConnection.getObject(myPath, myVersion);
    }

    public File getObject() {
      return myObject;
    }
  }
}
