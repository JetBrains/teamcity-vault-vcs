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

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.vcs.*;
import jetbrains.buildServer.util.FileUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.io.File;


/**
 * User: vbedrosova
 * Date: 15.05.2009
 * Time: 18:22:17
 */
public final class VaultVcsSupport extends ServerVcsSupport implements CollectChangesByIncludeRules,
                                                                       BuildPatchByIncludeRules,
                                                                       PropertiesProcessor,
                                                                       TestConnectionSupport {
  private static final Logger LOG = Logger.getLogger(VaultVcsSupport.class);
  private static final String HTTP_PEFIX = "http://";
  public static final String NO_API_FOUND_MESSAGE = "Vault integration could not find Vault Java API jars.";

  private final VaultFileContentProvider myFileContentProvider;

  public VaultVcsSupport(@NotNull ServerPaths serverPaths) {
    LOG.debug("Vault plugin is working");
    myFileContentProvider = new VaultFileContentProvider();
    final File cache = new File(serverPaths.getCachesDir(), "vault");
    if (FileUtil.delete(cache)) {
      LOG.debug("Vault plugin deleted it's cache under " + cache.getAbsolutePath());
    }
    if (System.getProperty("vault.enable.cache") != null) {
      VaultCache.enableCache(cache);
    }
  }


  //-------------------------------------------------------------------------------
  // from VcsSupportContext

  @NotNull
  public VcsFileContentProvider getContentProvider() {
    return myFileContentProvider;
  }

  @NotNull
  public CollectChangesPolicy getCollectChangesPolicy() {
    return this;
  }

  @NotNull
  public BuildPatchPolicy getBuildPatchPolicy() {
    return this;
  }

  @Nullable
  public TestConnectionSupport getTestConnectionSupport() {
    return this;
  }

//  VcsSupportCore 	getCore(); default this
//  LabelingSupport 	getLabelingSupport(); default null
//  VcsPersonalSupport 	getPersonalSupport(); default null
//  RootMerger 	getRootMerger(); default null

  // end from VcsSupportContext
  //-------------------------------------------------------------------------------


  //-------------------------------------------------------------------------------
  // from VcsSupportCore

  @NotNull
  public String getCurrentVersion(@NotNull VcsRoot root) throws VcsException {
    if (!VaultApiDetector.detectApi()) {
      throw new VcsException(NO_API_FOUND_MESSAGE);
    }
    return VaultConnection.getCurrentVersion(new VaultConnectionParameters(root));
  }

  public boolean sourcesUpdatePossibleIfChangesNotFound(@NotNull VcsRoot root) {
    return false; //TODO: false for now till labeling not supported
  }

  public boolean isCurrentVersionExpensive() {
    return true;
  }

  public boolean allowSourceCaching() {
    return false;
  }

  // end from VcsSupportCore
  //--------------------------------------------------------------------------------


  //-------------------------------------------------------------------------------
  // from VcsSupportConfig

  @NotNull
  public String getName() {
    return "vault-vcs";
  }

  @NotNull
  public String getDisplayName() {
    return "Vault";
  }

  public PropertiesProcessor getVcsPropertiesProcessor() {
    return this;
  }

  @NotNull
  public String getVcsSettingsJspFilePath() {
    return "vaultSettings.jsp";
  }

  @NotNull
  public String describeVcsRoot(VcsRoot vcsRoot) {
    final String server = vcsRoot.getProperty(VaultConnectionParameters.SERVER); 
    return (server == null) ? "vault" : ("vault: " + server);
  }

  public Map<String, String> getDefaultVcsProperties() {
    return new HashMap<String, String>();
  }

  public String getVersionDisplayName(@NotNull String version, @NotNull VcsRoot root) throws VcsException {
    return version;
  }

  @NotNull
  public Comparator<String> getVersionComparator() {
    return new VcsSupportUtil.IntVersionComparator();
  }

//  boolean 	isAgentSideCheckoutAvailable(); default false

  // end from VcsSupportConfig
  //-------------------------------------------------------------------------------


  //-------------------------------------------------------------------------------
  // from CollectChangesByIncludeRules

  @NotNull
  public IncludeRuleChangeCollector getChangeCollector(@NotNull VcsRoot root,
                                                       @NotNull String fromVersion,
                                                       @Nullable String currentVersion) throws VcsException {
    if (!VaultApiDetector.detectApi()) {
      throw new VcsException(NO_API_FOUND_MESSAGE);
    }    
    return new VaultChangeCollector(root, fromVersion, currentVersion);
  }

  // end from CollectChangesByIncludeRules
  //-------------------------------------------------------------------------------


  //-------------------------------------------------------------------------------
  // from BuildPatchByIncludeRules

  @NotNull
  public IncludeRulePatchBuilder getPatchBuilder(@NotNull VcsRoot root, @Nullable String fromVersion, @NotNull String toVersion) {
    return new VaultPatchBuilder(root, fromVersion, toVersion);
  }

  // end from BuildPatchByIncludeRules
  //-------------------------------------------------------------------------------

  //-------------------------------------------------------------------------------
  // from PropertiesProcessor

  public Collection<InvalidProperty> process(Map<String, String> properties) {
    final List<InvalidProperty> invalids = new ArrayList<InvalidProperty>();
    String prop; 
    prop = properties.get(VaultConnectionParameters.SERVER);
    if ((prop == null) || ("".equals(prop))) {
      invalids.add(new InvalidProperty(VaultConnectionParameters.SERVER, "Vault server URL must be specified"));
    } else if (!prop.startsWith(HTTP_PEFIX)) {
      invalids.add(new InvalidProperty(VaultConnectionParameters.SERVER, "Vault server URL must have http://hostname[:port] structure"));
    }
    prop = properties.get(VaultConnectionParameters.REPO);
    if ((prop == null) || ("".equals(prop))) {
      invalids.add(new InvalidProperty(VaultConnectionParameters.REPO, "Repository name must be specified"));
    }
    prop = properties.get(VaultConnectionParameters.USER);
    if ((prop == null) || ("".equals(prop))) {
      invalids.add(new InvalidProperty(VaultConnectionParameters.USER, "User name must be specified"));
    }
    return invalids;
  }

  // end from PropertiesProcessor
  //-------------------------------------------------------------------------------

  //-------------------------------------------------------------------------------
  // from TestConnectionSupport

  public String testConnection(@NotNull VcsRoot vcsRoot) throws VcsException {
    if (!VaultApiDetector.detectApi()) {
      throw new VcsException(NO_API_FOUND_MESSAGE);
    }
    VaultConnection.testConnection(new VaultConnectionParameters(vcsRoot.getProperties()));
    return null;
  }

  // end from TestConnectionSupport
  //-------------------------------------------------------------------------------
}
