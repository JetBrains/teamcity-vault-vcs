

package jetbrains.buildServer.buildTriggers.vcs.vault.connection;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import jetbrains.buildServer.plugins.classLoaders.TeamCityClassLoader;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * @User Victory.Bedrosova
 * 2/18/14.
 */
class VaultApiJarClassLoader extends TeamCityClassLoader {
  private static final Logger LOG = Logger.getLogger(VaultApiJarClassLoader.class);

  VaultApiJarClassLoader(@NotNull final ClassLoader parent, @NotNull final List<File> jars) {
    super(parent, false);

    assertNoVaultApiInClassPath();

    for (File jar : jars) {
      if (isValidVaultLib(jar)) {
        addJar(jar);
      }
    }
    LOG.debug("Created new Vault API classloader with paths: " + getURLsAsString());
  }

  private static void assertNoVaultApiInClassPath() {
    try {
      VaultApiJarClassLoader.class.getClassLoader().loadClass("VaultClientIntegrationLib.ServerOperations");
    } catch (ClassNotFoundException e) {
      // expected
      return;
    }
    throw new IllegalStateException("VaultClientIntegrationLib.ServerOperations class is not expected to be in the current class path");
  }

  @NotNull
  private String getURLsAsString() {
    final StringBuilder sb = new StringBuilder();
    for (URL url : getURLs()) {
      sb.append(url).append("; ");
    }
    return sb.toString();
  }

  //@Override since 8.1
  @SuppressWarnings("override")
  protected void addJar(@NotNull final File jar) {
    addURL(toUrl(jar));
  }

  @NotNull
  private static URL toUrl(final File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException("Failed to create URL from file " + file, e);
    }
  }

  private static boolean isValidVaultLib(@NotNull File jar) {
    final String name = jar.getName();
    return name.endsWith(".jar") && !isLog4j(name);
  }

  private static boolean isLog4j(@NotNull String fileName) {
    return fileName.matches("log4j\\-.*\\.jar");
  }
}