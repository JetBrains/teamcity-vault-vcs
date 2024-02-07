
package jetbrains.buildServer.vcs.patches;

import jetbrains.buildServer.vcs.patches.fs.MemoryFileSystemImplTest;
import jetbrains.buildServer.vcs.patches.fs.MemoryFileSystemTest;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Maxim Podkolzine (maxim.podkolzine@gmail.com)
 */
public class AllTests extends TestSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTestSuite(MemoryFileSystemTest.class);
    suite.addTestSuite(MemoryFileSystemImplTest.class);
    suite.addTestSuite(ChangesPatchBuilderTest.class);

    return suite;
  }
}