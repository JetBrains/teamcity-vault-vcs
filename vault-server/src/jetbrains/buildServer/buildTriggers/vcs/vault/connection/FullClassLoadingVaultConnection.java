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

package jetbrains.buildServer.buildTriggers.vcs.vault.connection;

import java.io.File;
import java.util.List;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionParameters;
import org.jetbrains.annotations.NotNull;

/**
 * @User Victory.Bedrosova
 * 2/19/14.
 */
class FullClassLoadingVaultConnection extends ClassLoadingVaultConnection {
  public FullClassLoadingVaultConnection(@NotNull final VaultConnectionParameters parameters, @NotNull final List<File> jars) {
    super(parameters, getClassLoader(jars));
  }

  @NotNull
  private static ClassLoader getClassLoader(@NotNull final List<File> jars) {
    return new VaultApiJarClassLoader(FullClassLoadingVaultConnection.class.getClassLoader(), jars);
  }
}
