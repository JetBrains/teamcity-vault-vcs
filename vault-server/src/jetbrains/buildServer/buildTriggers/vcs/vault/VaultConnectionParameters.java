/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
import jetbrains.buildServer.vcs.VcsRoot;

import java.util.Map;
import java.io.File;

/**
 * User: vbedrosova
 * Date: 22.09.2009
 * Time: 16:57:57
 */
public final class VaultConnectionParameters {
  public static final String SERVER = "vault.server";
  public static final String REPO = "vault.repo";
  public static final String USER = "vault.user";
  public static final String PASSWORD = "secure:vault.password";

  @NotNull private final String myUrl;
  @NotNull private final String myRepoName;
  @NotNull private final String myUser;
  @NotNull private final String myPassword;

  public VaultConnectionParameters(@NotNull Map<String, String> properties) {
    myUrl = properties.get(SERVER);
    myRepoName = properties.get(REPO);
    myUser = properties.get(USER);
    myPassword = properties.get(PASSWORD);
  }

  public VaultConnectionParameters(@NotNull VcsRoot root) {
    this(root.getProperties());
  }

  @NotNull public String getUrl() {
    return myUrl;
  }

  @NotNull public String getRepoName() {
    return myRepoName;
  }

  @NotNull public String getUser() {
    return myUser;
  }

  @NotNull public String getPassword() {
    return myPassword;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VaultConnectionParameters that = (VaultConnectionParameters) o;

    if (!myPassword.equals(that.myPassword)) return false;
    if (!myRepoName.equals(that.myRepoName)) return false;
    if (!myUrl.equals(that.myUrl)) return false;
    if (!myUser.equals(that.myUser)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myUrl.hashCode();
    result = 31 * result + myRepoName.hashCode();
    result = 31 * result + myUser.hashCode();
    result = 31 * result + myPassword.hashCode();
    return result;
  }
}
