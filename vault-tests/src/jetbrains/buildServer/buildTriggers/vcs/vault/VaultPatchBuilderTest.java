/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import jetbrains.buildServer.vcs.patches.PatchTestCase;
import jetbrains.buildServer.vcs.patches.PatchBuilderImpl;
import jetbrains.buildServer.vcs.impl.SVcsRootImpl;
import jetbrains.buildServer.vcs.IncludeRule;
import jetbrains.buildServer.vcs.VcsRoot;
import jetbrains.buildServer.util.FileUtil;

import java.io.File;
import java.io.ByteArrayOutputStream;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 20.07.2009
 * Time: 18:29:44
 */

/*
As for now the versions are:

27.08.2009 11:20:31 repo created
27.08.2009 11:54:15 added $/file1.txt
27.08.2009 12:12:29 edited $/file1.txt

 */

public class VaultPatchBuilderTest extends PatchTestCase {
  private static final String PATH = "C:/vbedrosova/work/Vault/vaultJavaCLC/vault.cmd";
  private static final String SERVER = "ruspd-pc02.swiftteams.local:8888";
  private static final String USER = "admin";
  private static final String PASWORD = "password";
  private static final String REPO = "Test";

  protected String getTestDataPath() {
    return "vault-tests" + File.separator + "testData";
  }

  @BeforeMethod
  protected void setUp() throws Exception {
//    final File cache = FileUtil.createTempDirectory("vault", "");
//    FileUtil.delete(cache);
//    System.setProperty("vault.caches.path", cache.getAbsolutePath());
    final String testName = getTestName();
    final File testDataSvn = TestUtil.getTestData(testName + "_Vcs", null);
    final File testDataNoSvn = new File(testDataSvn.getAbsolutePath().replace("_Vcs", ""));
    FileUtil.createDir(testDataNoSvn);
    FileUtil.copyDir(testDataSvn, testDataNoSvn, false);
  }

  private void runTest(String fromVersion, @NotNull String toVersion) throws Exception {
    final SVcsRootImpl root = new SVcsRootImpl("vault");    
    root.addProperty("vault.path", PATH);
    root.addProperty("vault.server", SERVER);
    root.addProperty("vault.user", USER);
    root.addProperty("secure:vault.password", PASWORD);
    root.addProperty("vault.repo", REPO);

    final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
    final PatchBuilderImpl patchBuilder = new PatchBuilderImpl(outputBuffer);
    final VaultPatchBuilder vaultPatchBuilder = new VaultPatchBuilder(root, fromVersion, toVersion);

    try {
      vaultPatchBuilder.buildPatch(patchBuilder, IncludeRule.createDefaultInstance());
    } finally {
      patchBuilder.close();
    }
    checkPatchResult(outputBuffer.toByteArray());
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testExportOneTextFile() throws Exception {
    runTest(null, "27.08.2009 11:54:15");
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testChangeOneTextFile() throws Exception {
    runTest("27.08.2009 11:54:15", "27.08.2009 12:12:29");
  }
}
