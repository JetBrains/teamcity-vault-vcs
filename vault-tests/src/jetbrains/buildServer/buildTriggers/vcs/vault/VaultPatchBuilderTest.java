

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
import java.io.IOException;
import jetbrains.buildServer.buildTriggers.vcs.vault.connection.VaultConnectionFactoryProxy;
import jetbrains.buildServer.buildTriggers.vcs.vault.impl.VaultConnectionImpl;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.vcs.impl.VcsRootImpl;
import jetbrains.buildServer.vcs.patches.PatchBuilderImpl;
import jetbrains.buildServer.vcs.patches.PatchTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.*;

/**
 * User: vbedrosova
 * Date: 20.07.2009
 * Time: 18:29:44
 */

@Test
public class VaultPatchBuilderTest extends PatchTestCase {
  private static final int CONNECTION_TRIES_NUMBER = 20;

  private static final String SERVER_URL = System.getProperty("vault.test.server");
  private static final String USER = System.getProperty("vault.test.login");
  private static final String PASWORD = System.getProperty("vault.test.password");

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

  @NotNull
  private static File getVaultFile(@NotNull String path) {
    final File file = new File("external-repos/vault/" + path);
    return file.exists() ? file : new File(path);
  }

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

  private void runTest(@Nullable Integer fromVersion, @NotNull Integer toVersion) throws Exception {
    final VcsRootImpl root = new VcsRootImpl(-1, "vault");
    root.addProperty("vault.server", SERVER_URL);
    root.addProperty("vault.user", USER);
    root.addProperty("secure:vault.password", PASWORD);
    root.addProperty("vault.repo", getTestName());

    final ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
    final PatchBuilderImpl patchBuilder = new PatchBuilderImpl(outputBuffer);

    final VaultPatchBuilder vaultPatchBuilder =
      new VaultPatchBuilder(VaultConnectionFactoryProxy.makeEternal(new VaultConnectionImpl(new VaultConnectionParameters(root, myCache))), patchBuilder, null);

    final String fromVersionStr = fromVersion == null ? null : String.valueOf(myBeginTx + fromVersion);
    final String toVersionStr = String.valueOf(myBeginTx + toVersion);

    try {
      if (fromVersionStr == null) {
        vaultPatchBuilder.buildCleanPatch(toVersionStr);
      } else {
        vaultPatchBuilder.buildIncrementalPatch(fromVersionStr, toVersionStr);
      }

    } finally {
      patchBuilder.close();
    }
    checkPatchResult(outputBuffer.toByteArray());
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testEmptyRepoCleanPatch() throws Exception {
    runTest(null, 0);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testEmptyRepoFromEqualsTo() throws Exception {
    runTest(0, 0);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testEmptyRepoFromDoesNotExist() throws Exception {
    runTest(20, 30);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testEmptyRepoToDoesNotExist() throws Exception {
    runTest(0, 20);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testExportOneTextFile() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd("file1"));
    ServerOperations.Logout();
    runTest(null, 1);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testExportOneTextFileToFolder() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.Logout();
    runTest(null, 1);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testExportOneDir() throws Exception {
    createAfterFolder("fold1");

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd("fold1"));
    ServerOperations.Logout();
    runTest(null, 1);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testRenameOneTextFile() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd("file1"));
    ServerOperations.ProcessCommandRename("$/file1", "new_file1");
    ServerOperations.Logout();
    runTest(1, 2);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testRenameOneEmptyDir() throws Exception {
    createBeforeFolder("fold1");
    createAfterFolder("new_fold1");

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd("fold1"));
    ServerOperations.ProcessCommandRename("$/fold1", "new_fold1");
    ServerOperations.Logout();
    runTest(1, 2);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testDeleteOneTextFile() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd("file1"));
    ServerOperations.ProcessCommandDelete(toArray("$/file1"));
    ServerOperations.Logout();
    runTest(1, 2);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testDeleteOneEmptyDir() throws Exception {
    createBeforeFolder("fold1");

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd("fold1"));
    ServerOperations.ProcessCommandDelete(toArray("$/fold1"));
    ServerOperations.Logout();
    runTest(1, 2);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddAndRenameOneTextFile() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd("file1"));
    ServerOperations.ProcessCommandRename("$/file1", "new_file1");
    ServerOperations.Logout();
    runTest(0, 2);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddAndRenameOneEmptyDir() throws Exception {
    createAfterFolder("new_fold1");

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd("fold1"));
    ServerOperations.ProcessCommandRename("$/fold1", "new_fold1");
    ServerOperations.Logout();
    runTest(0, 2);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddAndDeleteOneTextFile() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd("file1"));
    ServerOperations.ProcessCommandDelete(toArray("$/file1"));
    ServerOperations.Logout();
    runTest(0, 2);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddAndDeleteOneEmptyDir() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd("fold1"));
    ServerOperations.ProcessCommandDelete(toArray("$/fold1"));
    ServerOperations.Logout();
    runTest(0, 2);
  }

