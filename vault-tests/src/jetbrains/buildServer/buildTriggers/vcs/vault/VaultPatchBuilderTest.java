/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import VaultClientIntegrationLib.DateSortOption;
import VaultClientIntegrationLib.GetOptions;
import VaultClientIntegrationLib.ServerOperations;
import VaultClientIntegrationLib.UnchangedHandler;
import VaultClientOperationsLib.ChangeSetItemColl;
import VaultClientOperationsLib.LocalCopyType;
import VaultLib.VaultHistoryItem;
import java.io.ByteArrayOutputStream;
import java.io.File;
import jetbrains.buildServer.buildTriggers.vcs.vault.impl.VaultConnectionFactoryImpl;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.vcs.IncludeRule;
import jetbrains.buildServer.vcs.impl.VcsRootImpl;
import jetbrains.buildServer.vcs.patches.PatchBuilderImpl;
import jetbrains.buildServer.vcs.patches.PatchTestCase;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.*;

/**
 * User: vbedrosova
 * Date: 20.07.2009
 * Time: 18:29:44
 */

@Test
public class VaultPatchBuilderTest extends PatchTestCase {
  private static final int CONNECTION_TRIES_NUMBER = 20;

  private static final String SERVER_URL = "http://vault-server.labs.intellij.net/VaultService";
  private static final String USER = "vault-admin";
  private static final String PASWORD = "wuaEtESawETA";
  //private static final String SERVER_URL = System.getProperty("vault.test.server");
  //private static final String USER = System.getProperty("vault.test.login");
  //private static final String PASWORD = System.getProperty("vault.test.password");

  static {
    if (SERVER_URL == null) {
      fail("Vault test server URL is not specified in JVM arguments." +
        "Use -Dvault.test.server=\"...\" jvm option to specify Vault test server URL");
    }
    if (USER == null) {
      fail("Vault test user is not specified in JVM arguments." +
        "Use -Dvault.test.login=\"...\" jvm option to specify Vault test user");
    }
    if (PASWORD == null) {
      fail("Vault test user password is not specified in JVM arguments." +
        "Use -Dvault.test.password=\"...\" jvm option to specify Vault test user password");
    }
  }

  private long myBeginTx;
  private File myRepoContent;
  private File myCache;
  private File myTestData;

  @Override
  protected String getTestDataPath() {
    return myTestData.getAbsolutePath();
  }

  private String getObjectPathForRepo(@NotNull String path) {
    return new File(myRepoContent, path).getAbsolutePath();
  }

  private File getBeforeFolder() {
    return getTestData("before");
  }

  private File getAfterFolder() {
    return getTestData("after");
  }

  @BeforeSuite
  protected void setUpSuite() throws Exception {
    ServerOperations.client.LoginOptions.URL = SERVER_URL;
    ServerOperations.client.LoginOptions.User = USER;
    ServerOperations.client.LoginOptions.Password = PASWORD;
    ServerOperations.client.AutoCommit = true;

    final File testDataSvn = TestUtil.getTestData("repoContent_Vcs", null);

    myRepoContent = FileUtil.createTempDirectory("vault_repo", "");

    FileUtil.copyDir(testDataSvn, myRepoContent, false);

    FileUtil.createDir(new File(myRepoContent, "fold1"));
    FileUtil.createDir(new File(myRepoContent, "fold2"));
  }

  private long getBeginTx() throws Exception {
    final VaultHistoryItem[] historyItems = ServerOperations.ProcessCommandHistory("$", true, DateSortOption.desc, null, null, null, null, null, null, -1, -1, 1);
    return historyItems[0].get_TxID();
  }

  @AfterSuite
  protected void tearDownSuite() throws Exception {
    FileUtil.delete(myRepoContent);
  }

