package jetbrains.buildServer.vcs.patches.fs;

import junit.framework.TestCase;

/**
 * User: vbedrosova
 * Date: 24.12.2009
 * Time: 13:10:20
 */
public class MemoryFileSystemTest extends TestCase {
  public void testCheckPathOk() {
    MemoryFileSystem.checkPath("foo");
    MemoryFileSystem.checkPath("foo/bar");
    MemoryFileSystem.checkPath("foo/bar/baz");
    MemoryFileSystem.checkPath("abcdefghijklmnopqrstuvwxyz");
    MemoryFileSystem.checkPath("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    MemoryFileSystem.checkPath("0123456789");
    MemoryFileSystem.checkPath("src/com/jetbrains/patcher/Patcher.java");
  }

  public void testCheckPathFailure1() {
    assertFalse(MemoryFileSystem.checkPath(""));
  }

  public void testCheckPathFailure2() {
    assertFalse(MemoryFileSystem.checkPath("/foo"));
  }

  public void testCheckPathFailure3() {
    assertFalse(MemoryFileSystem.checkPath("foo/"));
  }

  public void testCheckPathFailure4() {
    assertFalse(MemoryFileSystem.checkPath("foo//bar"));
  }
}
