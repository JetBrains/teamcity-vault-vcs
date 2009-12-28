package jetbrains.buildServer.vcs.patches.fs;

import jetbrains.buildServer.vcs.patches.util.Assert;

import java.util.Collection;

/**
 * User: vbedrosova
 * Date: 22.12.2009
 * Time: 13:18:23
 */
public class MemoryFileSystem {

  public MemoryFileSystem() {
    myImpl = new MemoryFileSystemImpl();
  }

  public void createFile(String path) {
    if (containsFile(path)) {
      throw new FileSystemException((new StringBuilder()).append("File ").append(path).append(" already exists").toString());
    } else {
      Assert.assertFalse(myImpl.add(path, true, true), (new StringBuilder()).append("Path ").append(path).append(" already denotes a directory").toString());
    }
  }

  public void writeFile(String path) {
    Assert.assertFalse(myImpl.add(path, true, false), (new StringBuilder()).append("Path ").append(path).append(" already denotes a directory").toString());
  }

  public void deleteFile(String path) {
    Assert.assertTrue(myImpl.remove(path, true));
  }

  public void createDirectory(String path) {
    if (containsNode(path)) {
      throw new FileSystemException((new StringBuilder()).append("Directory ").append(path).append(" already exists").toString());
    } else {
      Assert.assertFalse(myImpl.add(path, false, true), (new StringBuilder()).append("Path ").append(path).append(" already denotes a file").toString());
    }
  }

  public void deleteDirectory(String path) {
    Assert.assertTrue(myImpl.remove(path, false));
  }

  public boolean containsFile(String path) {
    return myImpl.contains(path, true);
  }

  public boolean containsNewFile(String path) {
    return myImpl.contains(path, true, true);
  }

  public boolean containsModifiedFile(String path) {
    return myImpl.contains(path, true, false);
  }

  public boolean containsDirectory(String path) {
    return myImpl.contains(path, false);
  }

  public boolean containsNode(String path) {
    return myImpl.containsNode(path);
  }

  public boolean containsAncestor(String path) {
    return myImpl.containsAncestor(path);
  }

  public void toCollections(Collection newFiles, Collection modifiedFiles, Collection newDirectories) {
    myImpl.toCollections(newFiles, modifiedFiles, newDirectories);
  }

  public static boolean checkPath(String path) {
    return path.length() != 0 && !path.startsWith("/") && !path.endsWith("/") && !path.contains("//");
  }

  private MemoryFileSystemImpl myImpl;
}
