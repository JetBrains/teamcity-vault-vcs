/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import java.util.List;
import java.util.Map;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.vcs.ModificationData;
import jetbrains.buildServer.vcs.VcsChange;
import jetbrains.buildServer.vcs.VcsRoot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 07.07.2009
 * Time: 19:32:05
 */
public final class VaultUtil {
  public static final String ROOT = "$";
  public static final String SEPARATOR = "/";
  public static final String ROOT_PREFIX = ROOT + SEPARATOR;
  public static final String CURRENT = ".";

  public static final String SERVER = "vault.server";
  public static final String REPO = "vault.repo";
  public static final String USER = "vault.user";
  public static final String PASSWORD = "secure:vault.password";

  public static final String NO_API_FOUND_EXCEPTION = "Vault integration could not find some of Vault Java API jars.";

  public static String getRepoParentPath(@NotNull String repoPath) {
    return ROOT.equals(repoPath) ? "" : repoPath.substring(0, repoPath.lastIndexOf(SEPARATOR));
  }

  public static String getRepoPathFromPath(@NotNull String path) {
    if (path.startsWith(ROOT)) return path;
    return ("".equals(path) || CURRENT.equals(path)) ? ROOT : ROOT + SEPARATOR + path.replace("\\", SEPARATOR);
  }

  public static String getPathFromRepoPath(@NotNull String repoPath) {
    return ROOT.equals(repoPath) ? "" : (repoPath.startsWith(ROOT_PREFIX) ? repoPath.substring(2) : repoPath);
  }

  @NotNull
  public static List<ModificationData> groupChanges(@NotNull final VcsRoot root, @NotNull List<ChangeInfo> changes) {
    return CollectionsUtil.convertCollection(CollectionsUtil.groupBy(changes, new Converter<ModificationInfo, ChangeInfo>() {
      public ModificationInfo createFrom(@NotNull final ChangeInfo source) {
        return source.getModificationInfo();
      }
    }).entrySet(), new Converter<ModificationData, Map.Entry<ModificationInfo, List<ChangeInfo>>>() {
      public ModificationData createFrom(@NotNull final Map.Entry<ModificationInfo, List<ChangeInfo>> source) {
        final ModificationInfo mi = source.getKey();
        return new ModificationData(mi.getDate(), toVcsChanges(source.getValue()), mi.getComment(), mi.getUser(), root, mi.getVersion(), mi.getDisplayVersion());
      }
    });
  }

  @NotNull
  public static List<VcsChange> toVcsChanges(@NotNull List<ChangeInfo> changes) {
    return CollectionsUtil.convertCollection(changes, new Converter<VcsChange, ChangeInfo>() {
      public VcsChange createFrom(@NotNull final ChangeInfo source) {
        final ModificationInfo mi = source.getModificationInfo();
        return new VcsChange(source.getChangeType(), source.getChangeName(), source.getRelativePath(), source.getRelativePath(), mi.getPrevVersion(), mi.getVersion());
      }
    });
  }

  @NotNull
  public static String getFullRepoPathWithCommonPart(@NotNull String path, @NotNull String targetPath) {
    if (path.startsWith(ROOT)) {
      return path;
    }
    return ROOT_PREFIX + getFullPathWithCommonPart(path, targetPath);
  }

  @NotNull
  public static String getFullPathWithCommonPart(@NotNull String path, @NotNull String targetPath) {
    targetPath = targetPath.replace('\\', '/');
    if (targetPath.endsWith("/")) {
      targetPath = targetPath.substring(0, targetPath.length() - 1);
    }
    if (targetPath.contains("/")) {
      targetPath = targetPath.substring(0, targetPath.lastIndexOf("/"));
    } else {
      targetPath = StringUtil.EMPTY;
    }
    return (targetPath.length() == 0 ? "" : targetPath + "/") + path;
  }

  @NotNull
  public static String getFullPath(@NotNull String path, @NotNull String targetPath) {
    targetPath = targetPath.replace('\\', '/');
    if (targetPath.endsWith("/")) {
      targetPath = targetPath.substring(0, targetPath.length() - 1);
    }
    return (targetPath.length() == 0 ? "" : targetPath + "/") + path;
  }

  @Nullable
  public static String getRelativePath(@NotNull String path, @NotNull String targetPath) {
    final String relativePath = getPathFromRepoPath(path);

    if (StringUtil.isNotEmpty(targetPath)) {
      return relativePath.startsWith(targetPath) ? relativePath.substring(targetPath.length() + 1) : null;
    }

    return relativePath;
  }
}
