/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.buildTriggers.vcs.vault;

import java.io.File;
import jetbrains.buildServer.buildTriggers.vcs.vault.connection.VaultConnectionFactoryProxy;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.WaitFor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * @User Victory.Bedrosova
 * 1/8/14.
 */
@Test
public class VaultConnectionTest extends Assert {
  private static final String SERVER_URL = System.getProperty("vault.test.server");
  private static final String USER = System.getProperty("vault.test.login");
  private static final String PASWORD = System.getProperty("vault.test.password");
  private static final String REPO = System.getProperty("vault.test.repo");

  static {
    if (SERVER_URL == null) {
      Assert.fail("Vault test server URL is not specified in JVM arguments." +
        "Use -Dvault.test.server=\"...\" jvm option to specify Vault test server URL");
    }
    if (USER == null) {
      Assert.fail("Vault test user is not specified in JVM arguments." +
        "Use -Dvault.test.login=\"...\" jvm option to specify Vault test user");
    }
    if (PASWORD == null) {
      Assert.fail("Vault test user password is not specified in JVM arguments." +
        "Use -Dvault.test.password=\"...\" jvm option to specify Vault test user password");
    }
    if (REPO == null) {
      Assert.fail("Vault test user repository is not specified in JVM arguments." +
        "Use -Dvault.test.repo=\"...\" jvm option to specify Vault test user repository");
    }
  }

  public static final int THREADS_NUM = 100;

  private File myCache;
  private volatile int myCounter;
  private volatile Throwable myError;

  @BeforeSuite
  protected void setUpSuite() throws Exception {
    myCache = FileUtil.createTempDirectory("vault_cache", "");
    myCounter = 0;
  }

  @AfterSuite
  protected void tearDownSuite() throws Exception {
    FileUtil.delete(myCache);
  }

  @Test
  public void testApiClassLoaders() throws Throwable {
    final File apiFolder = getPluginFile("vaultAPI");
    final File connectionJar = getPluginFile("out/artifacts/plugin/standalone/vault-connection.jar");

    Assert.assertTrue(apiFolder.isDirectory());
    Assert.assertTrue(connectionJar.isFile());

    final VaultConnectionFactoryProxy factory = new VaultConnectionFactoryProxy() {
      @NotNull
      @Override
      protected File getVaultConnectionJar() {
        return connectionJar;
      }

      @Nullable
      @Override
      protected File getVaultApiFolder() {
        return apiFolder;
      }
    };

    for (int i = 0; i < THREADS_NUM; ++i) {
      new Thread(new Runnable() {
        public void run() {
          try {
            final VaultConnection connection = factory.getOrCreateConnection(new VaultConnectionParameters(SERVER_URL, REPO, USER, PASWORD, "VaultConnectionsPoolingTest", myCache));
            System.out.println(connection.getFolderVersion("$"));
            ++myCounter;
          } catch (Throwable e) {
            myError = e;
          }
        }
      }, "VaultConnectionsPoolingTest" + i).start();
    }

    new WaitFor(THREADS_NUM * 1000L) {
      @Override
      protected boolean condition() {
        return THREADS_NUM == myCounter;
      }
    };

    if (myError == null) return;

    throw myError;
  }

  @NotNull
  private File getPluginFile(@NotNull String path) throws Exception {
    final File file = new File("external-repos/vault/" + path);
    return file.exists() ? file : new File(path);
  }
}
