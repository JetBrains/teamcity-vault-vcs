package jetbrains.buildServer.buildTriggers.vcs.vault.impl;

import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection1;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionFactory;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionParameters;
import jetbrains.buildServer.vcs.VcsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

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
      myConnection.login();
    }
    return myConnection;
  }

  @NotNull
  public VaultConnectionParameters getParameters() {
    return myConnection.getParameters();
  }

  public void login() throws VcsException {
    ensureActiveConnection();
  }

  public void logout() throws VcsException {
    myConnection.logout();
  }

  public boolean isAlive() throws VcsException {
    return ensureActiveConnection().isAlive();
  }

  public void resetCaches() {
    myConnection.resetCaches();
  }

  @Nullable
  public File getObject(@NotNull String path, @NotNull String version) throws VcsException {
    return ensureActiveConnection().getObject(path, version);
  }

  public boolean objectExists(@NotNull String path) throws VcsException {
    return ensureActiveConnection().objectExists(path);
  }

  @NotNull
  public String getCurrentVersion() throws VcsException {
    return ensureActiveConnection().getCurrentVersion();
  }

  @Nullable
  public String getDisplayVersion(@NotNull String version) throws VcsException {
    return ensureActiveConnection().getDisplayVersion(version);
  }
}
