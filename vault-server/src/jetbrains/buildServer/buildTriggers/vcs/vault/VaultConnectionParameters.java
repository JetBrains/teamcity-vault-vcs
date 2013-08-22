package jetbrains.buildServer.buildTriggers.vcs.vault;

import com.intellij.util.containers.HashMap;
import java.util.Map;
import jetbrains.buildServer.vcs.VcsRoot;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Victory.Bedrosova on 8/19/13.
 */
public final class VaultConnectionParameters {
  @NotNull
  private final String myURL;
  @NotNull
  private final String myRepository;
  @NotNull
  private final String myUser;
  @NotNull
  private final String myPassword;
  @NotNull
  private final String myStringRepresentation;

  public VaultConnectionParameters(@NotNull final Map<String, String> parameters, @NotNull final String stringRepresentation) {
    this(parameters.get(VaultUtil.SERVER), parameters.get(VaultUtil.REPO), parameters.get(VaultUtil.USER), parameters.get(VaultUtil.PASSWORD), stringRepresentation);
  }

  public VaultConnectionParameters(@NotNull final VcsRoot vcsRoot) {
    this(vcsRoot.getProperties(), vcsRoot.toString());
  }

  public VaultConnectionParameters(@NotNull String URL,
                                   @NotNull String repository,
                                   @NotNull String user,
                                   @NotNull String password,
                                   @NotNull String stringRepresentation) {
    myURL = URL;
    myRepository = repository;
    myUser = user;
    myPassword = password;
    myStringRepresentation = stringRepresentation;
  }

  @NotNull
  public String getURL() {
    return myURL;
  }

  @NotNull
  public String getRepository() {
    return myRepository;
  }

  @NotNull
  public String getUser() {
    return myUser;
  }

  @NotNull
  public String getPassword() {
    return myPassword;
  }

  @NotNull
  public String getStringRepresentation() {
    return myStringRepresentation;
  }

  /**
   * For backward compatibility
   */
  @Deprecated
  @NotNull
  public Map<String, String> asMap() {
    final Map<String, String> map = new HashMap<String, String>();
    map.put(VaultUtil.SERVER, myURL);
    map.put(VaultUtil.REPO, myRepository);
    map.put(VaultUtil.USER, myUser);
    map.put(VaultUtil.PASSWORD, myPassword);
    return map;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VaultConnectionParameters that = (VaultConnectionParameters) o;

    if (!myPassword.equals(that.myPassword)) return false;
    if (!myRepository.equals(that.myRepository)) return false;
    if (!myURL.equals(that.myURL)) return false;
    if (!myUser.equals(that.myUser)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myURL.hashCode();
    result = 31 * result + myRepository.hashCode();
    result = 31 * result + myUser.hashCode();
    result = 31 * result + myPassword.hashCode();
    return result;
  }
}
