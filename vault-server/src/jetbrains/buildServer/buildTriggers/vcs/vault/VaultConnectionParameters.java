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

  @NotNull private final String myUrl;
  @NotNull private final String myRepoName;
  @NotNull private final String myUser;
  @NotNull private final String myPassword;

  public VaultConnectionParameters(@NotNull Map<String, String> properties) {
    myUrl = properties.get(SERVER);
    myRepoName = properties.get(REPO);
    myUser = properties.get(USER);
    myPassword = properties.get(PASSWORD);
  }

  public VaultConnectionParameters(@NotNull VcsRoot root) {
    this(root.getProperties());
  }

  @NotNull public String getUrl() {
    return myUrl;
  }

  @NotNull public String getRepoName() {
    return myRepoName;
  }

  @NotNull public String getUser() {
    return myUser;
  }

  @NotNull public String getPassword() {
    return myPassword;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VaultConnectionParameters that = (VaultConnectionParameters) o;

    if (!myPassword.equals(that.myPassword)) return false;
    if (!myRepoName.equals(that.myRepoName)) return false;
    if (!myUrl.equals(that.myUrl)) return false;
    if (!myUser.equals(that.myUser)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myUrl.hashCode();
    result = 31 * result + myRepoName.hashCode();
    result = 31 * result + myUser.hashCode();
    result = 31 * result + myPassword.hashCode();
    return result;
  }
}
