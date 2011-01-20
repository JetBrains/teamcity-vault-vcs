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

package jetbrains.buildServer.vcs.patches;

import jetbrains.buildServer.vcs.VcsChange;
import jetbrains.buildServer.vcs.VcsChangeInfo;
import jetbrains.buildServer.vcs.VcsException;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class ChangesPatchBuilderTest extends TestCase {
  private ChangesPatchBuilder myPatchBuilder = null;
  private final ChangesPatchBuilder.FileContentProvider myFileContentProvider = new ChangesPatchBuilder.FileContentProvider() {

    public File getFile(@NotNull String path, @NotNull String version) throws VcsException {
      return new File("changes-patch-builder-test/testData/file1");
    }
  };
  private PatchBuilderMock myPatchBuilderMock = null;

  protected void setUp() throws Exception {
    myPatchBuilderMock = new PatchBuilderMock();
    myPatchBuilder = new ChangesPatchBuilder();
  }

  /*************************************************************************************************
   * Simple tests.
   ************************************************************************************************/

  /**
   * Add a single file.
   */
  public void testSimple1() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo/bar", "foo/bar", "0", "1")
    }, new String[] {
      "CREATE foo/bar 1"
    });
  }

  /**
   * Delete a single file.
   */
  public void testSimple2() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "foo/bar", "foo/bar", "0", "1")
    }, new String[] {
      "DELETE foo/bar"
    });
  }

  /**
   * Modify a single file.
   */
  public void testSimple3() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "foo/bar", "foo/bar", "0", "1")
    }, new String[] {
      "WRITE foo/bar 1"
    });
  }

  /**
   * Add a single directory.
   */
  public void testSimple4() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo/bar", "foo/bar", "0", "1")
    }, new String[] {
      "CREATE_DIR foo/bar"
    });
  }

  /**
   * Delete a single directory.
   */
  public void testSimple5() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo/bar", "foo/bar", "0", "1")
    }, new String[] {
      "DELETE_DIR foo/bar"
    });
  }

  /**
   * Three independent actions.
   * File creation should take place after directory creation.
   */
  public void testSimple6() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo/bar", "foo/bar", "1", "2"),
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "foobar", "foobar", "2", "3")
    }, new String[] {
      "DELETE foobar",
      "CREATE_DIR foo",
      "CREATE foo/bar 2"
    });
  }

  /**
   * Add and delete a single file two times.
   * Nothing should be done.
   */
  public void testSimple7() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "foo", "foo", "1", "2"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo", "foo", "2", "3"),
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "foo", "foo", "3", "4")
    }, new String[] {
    });
  }

  /**
   * Modify a single file two times.
   * File should be written just once.
   */
  public void testSimple8() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "foo/bar", "foo/bar", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "foo/bar", "foo/bar", "1", "2")
    }, new String[] {
      "WRITE foo/bar 2"
    });
  }

  /**
   * Add and modify a single file.
   * Only one action should take place: file creation.
   */
  public void testSimple9() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo/bar", "foo/bar", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "foo/bar", "foo/bar", "1", "2")
    }, new String[] {
      "CREATE foo/bar 2"
    });
  }

  /**
   * Create and delete a single directory.
   * Nothing should be done.
   */
  public void testSimple10() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo", "foo", "1", "2")
    }, new String[] {
    });
  }

  /**
   * Create and delete a file, then delete a parent directory.
   * First two actions should be ignored.
   */
  public void testSimple11() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo/bar", "foo/bar", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "foo/bar", "foo/bar", "1", "2"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo", "foo", "2", "3"),
    }, new String[] {
      "DELETE_DIR foo"
    });
  }

  /**
   * Three actions on a single file: modify, delete and add new.
   * File should be rewritten (it must exist and its content should be updated).
   */
  public void testSimple12() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "foo/bar", "foo/bar", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "foo/bar", "foo/bar", "1", "2"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo/bar", "foo/bar", "2", "3"),
    }, new String[] {
      "WRITE foo/bar 3"
    });
  }

  /**
   * Delete the file and add the file with the same name.
   * File should be rewritten (it must exist and its content should be updated).
   */
  public void testSimple13() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "foo/bar", "foo/bar", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo/bar", "foo/bar", "1", "2"),
    }, new String[] {
      "WRITE foo/bar 2"
    });
  }

  /**
   * Delete the directory, then create a new directory with the same name.
   * Both actions should take place.
   */
  public void testSimple14() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo", "foo", "1", "2"),
    }, new String[] {
      "DELETE_DIR foo",
      "CREATE_DIR foo"
    });
  }

  /**
   * Delete the file and the parent directory.
   * Only the last action should take place.
   */
  public void testSimple15() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo/bar", "foo/bar", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo", "foo", "1", "2"),
    }, new String[] {
      "DELETE_DIR foo"
    });
  }

  /**
   * Add and modify some files inside a directory, then delete it.
   * Only the deletion should take place.
   */
  public void testSimple16() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo/bar/baz", "foo/bar/baz", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "foo/Bar", "foo/Bar", "1", "2"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo", "foo", "2", "3"),
    }, new String[] {
      "DELETE_DIR foo"
    });
  }

  /**
   * Delete the inner directory, then create a parent.
   * Both actions should take place.
   */
  public void testSimple17() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo/bar", "foo/bar", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo", "foo", "1", "2"),
    }, new String[] {
      "DELETE_DIR foo/bar",
      "CREATE_DIR foo"
    });
  }

  /**
   * Delete the directory, then add and delete again.
   * First two actions should be ignored.
   */
  public void testSimple18() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo", "foo", "1", "2"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo", "foo", "2", "3"),
    }, new String[] {
      "DELETE_DIR foo"
    });
  }

  /**
   * Delete and add the directory two times.
   * First two actions should be ignored.
   */
  public void testSimple19() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo", "foo", "1", "2"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo", "foo", "2", "3"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo", "foo", "3", "4"),
    }, new String[] {
      "DELETE_DIR foo",
      "CREATE_DIR foo"
    });
  }

  /**
   * Add and delete some directories, then delete the parent.
   * First two actions should be ignored.
   */
  public void testSimple20() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo/bar", "foo/bar", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo/bar", "foo/bar", "1", "2"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo", "foo", "2", "3"),
    }, new String[] {
      "DELETE_DIR foo"
    });
  }

  /**
   * Modify the file, then delete it.
   * First action should be ignored.
   */
  public void testSimple21() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "foo/bar", "foo/bar", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "foo/bar", "foo/bar", "1", "2"),
    }, new String[] {
      "DELETE foo/bar"
    });
  }

  /*************************************************************************************************
   * Incorrect change sets.
   ************************************************************************************************/

  /**
   * Create the same file two times in a row.
   */
  public void testIncorrectChangeSet1() throws Exception {
    checkIncorrectPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo/bar", "foo/bar", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo/bar", "foo/bar", "1", "2"),
    });
  }

  /**
   * Modify the file, then create it.
   */
  public void testIncorrectChangeSet2() throws Exception {
    checkIncorrectPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "foo/bar", "foo/bar", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo/bar", "foo/bar", "1", "2"),
    });
  }

  /**
   * Delete the same file two times in a row.
   */
  public void testIncorrectChangeSet3() throws Exception {
    checkIncorrectPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "foo/bar", "foo/bar", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "foo/bar", "foo/bar", "1", "2"),
    });
  }

  /**
   * Add the same directory two times in a row.
   */
  public void testIncorrectChangeSet4() throws Exception {
    checkIncorrectPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo/bar", "foo/bar", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo/bar", "foo/bar", "1", "2"),
    });
  }

  /**
   * Delete the same directory two times in a row.
   */
  public void testIncorrectChangeSet5() throws Exception {
    checkIncorrectPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo/bar", "foo/bar", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo/bar", "foo/bar", "1", "2"),
    });
  }

  /**
   * Delete a directory, then try to create a file inside.
   */
  public void testIncorrectChangeSet6() throws Exception {
    checkIncorrectPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo/bar", "foo/bar", "1", "2"),
    });
  }

  /**
   * Delete a directory, then try to modify a file inside.
   */
  public void testIncorrectChangeSet7() throws Exception {
    checkIncorrectPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "foo/bar", "foo/bar", "1", "2"),
    });
  }

  /**
   * Delete a directory, then try to delete a file inside.
   */
  public void testIncorrectChangeSet8() throws Exception {
    checkIncorrectPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "foo/bar", "foo/bar", "1", "2"),
    });
  }

  /**
   * Create a file, then try to create a file inside (as if it was a directory).
   */
  public void testIncorrectChangeSet9() throws Exception {
    checkIncorrectPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo/bar", "foo/bar", "1", "2"),
    });
  }

  /**
   * Create a file, then try to create a directory with the same name.
   */
  public void testIncorrectChangeSet10() throws Exception {
    checkIncorrectPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo", "foo", "1", "2"),
    });
  }

  /**
   * Modify a file, then try to create a directory with the same name.
   */
  public void testIncorrectChangeSet11() throws Exception {
    checkIncorrectPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo", "foo", "1", "2"),
    });
  }

  /**
   * Create a directory, then try to create a file with the same name.
   */
  public void testIncorrectChangeSet12() throws Exception {
    checkIncorrectPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo", "foo", "1", "2"),
    });
  }

  /**
   * Create a directory, then try to modify a file with the same name.
   */
  public void testIncorrectChangeSet13() throws Exception {
    checkIncorrectPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "foo", "foo", "1", "2"),
    });
  }

  /**
   * Create a directory, then try to create a parent directory.
   */
  public void testIncorrectChangeSet14() throws Exception {
    checkIncorrectPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo/bar", "foo/bar", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo", "foo", "1", "2"),
    });
  }

  /**
   * Delete a directory, then try to create a directory inside it.
   */
  public void testIncorrectChangeSet15() throws Exception {
    checkIncorrectPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo/bar", "foo/bar", "1", "2"),
    });
  }

  /**
   * Delete a directory, then try to delete a directory inside it.
   */
  public void testIncorrectChangeSet16() throws Exception {
    checkIncorrectPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo/bar", "foo/bar", "1", "2"),
    });
  }

  /**
   * Delete the file, then try to modify it.
   */
  public void testIncorrectChangeSet17() throws Exception {
    checkIncorrectPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "foo", "foo", "1", "2"),
    });
  }

  /*************************************************************************************************
   * Complex scenarios.
   ************************************************************************************************/

  public void testComplex1() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "foo", "foo", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo/bar", "foo/bar", "1", "2"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo/baz", "foo/baz", "2", "3"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "foo", "foo", "3", "4"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "foo/bar/baz", "foo/bar/baz", "4", "5"),
    }, new String[] {
      "CREATE foo/bar/baz 5"
    });
  }

  public void testComplex2() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "a/b/c/d", "a/b/c/d", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "a/b/c/e", "a/b/c/e", "1", "2"),
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "a/b/c/e/f", "a/b/c/e/f", "2", "3"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "a/b", "a/b", "3", "4"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "a/b", "a/b", "4", "5"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "a/b/c", "a/b/c", "5", "6"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "a/b/c/d", "a/b/c/d", "6", "7"),
    }, new String[] {
      "DELETE_DIR a/b",
      "CREATE_DIR a/b",
      "CREATE_DIR a/b/c",
      "CREATE_DIR a/b/c/d"
    });
  }

  public void testComplex3() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "a/b/c/d", "a/b/c/d", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "a/b/c/e", "a/b/c/e", "1", "2"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "a/b/c/e/f", "a/b/c/e/f", "2", "3"),
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "a/b/c/d", "a/b/c/d", "3", "4"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "a/b/c/e", "a/b/c/e", "4", "5"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "a/b/c/e", "a/b/c/e", "5", "6"),

    }, new String[] {
      "CREATE a/b/c/d 4",
      "CREATE a/b/c/e 6"
    });
  }

  public void testComplex4() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "src/foo/bar/NewFile.java", "src/foo/bar/NewFile.java", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "src/foo/bar/OldFile1.java", "src/foo/bar/OldFile1.java", "1", "2"),
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "src/foo/bar/OldFile2.java", "src/foo/bar/OldFile2.java", "2", "3"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "src/foo/baz", "src/foo/baz", "3", "4"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "src/foo/baz/AnotherNewFile.java", "src/foo/baz/AnotherNewFile.java", "4", "5"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "test/foo/baz", "test/foo/baz", "5", "6"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "test/foo/baz/AnotherNewFileTest.java", "test/foo/baz/AnotherNewFileTest.java", "6", "7"),
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "test/foo/bar/OldFile1.java", "test/foo/bar/OldFile1.java", "7", "8"),
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "test/foo/bar/OldFile2.java", "test/foo/bar/OldFile2.java", "8", "9"),
    }, new String[] {
      "CREATE_DIR src/foo/baz",
      "CREATE_DIR test/foo/baz",
      "CREATE src/foo/bar/NewFile.java 1",
      "CREATE src/foo/baz/AnotherNewFile.java 5",
      "CREATE test/foo/baz/AnotherNewFileTest.java 7",
      "WRITE src/foo/bar/OldFile1.java 2",
      "WRITE src/foo/bar/OldFile2.java 3",
      "WRITE test/foo/bar/OldFile1.java 8",
      "WRITE test/foo/bar/OldFile2.java 9"
    });
  }

  public void testComplex5() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "src/foo/File1.java", "src/foo/File1.java", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "src/foo/File2.java", "src/foo/File2.java", "1", "2"),
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "src/foo/File3.java", "src/foo/File3.java", "2", "3"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "src/foo/bar/baz", "src/foo/bar/baz", "3", "4"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "src/foo/bar", "src/foo/bar", "4", "5"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "src/foo", "src/foo", "5", "6"),
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "test/foo/File1Test.java", "test/foo/File1Test.java", "6", "7"),
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "test/foo/File2Test.java", "test/foo/File2Test.java", "7", "8"),
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "test/foo/File3Test.java", "test/foo/File3Test.java", "8", "9"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "test/foo/bar", "test/foo/bar", "9", "10"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "test/foo", "test/foo", "10", "11"),
    }, new String[] {
      "DELETE_DIR src/foo",
      "DELETE_DIR test/foo",
    });
  }

  public void testComplex6() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "src/foo/File1.java", "src/foo/File1.java", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "src/foo", "src/foo", "1", "2"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "src/bar/File2.java", "src/bar/File2.java", "2", "3"),
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "src/bar/File3.java", "src/bar/File3.java", "3", "4"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "src/foo", "src/foo", "4", "5"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "src/foo/bar", "src/foo/bar", "5", "6"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "src/foo/bar/File4.java", "src/foo/bar/File4.java", "6", "7"),
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "src/foo/bar/File4.java", "src/foo/bar/File4.java", "7", "8"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "src/baz", "src/baz", "8", "9"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED  , null, "src", "src", "9", "10"),
    }, new String[] {
      "DELETE_DIR src"
    });
  }

  public void testComplex7() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "src", "src", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "src", "src", "1", "2"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "src/foo", "src/foo", "2", "3"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "src/foo/bar", "src/foo/bar", "3", "4"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "src/foo/bar/File.java", "src/foo/bar/File.java", "4", "5"),
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "src/foo/bar/File.java", "src/foo/bar/File.java", "5", "6"),
      new VcsChange(VcsChangeInfo.Type.REMOVED, null, "src/foo/bar/File.java", "src/foo/bar/File.java", "6", "7"),
    }, new String[] {
      "DELETE_DIR src",
      "CREATE_DIR src",
      "CREATE_DIR src/foo",
      "CREATE_DIR src/foo/bar",
    });
  }

  public void testComplex8() throws Exception {
    checkPatch(new VcsChange[] {
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "src", "src", "0", "1"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "src/foo", "src/foo", "1", "2"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "src/foo/bar", "src/foo/bar", "2", "3"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "src/foo/bar/File.java", "src/foo/bar/File.java", "3", "4"),
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "src/foo/bar/File.java", "src/foo/bar/File.java", "4", "5"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "src/foo/bar/File2.java", "src/foo/bar/File2.java", "5", "6"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_REMOVED, null, "src", "src", "6", "7"),
      new VcsChange(VcsChangeInfo.Type.DIRECTORY_ADDED, null, "src", "src", "7", "8"),
      new VcsChange(VcsChangeInfo.Type.ADDED, null, "src/foo", "src/foo", "8", "9"),
      new VcsChange(VcsChangeInfo.Type.CHANGED, null, "src/foo", "src/foo", "9", "10"),
    }, new String[] {
      "CREATE_DIR src",
      "CREATE src/foo 10"
    });
  }

  /*************************************************************************************************
   * Helper methods.
   ************************************************************************************************/

  /**
   * Runs the patcher on <code>changeArray</code> and compares the actual result with
   * <code>expectedArray</code>.
   *
   * @param changeArray the input VCS changes
   * @param expectedArray expected commands
   */
  private void checkPatch(VcsChange[] changeArray, String[] expectedArray) throws Exception {
    ArrayList<VcsChange> changes = new ArrayList<VcsChange>();
    Collections.addAll(changes, changeArray);

    myPatchBuilder.buildPatch(myPatchBuilderMock, changes, myFileContentProvider, true);
    ArrayList<String> actual = myPatchBuilderMock.getOperations();
    ArrayList<String> expected = new ArrayList<String>();
    Collections.addAll(expected,  expectedArray);

    // Since there are several possible answers (in general), this check
    // can fail for the correct answer. In this case you have to update the
    // expected array accordingly.
    assertEquals(expected, actual);
  }

  /**
   * Runs the patcher on <code>changeArray</code> and checks if an exception is thrown.
   *
   * @param changeArray the input VCS changes
   */
  private void checkIncorrectPatch(VcsChange[] changeArray) throws Exception {
    ArrayList<VcsChange> changes = new ArrayList<VcsChange>();
    Collections.addAll(changes, changeArray);

    try {
      myPatchBuilder.buildPatch(myPatchBuilderMock, changes, myFileContentProvider, true);
      fail("Patch succeeded, but should've been failed");
    } catch (VcsException e) {
      /* empty */
    }
  }
}
