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

import jetbrains.buildServer.vcs.VcsException;
import static jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection.ROOT;
import static jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection.ROOT_PREFIX;
import static jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection.SEPARATOR;

/**
 * User: vbedrosova
 * Date: 27.10.2009
 * Time: 16:49:51
 */
public class VaultCache {
  public static final String CACHE_SEPARATOR = "_";

  private static File ourCachesDir = null;

  public static void enableCache(File cachesDir) {
    ourCachesDir = cachesDir;
  }

  public static boolean cacheEnabled() {
    return ourCachesDir != null;
  }

  public static File getCache(@NotNull String repoPath, long version) throws VcsException {
    if (ourCachesDir == null) {
      throw new VcsException("Unable to get cache for " + repoPath + " at version " + version +
                             ", file caching is disabled");
    }
    String cacheName;
    if (ROOT.equals(repoPath)) {
      cacheName = "root";
    } else if (repoPath.startsWith(ROOT_PREFIX)) {
      cacheName = repoPath.substring(ROOT_PREFIX.length()).replace(SEPARATOR, CACHE_SEPARATOR);
    } else {
      throw new VcsException("Unable to get cache for " + repoPath + " at version " + version +
                             ", repo path must start with " + ROOT);
    }
    return new File(ourCachesDir, cacheName + CACHE_SEPARATOR + version);
  }
}
