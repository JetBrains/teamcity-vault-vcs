

package jetbrains.buildServer.buildTriggers.vcs.vault;

import java.io.File;
import java.util.List;
import jetbrains.buildServer.vcs.VcsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Victory.Bedrosova on 8/19/13.
 */
public interface VaultConnection {
  /**
   * Returns parameters used to create this connection
   */
  @NotNull
  VaultConnectionParameters getParameters() throws VcsException;

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
  void resetCaches() throws VcsException;

  void login() throws VcsException;
  void logout() throws VcsException;
  void refresh() throws VcsException;


  /**
   * Gets list of available repositories
   * @return see above
   */
  @NotNull
  public List<RepositoryInfo> getRepositories() throws VcsException ;

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
   * Same as previous method but throws exception if object not found
   */
  @NotNull
  File getExistingObject(@NotNull String path, @NotNull String version) throws VcsException;

  /**
   * Checks if the specified repo obejct exists at the specified revision
   *
   * @param path path to the object in repo
   * @param version VCS root revision or null for head revision
   *
   * @return true if the object exisits, false otherwise
   * @throws VcsException
   */
  boolean objectExists(@NotNull String path, @Nullable String version) throws VcsException;

  /**
   * Returns head revision for the specified folder which is latest transaction id for the specified folder
   * Path must exist
   *
   * @param path path to the fodler in repo
   *
   * @throws VcsException
   */
  @NotNull
  String getFolderVersion(@NotNull String path) throws VcsException;

  /**
   * Returns human readable revision of the specified folder or null if folder is not present at the specified version
   *
   * @param path path to the object in repo
   * @param version VCS root revision
   *
   * @throws VcsException
   */
  @Nullable
  Long getFolderDisplayVersion(@NotNull String path, @NotNull String version) throws VcsException;

  /**
   * Labels the specified folder at specified revision with specified label,
   * if another revision of the same folder is already labled with the same label, existing label is deleted
   *
   * @param path path to the object in repo
   * @param version VCS root revision
   * @param label non-empty lable text
   *
   * @throws VcsException
   */
  void labelFolder(@NotNull String path, @NotNull String version, @NotNull String label) throws VcsException;

  /**
   * Returns list of commit history items for the specified repo object
   *
   * @param path path to the object in repo
   * @param fromVersion start VCS root revision
   * @param toVersion end VCS root revision
   */
  @NotNull
  List<RawChangeInfo> getFolderHistory(@NotNull String path, @NotNull String fromVersion, @NotNull String toVersion) throws VcsException;
}