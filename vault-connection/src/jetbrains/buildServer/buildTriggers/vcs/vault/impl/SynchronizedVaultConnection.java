package jetbrains.buildServer.buildTriggers.vcs.vault.impl;

import jetbrains.buildServer.buildTriggers.vcs.vault.RawChangeInfo;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionParameters;
import jetbrains.buildServer.vcs.VcsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * Created by Victory.Bedrosova on 8/19/13.
 */
public class SynchronizedVaultConnection implements VaultConnection {
  @NotNull
  private final VaultConnection myConnection;

  public SynchronizedVaultConnection(@NotNull final VaultConnection connection) {
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

  @NotNull
  public synchronized File getExistingObject(@NotNull String path, @NotNull String version) throws VcsException {
    return myConnection.getExistingObject(path, version);
  }

  public synchronized boolean objectExists(@NotNull String path, @Nullable String version) throws VcsException {
    return myConnection.objectExists(path, version);
  }

  public synchronized void login() throws VcsException {
    myConnection.login();
  }

  public synchronized void logout() throws VcsException {
    myConnection.logout();
  }

  @NotNull
  public synchronized String getFolderVersion(@NotNull String path) throws VcsException {
    return myConnection.getFolderVersion(path);
  }

  @Nullable
  public synchronized Long getFolderDisplayVersion(@NotNull String path, @NotNull String version) throws VcsException {
    return myConnection.getFolderDisplayVersion(path, version);
  }

  public synchronized void labelFolder(@NotNull String path, @NotNull String version, @NotNull String label) throws VcsException {
    myConnection.labelFolder(path, version, label);
  }

  @NotNull
  public synchronized List<RawChangeInfo> getFolderHistory(@NotNull String path, @NotNull String fromVersion, @NotNull String toVersion) throws VcsException {
    return myConnection.getFolderHistory(path, fromVersion, toVersion);
  }
}
