/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package jetbrains.buildServer.buildTriggers.vcs.vault.impl;

import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnection;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionFactory;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultConnectionParameters;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Victory.Bedrosova on 9/25/13.
 */
@SuppressWarnings("UnusedDeclaration")
public class VaultConnectionFactoryImpl implements VaultConnectionFactory {
  @NotNull
  public VaultConnection getOrCreateConnection(@NotNull VaultConnectionParameters parameters) {
    return makeSynchronized(makeEternal(makeExceptionAware(new VaultConnectionImpl(parameters))));
  }

  @NotNull
  private ExceptionAwareConnection makeExceptionAware(@NotNull VaultConnection connection) {
    return new ExceptionAwareConnection(connection);
  }

  @NotNull
  private SynchronizedVaultConnection makeSynchronized(@NotNull VaultConnection connection) {
    return new SynchronizedVaultConnection(connection);
  }

  @NotNull
  private EternalVaultConnection makeEternal(@NotNull VaultConnection connection) {
    return new EternalVaultConnection(connection);
  }
}
