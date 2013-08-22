package jetbrains.buildServer.buildTriggers.vcs.vault;

import jetbrains.buildServer.vcs.VcsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Created by Victory.Bedrosova on 8/19/13.
 */
public interface VaultConnection1 {
  /**
   * Returns parameters used to create this connection
   */
  @NotNull
  VaultConnectionParameters getParameters();

  /**
   * Checks if this connection is alive, which means Vault API method
   * VaultClientIntegrationLib.ServerOperations#isConnected() returns true
   *
   * @return true if the connection is alive, false otherwise
   * @throws VcsException
   */
  boolean isAlive() throws VcsException;

  /**
   * Cleans temp folders
   */
  void resetCaches();

  /**
   * Gets the specified version of a repo object (file or folder) and stores it in a temp folder
   * You may delete the file when it's no longer used
   *
   * @param path path to the object in repo
   * @param version VCS root revision
   *
   * @return repo obeject or null if no object was present at the specified version
   * @throws VcsException
   */
  @Nullable
  File getObject(@NotNull String path, @NotNull String version) throws VcsException ;

  /**
   * Checks if object (file or folder) currently exists in repo
   *
   * @param path path to the object in repo
   *
   * @return true if the object currently exists, false otherwise
   * @throws VcsException
   */
  boolean objectExists(@NotNull String path) throws VcsException;

  /**
   * Returns repository head revision which is latest transaction id
   *
   * @throws VcsException
   */
  @NotNull
  String getCurrentVersion() throws VcsException;

  /**
   * Returns human readable repository head revision which is latest $ version
   *
   * @throws VcsException
   */
  @Nullable
  String getDisplayVersion(@NotNull String version) throws VcsException;

  void login() throws VcsException;
  void logout() throws VcsException;
}