  @Override
  @BeforeMethod
  protected void setUp() throws Exception {
    super.setUp();

//    Thread.sleep(2000);

    myCache = FileUtil.createTempDirectory("vault_cache", "");
    VaultCache.enableCache(myCache);
    VaultUtil.createTempDir();

    myTestData = FileUtil.createTempDirectory("vault_testData", "");

    final String testName = getTestName();
    final File testDataSvn = TestUtil.getTestDataMayNotExist(testName + "_Vcs", null);
    final File testDataNoSvn = new File(myTestData, testName);

    if (testDataSvn.isDirectory()) {
      FileUtil.copyDir(testDataSvn, testDataNoSvn, false);
    } else {
      FileUtil.createDir(testDataNoSvn);
    }
    FileUtil.createDir(new File(testDataNoSvn, "before"));
    FileUtil.createDir(new File(testDataNoSvn, "after"));

    ServerOperations.client.LoginOptions.Repository = testName;

    try {
      ServerOperations.ProcessCommandDeleteRepository(testName);
    } catch (Exception e) {
      System.out.println("Unable to delete repository " + testName + "( may be doesn't exist )");
    }
    ServerOperations.ProcessCommandAddRepository(testName, false);


    for (int i = 1; i <= CONNECTION_TRIES_NUMBER; ++i) {
      try {
        ServerOperations.Login();
        break;
      } catch (Exception e) {
        ServerOperations.Logout();
      }
    }
    myBeginTx = getBeginTx();
    ServerOperations.Logout();
  }

  @Override
  @AfterMethod
  protected void tearDown() throws Exception {
//    Thread.sleep(2000);
    ServerOperations.ProcessCommandDeleteRepository(getTestName());
    FileUtil.delete(myCache);
    FileUtil.delete(myTestData);
  }



