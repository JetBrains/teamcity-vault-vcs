package jetbrains.buildServer.buildTriggers.vcs.vault.impl;

import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection1;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionParameters;
import jetbrains.buildServer.vcs.VcsException;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Victory.Bedrosova on 8/19/13.
 */
public class SynchronizedVaultConnection implements VaultConnection1 {
  @NotNull
  private final VaultConnection1 myConnection;

  public SynchronizedVaultConnection(@NotNull final VaultConnection1 connection) {
    myConnection = connection;
  }

  @NotNull
  public VaultConnectionParameters getParameters() {
    return myConnection.getParameters();
  }

  public synchronized boolean isAlive() throws VcsException {
    return myConnection.isAlive();
  }
}
