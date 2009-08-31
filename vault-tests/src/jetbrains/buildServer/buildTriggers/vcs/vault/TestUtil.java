package jetbrains.buildServer.buildTriggers.vcs.vault;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * User: vbedrosova
 * Date: 27.08.2009
 * Time: 13:00:25
 */
public class TestUtil {
  public static File getTestData(String fileName, String folderName) throws FileNotFoundException {
    final String relativeFileName = "testData" + (folderName != null ? File.separator + folderName : "") + (fileName != null ? File.separator + fileName : "");
    final File file1 = new File("vault-tests" + File.separator + relativeFileName);
    if (file1.exists()) {
      return file1;
    }
    final File file3 = new File("svnrepo" + File.separator
                              + "vault" + File.separator
                              + "vault-tests" + File.separator + relativeFileName);
    if (file3.exists()) {
      return file3;
    }
    throw new FileNotFoundException(file1.getAbsolutePath() + " or file " + file3.getAbsolutePath() + " should exist.");
  }
}
