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
