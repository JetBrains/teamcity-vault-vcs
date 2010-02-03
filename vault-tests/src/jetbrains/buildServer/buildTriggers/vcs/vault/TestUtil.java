/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

  public static File getTestDataMayNotExist(String fileName, String folderName) throws FileNotFoundException {
    final String path = getTestData(null, null).getPath() + (folderName != null ? File.separator + folderName : "") + (fileName != null ? File.separator + fileName : "");
    return new File(path);
  }
}
