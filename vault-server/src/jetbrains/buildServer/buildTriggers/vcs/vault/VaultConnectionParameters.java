/*
 * Copyright 2000-2018 JetBrains s.r.o.
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
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.vcs.VcsRoot;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Victory.Bedrosova on 8/19/13.
 */
public final class VaultConnectionParameters {
  @NotNull
  private final String myURL;
  @NotNull
  private final String myRepository;
  @NotNull
  private final String myUser;
  @NotNull
  private final String myPassword;
  @NotNull
  private final String myStringRepresentation;
  @NotNull
  private final File myCacheFolder;

  public VaultConnectionParameters(@NotNull final Map<String, String> parameters, @NotNull final String stringRepresentation, @NotNull File cacheFolder) {
    this(parameters.get(VaultUtil.SERVER), parameters.get(VaultUtil.REPO), parameters.get(VaultUtil.USER), parameters.get(VaultUtil.PASSWORD), stringRepresentation, cacheFolder);
  }

  public VaultConnectionParameters(@NotNull final VcsRoot vcsRoot, @NotNull File cacheFolder) {
    this(vcsRoot.getProperties(), vcsRoot.toString(), cacheFolder);
  }

  public VaultConnectionParameters(@NotNull String URL,
                                   @NotNull String repository,
                                   @NotNull String user,
                                   @NotNull String password,
                                   @NotNull String stringRepresentation,
                                   @NotNull File cacheFolder) {
    myURL = URL;
    myRepository = repository;
    myUser = user;
    myPassword = password;
    myStringRepresentation = stringRepresentation;
    myCacheFolder = cacheFolder;
  }

  @NotNull
  public String getURL() {
    return myURL;
  }

  @NotNull
  public String getRepository() {
    return myRepository;
  }

  @NotNull
  public String getUser() {
    return myUser;
  }

  @NotNull
  public String getPassword() {
    return myPassword;
  }

  @NotNull
  public String getStringRepresentation() {
    return myStringRepresentation;
  }

  @NotNull
  Map<String, String> asMap() {
    final Map<String, String> map = new HashMap<String, String>();
    map.put(VaultUtil.SERVER, myURL);
    map.put(VaultUtil.REPO, myRepository);
    map.put(VaultUtil.USER, myUser);
    map.put(VaultUtil.PASSWORD, myPassword);
    return map;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VaultConnectionParameters that = (VaultConnectionParameters) o;

    return myPassword.equals(that.myPassword) &&
           myRepository.equals(that.myRepository) &&
           myURL.equals(that.myURL) &&
           myUser.equals(that.myUser);
  }

  @Override
  public int hashCode() {
    int result = myURL.hashCode();
    result = 31 * result + myRepository.hashCode();
    result = 31 * result + myUser.hashCode();
    result = 31 * result + myPassword.hashCode();
    return result;
  }

  @NotNull
  public File getConnectionCacheFolder() {
    return new File(myCacheFolder, String.valueOf(hashCode()));
  }
}
