package jetbrains.buildServer.buildTriggers.vcs.vault;

import com.intellij.util.containers.HashMap;
import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.plugins.classLoaders.TeamCityClassLoader;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @User Victory.Bedrosova
 * 1/8/14.
 */
public abstract class VaultApiConnector {
  private static final Logger LOG = Logger.getLogger(VaultApiConnector.class);

  @Nullable
  private volatile ClassLoader myVaultCommonApiClassLoader;
  @Nullable
  private final Integer myMaxClassLoaders;
  @NotNull
  private final Map<VaultConnectionParameters, VaultApiClassLoader> myClassLoaders = new HashMap<VaultConnectionParameters, VaultApiClassLoader>();

  public VaultApiConnector(final @Nullable Integer maxClassLoaders) {
    myMaxClassLoaders = maxClassLoaders;
  }

  @NotNull
  protected abstract File getVaultConnectionJar();

  @Nullable
  protected abstract File getVaultApiFolder();

  public synchronized boolean detectApi() {
    try {
      getVaultApiClassLoader(null).loadClass("VaultClientIntegrationLib.ServerOperations");
    } catch (ClassNotFoundException e) {
      LOG.debug(e.getMessage(), e);
      return false;
    } catch (NoClassDefFoundError e) {
      LOG.debug(e.getMessage(), e);
      return false;
    }
    return true;
  }

  @NotNull
  public synchronized ClassLoader getVaultApiClassLoader(@Nullable VaultConnectionParameters parameters) {
    VaultApiClassLoader classLoader = myClassLoaders.get(parameters);
    if (classLoader == null) {

      if (myVaultCommonApiClassLoader == null) {
        myVaultCommonApiClassLoader = new VaultCommonApiClassLoader(getClass().getClassLoader(), getVaultApiFolder());
      }

      //noinspection ConstantConditions
      classLoader = new VaultApiClassLoader(myVaultCommonApiClassLoader, getVaultConnectionJar(), getVaultApiFolder());
      myClassLoaders.put(parameters, classLoader);

      if (myMaxClassLoaders != null && myClassLoaders.size() > myMaxClassLoaders) throw new IllegalStateException("Only " + myMaxClassLoaders + " classloaders permitted");
    }
    return classLoader;
  }

  // we load this "heavy" Vault libs only once to save PermGen
  private static final List<String> COMMON_VAULT_LIBS =
    Arrays.asList("mscorlib.jar", "System.Web.jar", "System.Windows.Forms.jars", "System.jar", "System.Xml.jar", "System.Data.jar");

  private static abstract class BaseVaultApiClassLoader extends TeamCityClassLoader {
    private BaseVaultApiClassLoader(@NotNull final ClassLoader parent, @Nullable File vaultApiFolder) {
      super(parent, false);
      addJars(vaultApiFolder);
    }

    private void addJars(@Nullable final File vaultApiFolder) {
      if (vaultApiFolder == null) return;

      final File[] jars = vaultApiFolder.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return isValidVaultLib(vaultApiFolder, dir, name) && acceptJar(name);
        }
      });

      if (jars == null) return;

      for (File jar : jars) {
        addJar(jar);
      }
    }

    protected abstract boolean acceptJar(@NotNull String name);

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
  }

  private static class VaultCommonApiClassLoader extends BaseVaultApiClassLoader {
    private VaultCommonApiClassLoader(@NotNull final ClassLoader parent, @Nullable File vaultApiFolder) {
      super(parent, vaultApiFolder);
    }

    @Override
    protected boolean acceptJar(@NotNull final String name) {
      return isCommonVaultLib(name);
    }
  }

  private static class VaultApiClassLoader extends BaseVaultApiClassLoader {
    private VaultApiClassLoader(@NotNull final ClassLoader parent, @NotNull File vaultConnectionJar, @Nullable File vaultApiFolder) {
      super(parent, vaultApiFolder);
      addJar(vaultConnectionJar);
    }

    @Override
    protected boolean acceptJar(@NotNull final String name) {
      return !isCommonVaultLib(name);
    }
  }

  private static boolean isValidVaultLib(@NotNull File vaultApiFolder, @NotNull File dir, @NotNull String name) {
    return vaultApiFolder.equals(dir) && name.endsWith(".jar") && !isLog4j(name);
  }

  private static boolean isCommonVaultLib(@NotNull String fileName) {
    return COMMON_VAULT_LIBS.contains(fileName);
  }

  private static boolean isLog4j(@NotNull String fileName) {
    return fileName.matches("log4j\\-.*\\.jar");
  }
}