//  @Test(groups = {"all", "vault"}, dataProvider = "dp")
//  public void testAddRenameAndDeleteOneTextFile() throws Exception {
//    ServerOperations.Login();
//    ServerOperations.ProcessCommandAdd("$", toAdd("file1"));
//    ServerOperations.ProcessCommandRename("$/file1", "new_file1");
//    ServerOperations.ProcessCommandDelete(toArray("$/new_file1"));
//    ServerOperations.Logout();
//    runTest(0, 3);
//  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddRenameAndDeleteOneEmptyDir() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd("fold1"));
    ServerOperations.ProcessCommandRename("$/fold1", "new_fold1");
    ServerOperations.ProcessCommandDelete(toArray("$/new_fold1"));
    ServerOperations.Logout();
    runTest(0, 3);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddFileIntoFolders() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1/fold2", toAdd("file1"));
    ServerOperations.Logout();
    runTest(0, 1);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddFilesIntoFolders() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1/fold2", toAdd("file1"));
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file2"));
    ServerOperations.Logout();
    runTest(0, 2);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testRenameFolder() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1/fold2", toAdd("file1"));
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file2"));
    ServerOperations.ProcessCommandRename("$/fold1", "new_fold1");
    ServerOperations.Logout();
    runTest(2, 3);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddFilesIntoFoldersRenameFolder() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1/fold2", toAdd("file1"));
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file2"));
    ServerOperations.ProcessCommandRename("$/fold1", "new_fold1");
    ServerOperations.Logout();
    runTest(0, 3);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddFoldersWithFiles() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd("fold3"));
    ServerOperations.Logout();
    runTest(0, 3);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testMoveFile() throws Exception {
    createBeforeFolder("fold1");

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd("file1", "fold1"));
    ServerOperations.ProcessCommandMove("$/file1", "$/fold1");
    ServerOperations.Logout();
    runTest(1, 2);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddFileAndMove() throws Exception {
    createBeforeFolder("fold1");

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd("file1", "fold1"));
    ServerOperations.ProcessCommandMove("$/file1", "$/fold1");
    ServerOperations.Logout();
    runTest(0, 2);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testMoveFolder() throws Exception {
    createBeforeFolder("fold1");
    createBeforeFolder("fold2");

    createAfterFolder("fold1");
    createAfterFolder("fold1/fold2");

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd("fold1", "fold2"));
    ServerOperations.ProcessCommandMove("$/fold2", "$/fold1");
    ServerOperations.Logout();
    runTest(1, 2);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testMoveFolderWithContent() throws Exception {
    createBeforeFolder("fold2");

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1/fold3", toAdd("file2"));
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandCreateFolder("$/fold2");
    ServerOperations.ProcessCommandMove("$/fold1", "$/fold2");
    ServerOperations.Logout();
    runTest(3, 4);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddFileAndShare() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandShare("$/fold1/file1", "$");
    ServerOperations.Logout();
    runTest(0, 2);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddShareAndRenameFile() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandShare("$/fold1/file1", "$");
    ServerOperations.ProcessCommandRename("$/fold1/file1", "new_file1");
    ServerOperations.Logout();
    runTest(0, 3);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testShareAndRenameFile() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandShare("$/fold1/file1", "$");
    ServerOperations.ProcessCommandRename("$/file1", "new_file1");
    ServerOperations.Logout();
    runTest(1, 3);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testRenameSharedFile() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandShare("$/fold1/file1", "$");
    ServerOperations.ProcessCommandRename("$/fold1/file1", "new_file1");
    ServerOperations.Logout();
    runTest(2, 3);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddShareAndEditFile() throws Exception {
    final File workingFolder = createTempDir();

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandShare("$/fold1/file1", "$");
    ServerOperations.SetWorkingFolder("$/fold1/file1", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toArray("$/fold1/file1"), true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toArray("$/fold1/file1"));
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.Logout();
    runTest(0, 3);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testShareAndEditFile() throws Exception {
    final File workingFolder = createTempDir();

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandShare("$/fold1/file1", "$");
    ServerOperations.SetWorkingFolder("$/file1", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toArray("$/file1"), true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toArray("$/file1"));
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.Logout();
    runTest(1, 3);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testEditSharedFile() throws Exception {
    final File workingFolder = createTempDir();

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandShare("$/fold1/file1", "$");
    ServerOperations.SetWorkingFolder("$/file1", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toArray("$/file1"), true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toArray("$/file1"));
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.Logout();
    runTest(2, 3);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testMoveSharedFile() throws Exception {
    //createAfterFolder("fold1");

    final File workingFolder = createTempDir();

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandCreateFolder("$/fold2");
    ServerOperations.ProcessCommandCreateFolder("$/fold3");
    ServerOperations.ProcessCommandShare("$/fold1/file1", "$/fold2");
    ServerOperations.ProcessCommandMove("$/fold1/file1", "$/fold3");
    ServerOperations.SetWorkingFolder("$/fold3/file1", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toArray("$/fold3/file1"), true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toArray("$/fold3/file1"));
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.Logout();
    runTest(0, 7);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testMoveSharedFolder() throws Exception {
    final File workingFolder = createTempDir();

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandCreateFolder("$/fold2");
    ServerOperations.ProcessCommandCreateFolder("$/fold3");
    ServerOperations.ProcessCommandShare("$/fold1", "$/fold2");
    ServerOperations.ProcessCommandMove("$/fold1", "$/fold3");
    ServerOperations.SetWorkingFolder("$/fold2/fold1/file1", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toArray("$/fold2/fold1/file1"), true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toArray("$/fold2/fold1/file1"));
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.Logout();
    runTest(0, 6);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testMoveSharedFolderContent() throws Exception {
    final File workingFolder = createTempDir();

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandCreateFolder("$/fold2");
    ServerOperations.ProcessCommandCreateFolder("$/fold3");
    ServerOperations.ProcessCommandShare("$/fold1", "$/fold2");
    ServerOperations.ProcessCommandMove("$/fold1/file1", "$/fold3");
    ServerOperations.SetWorkingFolder("$/fold3/file1", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toArray("$/fold3/file1"), true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toArray("$/fold3/file1"));
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.Logout();
    runTest(0, 6);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testShareFolderWithContent() throws Exception {
    createBeforeFolder("fold2");

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1/fold3", toAdd("file2"));
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandCreateFolder("$/fold2");
    ServerOperations.ProcessCommandShare("$/fold1", "$/fold2");
    ServerOperations.Logout();
    runTest(3, 4);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testShareFolderWithContentEditRenameContent() throws Exception {
    createBeforeFolder("fold2");

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1/fold3", toAdd("file2"));
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandCreateFolder("$/fold2");
    ServerOperations.ProcessCommandShare("$/fold1", "$/fold2");
    ServerOperations.ProcessCommandRename("$/fold1/file1", "new_file1");

    final File workingFolder = createTempDir();

    ServerOperations.SetWorkingFolder("$/fold1/fold3/file2", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toArray("$/fold1/fold3/file2"), true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file2"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toArray("$/fold1/fold3/file2"));
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);

    ServerOperations.Logout();
    runTest(0, 6);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddFilesInFodlerAndShareFolder() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1/fold3", toAdd("file2"));
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandCreateFolder("$/fold2");
    ServerOperations.ProcessCommandShare("$/fold1", "$/fold2");
    ServerOperations.Logout();
    runTest(0, 4);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testEditFile() throws Exception {
    final File workingFolder = createTempDir();

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.SetWorkingFolder("$/fold1/file1", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toArray("$/fold1/file1"), true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toArray("$/fold1/file1"));
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.Logout();
    runTest(1, 2);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testAddAndEditFile() throws Exception {
    final File workingFolder = createTempDir();

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.SetWorkingFolder("$/fold1/file1", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toArray("$/fold1/file1"), true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toArray("$/fold1/file1"));
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.Logout();
    runTest(0, 2);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testDeleteFileAndThenParentDir() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandDelete(toArray("$/fold1/file1"));
    ServerOperations.ProcessCommandDelete(toArray("$/fold1"));
    ServerOperations.Logout();
    runTest(1, 3);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testBranchFolderWithContent() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandAdd("$/fold1/fold2", toAdd("file2"));
    ServerOperations.ProcessCommandBranch("$/fold1", "$/branch_fold");
    ServerOperations.Logout();
    runTest(2, 3);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testBranchFolderWithChangedContentRenameFolder() throws Exception {
    final File workingFolder = createTempDir();

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.SetWorkingFolder("$/fold1/file1", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toArray("$/fold1/file1"), true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toArray("$/fold1/file1"));
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.ProcessCommandBranch("$/fold1", "$/branch_fold");
    ServerOperations.ProcessCommandRename("$/branch_fold", "new_branch_fold");
    ServerOperations.Logout();
    runTest(0, 4);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testBranchFolderWithChangedContent() throws Exception {
    final File workingFolder = createTempDir();

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.SetWorkingFolder("$/fold1/file1", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toArray("$/fold1/file1"), true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toArray("$/fold1/file1"));
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.ProcessCommandBranch("$/fold1", "$/branch_fold");
    ServerOperations.Logout();
    runTest(0, 3);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testBranchChangedFolderWithContent() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandCreateFolder("$/fold");
    ServerOperations.ProcessCommandRename("$/fold", "fold2");
    ServerOperations.ProcessCommandRename("$/fold2", "fold1");
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandBranch("$/fold1", "$/branch_fold");
    ServerOperations.Logout();
    runTest(0, 5);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testShareFolderWithChangedContent() throws Exception {
    final File workingFolder = createTempDir();

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.SetWorkingFolder("$/fold1/file1", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toArray("$/fold1/file1"), true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toArray("$/fold1/file1"));
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.ProcessCommandCreateFolder("$/branch_fold");
    ServerOperations.ProcessCommandShare("$/fold1", "$/branch_fold");
    ServerOperations.Logout();
    runTest(0, 4);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testShareChangedFolderWithContent() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandCreateFolder("$/fold");
    ServerOperations.ProcessCommandRename("$/fold", "fold1");
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandCreateFolder("$/branch_fold");
    ServerOperations.ProcessCommandShare("$/fold1", "$/branch_fold");
    ServerOperations.Logout();
    runTest(0, 5);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testShareFolderWithContentToChangedFolder() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandCreateFolder("$/branch_fold1");
    ServerOperations.ProcessCommandRename("$/branch_fold1", "branch_fold2");
    ServerOperations.ProcessCommandRename("$/branch_fold2", "branch_fold");
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.ProcessCommandShare("$/fold1", "$/branch_fold");
    ServerOperations.Logout();
    runTest(0, 7);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testEditFileBuildPatchTwice() throws Exception {
    final File workingFolder = createTempDir();

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$/fold1", toAdd("file1"));
    ServerOperations.SetWorkingFolder("$/fold1/file1", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toArray("$/fold1/file1"), true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toArray("$/fold1/file1"));
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.Logout();
    runTest(1, 2);
    runTest(1, 2);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  public void testBigPatch() throws Exception {
    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd("file1"));
    ServerOperations.ProcessCommandCreateFolder("$/fold1");
    ServerOperations.ProcessCommandMove("$/file1", "$/fold1");
    ServerOperations.ProcessCommandCreateFolder("$/fold2");
    ServerOperations.ProcessCommandMove("$/fold1", "$/fold2");
    ServerOperations.ProcessCommandRename("$/fold2/fold1", "folder1");
    ServerOperations.ProcessCommandRename("$/fold2", "folder2");
    ServerOperations.ProcessCommandRename("$/folder2/folder1/file1", "f1");
    ServerOperations.ProcessCommandRename("$/folder2/folder1/f1", "f2");
    ServerOperations.Logout();
    runTest(0, 9);
  }

  @Test(groups = {"all", "vault"}, dataProvider = "dp")
  // TW-18188
  public void testLabelNotBreakHistory() throws Exception {
    final File workingFolder = createTempDir();

    ServerOperations.Login();
    ServerOperations.ProcessCommandAdd("$", toAdd("file1"));
    ServerOperations.SetWorkingFolder("$", workingFolder.getAbsolutePath(), false);
    ServerOperations.ProcessCommandCheckout(toArray("$/file1"), true, true, new GetOptions());
    FileUtil.copy(new File(getObjectPathForRepo("edited_file")), new File(workingFolder, "file1"));
    final ChangeSetItemColl cs = ServerOperations.ProcessCommandListChangeSet(toArray("$/file1"));
    ServerOperations.ProcessCommandCommit(cs, UnchangedHandler.Checkin, false, LocalCopyType.Leave, false);
    ServerOperations.ProcessCommandLabel("$", "label_name", -1); // label latest version;
    ServerOperations.Logout();
    runTest(1, 2);
  }

  @NotNull
  private String[] toAdd(String... files) {
    final String[] res = new String[files.length];
    for (int i = 0; i < files.length; ++i) {
      res[i] = getObjectPathForRepo(files[i]);
    }
    return res;
  }

  @NotNull
  private String[] toArray(String... files) {
    return files;
  }

  // empty folders can't be in test data
  private void createAfterFolder(@NotNull String name) throws IOException {
    FileUtil.createDir(new File(getAfterFolder(), name));
  }

  // empty folders can't be in test data
  private void createBeforeFolder(@NotNull String name) throws IOException {
    FileUtil.createDir(new File(getBeforeFolder(), name));
  }
}