package jetbrains.buildServer.buildTriggers.vcs.vault.impl;

import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection1;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionParameters;
import jetbrains.buildServer.vcs.VcsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

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

  public synchronized void resetCaches() {
    myConnection.resetCaches();
  }

  @Nullable
  public synchronized File getObject(@NotNull String path, @NotNull String version) throws VcsException {
    return myConnection.getObject(path, version);
  }

  public synchronized boolean objectExists(@NotNull String path) throws VcsException {
    return myConnection.objectExists(path);
  }

  public synchronized void login() throws VcsException {
    myConnection.login();
  }

  public void logout() throws VcsException {
    myConnection.logout();
  }

  @NotNull
  public synchronized String getCurrentVersion() throws VcsException {
    return myConnection.getCurrentVersion();
  }

  @Nullable
  public synchronized String getDisplayVersion(@NotNull String version) throws VcsException {
    return myConnection.getDisplayVersion(version);
  }
}
