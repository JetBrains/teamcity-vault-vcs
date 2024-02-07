

package jetbrains.buildServer.buildTriggers.vcs.vault.connection;

import java.io.File;
import java.util.List;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionParameters;
import org.jetbrains.annotations.NotNull;

/**
 * @User Victory.Bedrosova
 * 2/19/14.
 */
class FullClassLoadingVaultConnection extends ClassLoadingVaultConnection {
  public FullClassLoadingVaultConnection(@NotNull final VaultConnectionParameters parameters, @NotNull final List<File> jars) {
    super(parameters, getClassLoader(jars));
  }

  @NotNull
  private static ClassLoader getClassLoader(@NotNull final List<File> jars) {
    return new VaultApiJarClassLoader(FullClassLoadingVaultConnection.class.getClassLoader(), jars);
  }
}