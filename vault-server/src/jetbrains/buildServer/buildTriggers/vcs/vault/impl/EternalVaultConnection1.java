package jetbrains.buildServer.buildTriggers.vcs.vault.impl;

import jetbrains.buildServer.buildTriggers.vcs.vault.RawChangeInfo;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection1;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionParameters;
import jetbrains.buildServer.vcs.VcsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * Created by Victory.Bedrosova on 8/19/13.
 */
public class EternalVaultConnection1 implements VaultConnection1 {
  @NotNull
  private VaultConnection1 myConnection;

  public EternalVaultConnection1(@NotNull final VaultConnection1 connection) {
    myConnection = connection;
  }

  @NotNull
  private VaultConnection1 ensureActiveConnection() throws VcsException {
    if (!myConnection.isAlive()) {
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

  @NotNull
  public File getExistingObject(@NotNull String path, @NotNull String version) throws VcsException {
    return ensureActiveConnection().getExistingObject(path, version);
  }

  public boolean objectExists(@NotNull String path, @Nullable String version) throws VcsException {
    return ensureActiveConnection().objectExists(path, version);
  }

  @NotNull
  public String getFolderVersion(@NotNull String path) throws VcsException {
    return ensureActiveConnection().getFolderVersion(path);
  }

  @Nullable
  public Long getFolderDisplayVersion(@NotNull String path, @NotNull String version) throws VcsException {
    return ensureActiveConnection().getFolderDisplayVersion(path, version);
  }

  public void labelFolder(@NotNull String path, @NotNull String version, @NotNull String label) throws VcsException {
    ensureActiveConnection().labelFolder(path, version, label);
  }

  @NotNull
  public List<RawChangeInfo> getFolderHistory(@NotNull String path, @NotNull String fromVersion, @NotNull String toVersion) throws VcsException {
    return ensureActiveConnection().getFolderHistory(path, fromVersion, toVersion);
  }
}
