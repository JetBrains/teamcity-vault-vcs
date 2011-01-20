/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
