package jetbrains.buildServer.buildTriggers.vcs.vault.impl;

import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection1;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionFactory;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionParameters;
import jetbrains.buildServer.vcs.VcsException;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Victory.Bedrosova on 8/19/13.
 */
public class EternalVaultConnection1 implements VaultConnection1 {
  @NotNull
  private VaultConnection1 myConnection;
  @NotNull
  private final VaultConnectionFactory myConnectionFactory;

  public EternalVaultConnection1(@NotNull final VaultConnection1 connection, @NotNull final VaultConnectionFactory connectionFactory) {
    myConnection = connection;
    myConnectionFactory = connectionFactory;
  }

  @NotNull
  private VaultConnection1 ensureActiveConnection() throws VcsException {
    if (!myConnection.isAlive()) {
      myConnection = myConnectionFactory.getOrCreateConnection(getParameters());
    }
    return myConnection;
  }

  @NotNull
  public VaultConnectionParameters getParameters() {
    return myConnection.getParameters();
  }

  public boolean isAlive() throws VcsException {
    return ensureActiveConnection().isAlive();
  }
}
