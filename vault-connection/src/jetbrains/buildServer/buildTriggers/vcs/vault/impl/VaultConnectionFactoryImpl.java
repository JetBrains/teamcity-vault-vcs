

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
    return new VaultConnectionImpl(parameters);
  }
}