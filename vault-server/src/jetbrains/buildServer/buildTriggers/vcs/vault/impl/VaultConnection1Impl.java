package jetbrains.buildServer.buildTriggers.vcs.vault.impl;

import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection1;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionParameters;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Victory.Bedrosova on 8/19/13.
 */
class VaultConnection1Impl implements VaultConnection1 {
  @NotNull
  private final VaultConnectionParameters myParameters;

  public VaultConnection1Impl(@NotNull final VaultConnectionParameters parameters) {
    myParameters = parameters;
  }

  @NotNull
  public VaultConnection getStaticConnection() {
    return new VaultConnection();
  }
}
