package jetbrains.buildServer.buildTriggers.vcs.vault;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Victory.Bedrosova on 8/19/13.
 */
public interface VaultConnectionFactory {
  @NotNull
  VaultConnection getOrCreateConnection(@NotNull VaultConnectionParameters parameters);
}
