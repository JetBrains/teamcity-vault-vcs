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

import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.vcs.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.File;

/**
 * User: vbedrosova
 * Date: 03.07.2009
 * Time: 12:19:39
 */
public final class VaultFileContentProvider implements VcsFileContentProvider {
  @NotNull
  public byte[] getContent(@NotNull VcsModification vcsModification, @NotNull VcsChangeInfo change, @NotNull VcsChangeInfo.ContentType contentType, @NotNull VcsRoot vcsRoot) throws VcsException {
    return getContent(change.getRelativeFileName(), vcsRoot,
      contentType == VcsChangeInfo.ContentType.BEFORE_CHANGE ? change.getBeforeChangeRevisionNumber()
        : change.getAfterChangeRevisionNumber());
  }

  @NotNull
  public byte[] getContent(@NotNull String path, @NotNull VcsRoot root, @NotNull String version) throws VcsException {
    final File f;
    synchronized (VaultConnection.LOCK) {
      try {
        VaultConnection.connect(root.getProperties());
        f = VaultConnection.getObject(path, version);
        return FileUtil.loadFileBytes(f);
      } catch (IOException e) {
        throw new VcsException(e.getMessage(), e);
      } finally {
        VaultConnection.disconnect();
      }
    }
  }
}