  private void runTest(String fromVersion, @NotNull String toVersion) throws Exception {
    final VcsRootImpl root = new VcsRootImpl(-1, "vault");
    root.addProperty("vault.server", SERVER_URL);
    root.addProperty("vault.user", USER);
    root.addProperty("secure:vault.password", PASWORD);
    root.addProperty("vault.repo", getTestName());

    final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
    final PatchBuilderImpl patchBuilder = new PatchBuilderImpl(outputBuffer);
    final VaultPatchBuilder vaultPatchBuilder = new VaultPatchBuilder(new VaultConnectionFactoryImpl().getOrCreateConnection(new VaultConnectionParameters(root)), fromVersion, toVersion);

    try {
      vaultPatchBuilder.buildPatch(patchBuilder, IncludeRule.createDefaultInstance());
    } finally {
      patchBuilder.close();
    }
    checkPatchResult(outputBuffer.toByteArray());
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testEmptyRepoCleanPatch() throws Exception {
    runTest(null, "" + myBeginTx);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testEmptyRepoFromEqualsTo() throws Exception {
    runTest("" + myBeginTx, "" + myBeginTx);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testEmptyRepoToDoesNotExist() throws Exception {
    runTest("" + myBeginTx, "" + (myBeginTx + 20));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testExportOneTextFile() throws Exception {
    final String[] toAdd = {getObjectPathForRepo("file1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd);
    ServerOperations.Logout();
    runTest(null, "" + (myBeginTx + 1));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testExportOneDir() throws Exception {
    FileUtil.createDir(new File(getAfterFolder(), "fold1"));

    final String[] toAdd = {getObjectPathForRepo("fold1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd);
    ServerOperations.Logout();
    runTest(null, "" + (myBeginTx + 1));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testRenameOneTextFile() throws Exception {
    final String[] toAdd = {getObjectPathForRepo("file1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd);
    ServerOperations.ProcessCommandRename("$/file1", "new_file1");
    ServerOperations.Logout();
    runTest("" + (myBeginTx + 1), "" + (myBeginTx + 2));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testRenameOneEmptyDir() throws Exception {
    FileUtil.createDir(new File(getBeforeFolder(), "fold1"));
    FileUtil.createDir(new File(getAfterFolder(), "new_fold1"));

    final String[] toAdd = {getObjectPathForRepo("fold1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd);
    ServerOperations.ProcessCommandRename("$/fold1", "new_fold1");
    ServerOperations.Logout();
    runTest("" + (myBeginTx + 1), "" + (myBeginTx + 2));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testDeleteOneTextFile() throws Exception {
    final String[] toAdd = {getObjectPathForRepo("file1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd);
    final String[] toDelete = {"$/file1"};
    ServerOperations.ProcessCommandDelete(toDelete);
    ServerOperations.Logout();
    runTest("" + (myBeginTx + 1), "" + (myBeginTx + 2));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testDeleteOneEmptyDir() throws Exception {
    FileUtil.createDir(new File(getBeforeFolder(), "fold1"));

    final String[] toAdd = {getObjectPathForRepo("fold1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd);
    final String[] toDelete = {"$/fold1"};
    ServerOperations.ProcessCommandDelete(toDelete);
    ServerOperations.Logout();
    runTest("" + (myBeginTx + 1), "" + (myBeginTx + 2));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddAndRenameOneTextFile() throws Exception {
    final String[] toAdd = {getObjectPathForRepo("file1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd);
    ServerOperations.ProcessCommandRename("$/file1", "new_file1");
    ServerOperations.Logout();
    runTest("" + myBeginTx, "" + (myBeginTx + 2));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddAndRenameOneEmptyDir() throws Exception {
    FileUtil.createDir(new File(getAfterFolder(), "new_fold1"));

    final String[] toAdd = {getObjectPathForRepo("fold1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd);
    ServerOperations.ProcessCommandRename("$/fold1", "new_fold1");
    ServerOperations.Logout();
    runTest("" + myBeginTx, "" + (myBeginTx + 2));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddAndDeleteOneTextFile() throws Exception {
    final String[] toAdd = {getObjectPathForRepo("file1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd);
    final String[] toDelete = {"$/file1"};
    ServerOperations.ProcessCommandDelete(toDelete);
    ServerOperations.Logout();
    runTest("" + myBeginTx, "" + (myBeginTx + 2));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddAndDeleteOneEmptyDir() throws Exception {
    final String[] toAdd = {getObjectPathForRepo("fold1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd);
    final String[] toDelete = {"$/fold1"};
    ServerOperations.ProcessCommandDelete(toDelete);
    ServerOperations.Logout();
    runTest("" + myBeginTx, "" + (myBeginTx + 2));
  }

//  @Test(groups = {"all", "vault"}, dataProvider = "dp")
//  public void testAddRenameAndDeleteOneTextFile() throws Exception {
//    final String[] toAdd = {getObjectPathForRepo("file1")};
//    ServerOperations.Login();
//    ServerOperations.ProcessCommandAdd("$", toAdd);
//    ServerOperations.ProcessCommandRename("$/file1", "new_file1");
//    final String[] toDelete = {"$/new_file1"};
//    ServerOperations.ProcessCommandDelete(toDelete);
//    ServerOperations.Logout();
//    runTest("" + myBeginTx, "" + (myBeginTx + 3));
//  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddRenameAndDeleteOneEmptyDir() throws Exception {
    final String[] toAdd = {getObjectPathForRepo("fold1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd);
    ServerOperations.ProcessCommandRename("$/fold1", "new_fold1");
    final String[] toDelete = {"$/new_fold1"};
    ServerOperations.ProcessCommandDelete(toDelete);
    ServerOperations.Logout();
    runTest("" + myBeginTx, "" + (myBeginTx + 3));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddFileIntoFolders() throws Exception {
    final String[] toAdd = {getObjectPathForRepo("file1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1/fold2", toAdd);
    ServerOperations.Logout();
    runTest("" + myBeginTx, "" + (myBeginTx + 1));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddFilesIntoFolders() throws Exception {
    final String[] toAdd1 = {getObjectPathForRepo("file1")};
    final String[] toAdd2 = {getObjectPathForRepo("file2")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1/fold2", toAdd1);
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd2);
    ServerOperations.Logout();
    runTest("" + myBeginTx, "" + (myBeginTx + 2));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testRenameFolder() throws Exception {
    final String[] toAdd1 = {getObjectPathForRepo("file1")};
    final String[] toAdd2 = {getObjectPathForRepo("file2")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1/fold2", toAdd1);
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd2);
    ServerOperations.ProcessCommandRename("$/fold1", "new_fold1");
    ServerOperations.Logout();
    runTest("" + (myBeginTx + 2), "" + (myBeginTx + 3));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddFilesIntoFoldersRenameFolder() throws Exception {
    final String[] toAdd1 = {getObjectPathForRepo("file1")};
    final String[] toAdd2 = {getObjectPathForRepo("file2")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1/fold2", toAdd1);
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd2);
    ServerOperations.ProcessCommandRename("$/fold1", "new_fold1");
    ServerOperations.Logout();
    runTest("" + myBeginTx, "" + (myBeginTx + 3));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testMoveFile() throws Exception {
    FileUtil.createDir(new File(getBeforeFolder(), "fold1"));

    final String[] toAdd = {getObjectPathForRepo("file1"), getObjectPathForRepo("fold1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd);
    ServerOperations.ProcessCommandMove("$/file1", "$/fold1");
    ServerOperations.Logout();
    runTest("" + (myBeginTx + 1), "" + (myBeginTx + 2));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddFileAndMove() throws Exception {
    FileUtil.createDir(new File(getBeforeFolder(), "fold1"));

    final String[] toAdd = {getObjectPathForRepo("file1"), getObjectPathForRepo("fold1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd);
    ServerOperations.ProcessCommandMove("$/file1", "$/fold1");
    ServerOperations.Logout();
    runTest("" + myBeginTx, "" + (myBeginTx + 2));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testMoveFolder() throws Exception {
    FileUtil.createDir(new File(getBeforeFolder(), "fold1"));
    FileUtil.createDir(new File(getBeforeFolder(), "fold2"));
    final File fold1 = new File(getAfterFolder(), "fold1");
    FileUtil.createDir(fold1);
    FileUtil.createDir(new File(fold1, "fold2"));

    final String[] toAdd = {getObjectPathForRepo("fold1"), getObjectPathForRepo("fold2")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd);
    ServerOperations.ProcessCommandMove("$/fold2", "$/fold1");
    ServerOperations.Logout();
    runTest("" + (myBeginTx + 1), "" + (myBeginTx + 2));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testMoveFolderWithContent() throws Exception {
    FileUtil.createDir(new File(getBeforeFolder(), "fold2"));

    final String[] toAdd1 = {getObjectPathForRepo("file2")};
    final String[] toAdd2 = {getObjectPathForRepo("file1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1/fold3", toAdd1);
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd2);
    ServerOperations.ProcessCommandCreateFolder("$/fold2");
    ServerOperations.ProcessCommandMove("$/fold1", "$/fold2");
    ServerOperations.Logout();
    runTest("" + (myBeginTx + 3), "" + (myBeginTx + 4));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddFileAndShare() throws Exception {
    final String[] toAdd1 = {getObjectPathForRepo("file1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd1);
    ServerOperations.ProcessCommandShare("$/fold1/file1", "$");
    ServerOperations.Logout();
    runTest("" + myBeginTx, "" + (myBeginTx + 2));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testShareFolderWithContent() throws Exception {
    FileUtil.createDir(new File(getBeforeFolder(), "fold2"));

    final String[] toAdd1 = {getObjectPathForRepo("file2")};
    final String[] toAdd2 = {getObjectPathForRepo("file1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1/fold3", toAdd1);
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd2);
    ServerOperations.ProcessCommandCreateFolder("$/fold2");
    ServerOperations.ProcessCommandShare("$/fold1", "$/fold2");
    ServerOperations.Logout();
    runTest("" + (myBeginTx + 3), "" + (myBeginTx + 4));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddFilesInFodlerAndShareFolder() throws Exception {
    final String[] toAdd1 = {getObjectPathForRepo("file2")};
    final String[] toAdd2 = {getObjectPathForRepo("file1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1/fold3", toAdd1);
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd2);
    ServerOperations.ProcessCommandCreateFolder("$/fold2");
    ServerOperations.ProcessCommandShare("$/fold1", "$/fold2");
    ServerOperations.Logout();
    runTest("" + myBeginTx, "" + (myBeginTx + 4));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testEditFile() throws Exception {
    final File workingFolder = FileUtil.createTempDirectory("vault_test", "");
    final String[] toAdd1 = {getObjectPathForRepo("file1")};
    final String[] toCheckoutAndCommit = {"$/fold1/file1"};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd1);
    ServerOperations.SetWorkingFolder("$/fold1/file1", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toCheckoutAndCommit, true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toCheckoutAndCommit);
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.Logout();
    runTest("" + (myBeginTx + 1), "" + (myBeginTx + 2));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddAndEditFile() throws Exception {
    final File workingFolder = FileUtil.createTempDirectory("vault_test", "");
    final String[] toAdd1 = {getObjectPathForRepo("file1")};
    final String[] toCheckoutAndCommit = {"$/fold1/file1"};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd1);
    ServerOperations.SetWorkingFolder("$/fold1/file1", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toCheckoutAndCommit, true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toCheckoutAndCommit);
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.Logout();
    runTest("" + myBeginTx, "" + (myBeginTx + 2));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testDeleteFileAndThenParentDir() throws Exception {
    final String[] toAdd1 = {getObjectPathForRepo("file1")};
    final String[] toDelete1 = {"$/fold1/file1"};
    final String[] toDelete2 = {"$/fold1"};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd1);
    ServerOperations.ProcessCommandDelete(toDelete1);
    ServerOperations.ProcessCommandDelete(toDelete2);
    ServerOperations.Logout();
    runTest("" + (myBeginTx + 1), "" + (myBeginTx + 3));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testBranchFolderWithContent() throws Exception {
    final String[] toAdd1 = {getObjectPathForRepo("file1")};
    final String[] toAdd2 = {getObjectPathForRepo("file2")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd1);
    ServerOperations.ProcessCommandAdd("$/fold1/fold2", toAdd2);
    ServerOperations.ProcessCommandBranch("$/fold1", "$/branch_fold");
    ServerOperations.Logout();
    runTest("" + (myBeginTx + 2), "" + (myBeginTx + 3));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testEditFileBuildPatchTwice() throws Exception {
    final File workingFolder = FileUtil.createTempDirectory("vault_test", "");
    final String[] toAdd1 = {getObjectPathForRepo("file1")};
    final String[] toCheckoutAndCommit = {"$/fold1/file1"};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd1);
    ServerOperations.SetWorkingFolder("$/fold1/file1", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toCheckoutAndCommit, true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toCheckoutAndCommit);
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.Logout();
    runTest("" + (myBeginTx + 1), "" + (myBeginTx + 2));
    runTest("" + (myBeginTx + 1), "" + (myBeginTx + 2));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testEditFileBuildPatchTwiceDisabledCache() throws Exception {
    VaultCache.enableCache(null);

    final File workingFolder = FileUtil.createTempDirectory("vault_test", "");
    final String[] toAdd1 = {getObjectPathForRepo("file1")};
    final String[] toCheckoutAndCommit = {"$/fold1/file1"};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd1);
    ServerOperations.SetWorkingFolder("$/fold1/file1", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toCheckoutAndCommit, true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toCheckoutAndCommit);
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.Logout();
    runTest("" + (myBeginTx + 1), "" + (myBeginTx + 2));
    runTest("" + (myBeginTx + 1), "" + (myBeginTx + 2));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testBigPatch() throws Exception {
    final String[] toAdd1 = {getObjectPathForRepo("file1")};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd1);
    ServerOperations.ProcessCommandCreateFolder("$/fold1");
    ServerOperations.ProcessCommandMove("$/file1", "$/fold1");
    ServerOperations.ProcessCommandCreateFolder("$/fold2");
    ServerOperations.ProcessCommandMove("$/fold1", "$/fold2");
    ServerOperations.ProcessCommandRename("$/fold2/fold1", "folder1");
    ServerOperations.ProcessCommandRename("$/fold2", "folder2");
    ServerOperations.ProcessCommandRename("$/folder2/folder1/file1", "f1");
    ServerOperations.ProcessCommandRename("$/folder2/folder1/f1", "f2");
    ServerOperations.Logout();
    runTest("" + myBeginTx, "" + (myBeginTx + 9));
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  // TW-18188
  public void testLabelNotBreakHistory() throws Exception {
    final File workingFolder = FileUtil.createTempDirectory("vault_test", "");
    final String[] toAdd1 = {getObjectPathForRepo("file1")};
    final String[] toCheckoutAndCommit = {"$/file1"};
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd1);
    ServerOperations.SetWorkingFolder("$", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toCheckoutAndCommit, true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toCheckoutAndCommit);
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.ProcessCommandLabel("$", "label_name", -1); // label latest version;
    ServerOperations.Logout();
    runTest("" + (myBeginTx + 1), "" + (myBeginTx + 2));
  }
}
