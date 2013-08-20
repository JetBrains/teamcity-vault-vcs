package jetbrains.buildServer.buildTriggers.vcs.vault;

import jetbrains.buildServer.vcs.VcsException;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Victory.Bedrosova on 8/19/13.
 */
public interface VaultConnectionFactory {
  @NotNull VaultConnection1 getOrCreateConnection(@NotNull VaultConnectionParameters parameters);
}
