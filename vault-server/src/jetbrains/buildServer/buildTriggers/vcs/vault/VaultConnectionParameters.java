package jetbrains.buildServer.buildTriggers.vcs.vault;

import org.jetbrains.annotations.NotNull;
import jetbrains.buildServer.vcs.VcsRoot;

import java.util.Map;
import java.io.File;

/**
 * User: vbedrosova
 * Date: 22.09.2009
 * Time: 16:57:57
 */
public final class VaultConnectionParameters {
  public static String SERVER = "vault.server";
  public static String REPO = "vault.repo";
  public static String USER = "vault.user";
  public static String PASSWORD = "secure:vault.password";

  private final String myUrl;
  private final String myRepoName;
  private final String myUser;
  private final String myPassword;

  public VaultConnectionParameters(@NotNull Map<String, String> properties) {
    myUrl = properties.get(SERVER);
    myRepoName = properties.get(REPO);
    myUser = properties.get(USER);
    myPassword = properties.get(PASSWORD);
  }

  public VaultConnectionParameters(@NotNull VcsRoot root) {
    this(root.getProperties());
  }

  public String getUrl() {
    return myUrl;
  }

  public String getRepoName() {
    return myRepoName;
  }

  public String getUser() {
    return myUser;
  }

  public String getPassword() {
    return myPassword;
  }
}
