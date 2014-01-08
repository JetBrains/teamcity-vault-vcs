package jetbrains.buildServer.buildTriggers.vcs.vault;

import org.jetbrains.annotations.NotNull;

/**
 * @User Victory.Bedrosova
 * 1/3/14.
 */
public class RepositoryInfo {
  private final int myId;
  @NotNull
  private final String myName;

  public RepositoryInfo(final int id, @NotNull final String name) {
    myId = id;
    myName = name;
  }

  public int getId() {
    return myId;
  }

  @NotNull
  public String getName() {
    return myName;
  }
}
