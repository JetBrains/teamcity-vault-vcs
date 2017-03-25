/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import jetbrains.buildServer.vcs.VcsChangeInfo;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Victory.Bedrosova on 8/20/13.
 *
 * Represents one changed file
 */
public class ChangeInfo {
  @NotNull
  private final String myChangeName;
  @NotNull
  private final String myRepoPath;
  @NotNull
  private final String myRelativePath;
  @NotNull
  private final ModificationInfo myModificationInfo;
  @NotNull
  private final VcsChangeInfo.Type myChangeType;

  public ChangeInfo(@NotNull String changeName,
                    @NotNull String repoPath,
                    @NotNull String relativePath,
                    @NotNull ModificationInfo modificationInfo,
                    @NotNull VcsChangeInfo.Type changeType) {
    myChangeName = changeName;
    myRepoPath = repoPath;
    myRelativePath = relativePath;
    myModificationInfo = modificationInfo;
    myChangeType = changeType;
  }

  @NotNull
  public String getChangeName() {
    return myChangeName;
  }

  @NotNull
  public String getRepoPath() {
    return myRepoPath;
  }

  @NotNull
  public String getRelativePath() {
    return myRelativePath;
  }

  @NotNull
  public ModificationInfo getModificationInfo() {
    return myModificationInfo;
  }

  @NotNull
  public VcsChangeInfo.Type getChangeType() {
    return myChangeType;
  }
}
