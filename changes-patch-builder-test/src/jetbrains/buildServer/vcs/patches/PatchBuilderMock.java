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

package jetbrains.buildServer.vcs.patches;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 28.12.2009
 * Time: 15:58:06
 */
public class PatchBuilderMock implements PatchBuilder {
  private final ArrayList<String> myOperations = new ArrayList<String>();

  public ArrayList<String> getOperations() {
    return myOperations;
  }
  
  public void deleteFile(@NotNull File file, boolean b) throws IOException {
    myOperations.add("DELETE " + unifyPath(file.getPath()));
  }

  public void deleteDirectory(@NotNull File file, boolean b) throws IOException {
    myOperations.add("DELETE_DIR " + unifyPath(file.getPath()));
  }

  public void changeOrCreateTextFile(@NotNull File file, String s, @NotNull InputStream inputStream, long l, byte[] bytes) throws IOException {
    myOperations.add("WRITE_TEXT " + unifyPath(file.getPath()) + " " + s);
  }

  public void changeOrCreateBinaryFile(@NotNull File file, String s, @NotNull InputStream inputStream, long l) throws IOException {
    myOperations.add("WRITE " + unifyPath(file.getPath()) + " " + s);
  }

  public void createDirectory(@NotNull File file) throws IOException {
    myOperations.add("CREATE_DIR " + unifyPath(file.getPath()));
  }

  public void createBinaryFile(@NotNull File file, String s, @NotNull InputStream inputStream, long l) throws IOException {
    myOperations.add("CREATE " + unifyPath(file.getPath()) + " " + s);
  }

  public void createTextFile(@NotNull File file, String s, @NotNull InputStream inputStream, long l, byte[] bytes) throws IOException {
    myOperations.add("CREATE_TEXT " + unifyPath(file.getPath()) + " " + s);
  }

  public void renameFile(@NotNull File file, @NotNull File file1, boolean b) throws IOException {
    myOperations.add("RENAME " + unifyPath(file.getPath()));
  }

  public void renameDirectory(@NotNull File file, @NotNull File file1, boolean b) throws IOException {
    myOperations.add("RENAME_DIR " + unifyPath(file.getPath()));
  }

  public void setWorkingDirectory(@NotNull File file, boolean b) throws IOException {
    myOperations.add("SET_WORKING_DIR " + unifyPath(file.getPath()));
  }

  public void setLastModified(@NotNull File file, long l) throws IOException {
    myOperations.add("SET_LAST_MODIFIED " + unifyPath(file.getPath()));
  }

  private static String unifyPath(String path) {
    return path.replace("\\", "/");   
  }
}
