/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package jetbrains.buildServer.vcs.patches.fs;

import java.util.Collection;
import jetbrains.buildServer.vcs.patches.util.Assert;
import org.apache.log4j.Logger;

/**
 * User: vbedrosova
 * Date: 22.12.2009
 * Time: 13:18:23
 */
public class MemoryFileSystem {
  private static final Logger LOG = Logger.getLogger(MemoryFileSystem.class);

  public MemoryFileSystem() {
    myImpl = new MemoryFileSystemImpl();
  }

  public void createFile(String path) {
    LOG.debug("MemoryFileSystem create file: " + path);
    if (containsFile(path)) {
      throw new FileSystemException((new StringBuilder()).append("File ").append(path).append(" already exists").toString());
    } else {
      Assert.assertFalse(myImpl.add(path, true, true), (new StringBuilder()).append("Path ").append(path).append(" already denotes a directory").toString());
    }
  }

  public void writeFile(String path) {
    LOG.debug("MemoryFileSystem write file: " + path);
    Assert.assertFalse(myImpl.add(path, true, false), (new StringBuilder()).append("Path ").append(path).append(" already denotes a directory").toString());
  }

  public void deleteFile(String path) {
    LOG.debug("MemoryFileSystem delete file: " + path);
    Assert.assertTrue(myImpl.remove(path, true));
  }

  public void createDirectory(String path) {
    LOG.debug("MemoryFileSystem create folder: " + path);
    if (containsNode(path)) {
      throw new FileSystemException((new StringBuilder()).append("Directory ").append(path).append(" already exists").toString());
    } else {
      Assert.assertFalse(myImpl.add(path, false, true), (new StringBuilder()).append("Path ").append(path).append(" already denotes a file").toString());
    }
  }

  public void deleteDirectory(String path) {
    LOG.debug("MemoryFileSystem delete folder: " + path);
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

  private final MemoryFileSystemImpl myImpl;
}
