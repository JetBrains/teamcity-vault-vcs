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

package jetbrains.buildServer.buildTriggers.vcs.vault;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.vcs.IncludeRule;
import jetbrains.buildServer.vcs.VcsException;
import jetbrains.buildServer.vcs.VcsRoot;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 07.07.2009
 * Time: 19:32:05
 */
public final class VaultUtil {
  public static final String ROOT = "$";
  public static final String ROOT_PREFIX = "$/";
  public static final String SEPARATOR = "/";
  public static final String CURRENT = ".";  

  public static final File TEMP_DIR = new File(FileUtil.getTempDirectory(), "vault");

  public static void createTempDir() {
    FileUtil.delete(TEMP_DIR);
    TEMP_DIR.mkdirs();
  }

  public static final String SERVER = "vault.server";
  public static final String REPO = "vault.repo";
  public static final String USER = "vault.user";
  public static final String PASSWORD = "secure:vault.password";

  public static final String NO_API_FOUND_EXCEPTION = "Vault integration could not find some of Vault Java API jars.";

//        public static final byte Added = 10;
//        public static final byte BranchedFrom = 20;
//        public static final byte BranchedFromItem = 30;
//        public static final byte BranchedFromShare = 40;
//        public static final byte BranchedFromShareItem = 50;
//        public static final byte CheckIn = 60;
//        public static final byte Created = 70;
//        public static final byte Deleted = 80;
//        public static final byte Label = 90;
//        public static final byte MovedFrom = 120;
//        public static final byte MovedTo = -126;
//        public static final byte Obliterated = -116;
//        public static final byte Pinned = -106;
//        public static final byte PropertyChange = -96;
//        public static final byte Renamed = -86;
//        public static final byte RenamedItem = -76;
//        public static final byte SharedTo = -66;
//        public static final byte Snapshot = -56;
//        public static final byte SnapshotFrom = -55;
//        public static final byte SnapshotItem = -54;
//        public static final byte Undeleted = -46;
//        public static final byte UnPinned = -36;
//        public static final byte Rollback = -26;

  public static final Set<String> NOT_CHANGED_CHANGE_TYPES = new HashSet<String>();

  static {
    NOT_CHANGED_CHANGE_TYPES.add("Created");

    NOT_CHANGED_CHANGE_TYPES.add("Label");

    NOT_CHANGED_CHANGE_TYPES.add("Obliterated");

    NOT_CHANGED_CHANGE_TYPES.add("Pinned");
    NOT_CHANGED_CHANGE_TYPES.add("UnPinned");

    NOT_CHANGED_CHANGE_TYPES.add("PropertyChange");

    NOT_CHANGED_CHANGE_TYPES.add("MovedFrom");

    NOT_CHANGED_CHANGE_TYPES.add("Snapshot");
    NOT_CHANGED_CHANGE_TYPES.add("SnapshotFrom");
    NOT_CHANGED_CHANGE_TYPES.add("SnapshotItem");

    NOT_CHANGED_CHANGE_TYPES.add("BranchedFrom");
    NOT_CHANGED_CHANGE_TYPES.add("BranchedFromShare");
    NOT_CHANGED_CHANGE_TYPES.add("BranchedFromShareItem");
  }

  public static final Set<String> ADDED_CHANGE_TYPES =  new HashSet<String>();

  static {
    ADDED_CHANGE_TYPES.add("Added");

    ADDED_CHANGE_TYPES.add("BranchedFromItem");

    ADDED_CHANGE_TYPES.add("MovedTo");

    ADDED_CHANGE_TYPES.add("Renamed");
    ADDED_CHANGE_TYPES.add("RenamedItem");

    ADDED_CHANGE_TYPES.add("SharedTo");

    ADDED_CHANGE_TYPES.add("Undeleted");
  }

  public static final Set<String> CHANGED_CHANGE_TYPES =  new HashSet<String>();

  static {
    CHANGED_CHANGE_TYPES.add("CheckIn");
    CHANGED_CHANGE_TYPES.add("Rollback");
  }

  public static final Set<String> REMOVED_CHANGE_TYPES =  new HashSet<String>();

  static {
    REMOVED_CHANGE_TYPES.add("Deleted");
  }

  public static long parseLong(@NotNull String num) throws VcsException {
    try {
      return Long.parseLong(num);
    } catch (NumberFormatException e) {
      throw new VcsException(e);
    }
  }

  public static void checkIncludeRule(VcsRoot root, final IncludeRule includeRule) throws VcsException {
    VaultConnection.doInConnection(root.getProperties(), new VaultConnection.InConnectionProcessor() {
      public void process() throws Throwable {
        if (!VaultConnection.objectExists(getRepoPathFromPath(includeRule.getFrom()))) {
          throw new VcsException("Invalid rule " + includeRule.toDescriptiveString()
            + ", no such repository folder");
        }
      }
    }, false);
  }

  public static String getRepoParentPath(@NotNull String repoPath) {
    return ROOT.equals(repoPath) ? "" : repoPath.substring(0, repoPath.lastIndexOf(SEPARATOR));
  }

  public static String getName(@NotNull String repoPath) {
    return ROOT.equals(repoPath) ? ROOT : repoPath.substring(repoPath.lastIndexOf("/") + 1);
  }

  public static String getRepoPathFromPath(@NotNull String path) {
    return ("".equals(path) || CURRENT.equals(path)) ? ROOT : ROOT + SEPARATOR + path.replace("\\", SEPARATOR);
  }

  public static String getPathFromRepoPath(@NotNull String repoPath) {
    return ROOT.equals(repoPath) ? "" : repoPath.substring(2);
  }
}
