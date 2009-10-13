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

import VaultClientNetLib.VaultConnection;


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

  private final VaultFileContentProvider myFileContentProvider;

  public VaultVcsSupport(@NotNull ServerPaths serverPaths) {
    LOG.debug("Vault plugin is working");
    myFileContentProvider = new VaultFileContentProvider();
    final File cache = new File(serverPaths.getCachesDir(), "vault");
    if (FileUtil.delete(cache)) {
      LOG.debug("Vault plugin deleted it's cache under " + cache.getAbsolutePath());
    }
    if (System.getProperty("vault.enable.cache") != null) {
      VaultConnection1.enableCache(cache);
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
    return VaultConnection1.getCurrentVersion(new VaultConnectionParameters(root));
  }

  public boolean sourcesUpdatePossibleIfChangesNotFound(@NotNull VcsRoot root) {
    return false; //TODO: false for now till labeling not supported
  }

  //  boolean isCurrentVersionExpensive(); default false
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
    return "vault: " + vcsRoot.getName();
  }

  public Map<String, String> getDefaultVcsProperties() {
    return new HashMap<String, String>();
  }

  public String getVersionDisplayName(@NotNull String version, @NotNull VcsRoot root) throws VcsException {
//TODO: how is better?
//    return VaultUtil.getDate(version).toString();
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
    List<InvalidProperty> invalids = new ArrayList<InvalidProperty>();
    String prop; 
    prop = properties.get(VaultConnectionParameters.SERVER);
    if ((prop == null) || ("".equals(prop))) {
      invalids.add(new InvalidProperty(VaultConnectionParameters.SERVER, "Vault server must not be empty"));
    }
    prop = properties.get(VaultConnectionParameters.REPO);
    if ((prop == null) || ("".equals(prop))) {
      invalids.add(new InvalidProperty(VaultConnectionParameters.REPO, "Repository name must not be empty"));
    }
    prop = properties.get(VaultConnectionParameters.USER);
    if ((prop == null) || ("".equals(prop))) {
      invalids.add(new InvalidProperty(VaultConnectionParameters.USER, "Username must not be empty"));
    }
    if (invalids.size() > 0) {
      return invalids;
    }
    if (properties.get(VaultConnectionParameters.PASSWORD) == null) {
      properties.put(VaultConnectionParameters.PASSWORD, "");
    }
    try {
      testConnection(properties);
    } catch (VcsException e) {
      invalids.add(new InvalidProperty(VaultConnectionParameters.SERVER, e.getMessage()));
    }
    return invalids;
  }

  // end from PropertiesProcessor
  //-------------------------------------------------------------------------------

  //-------------------------------------------------------------------------------
  // from TestConnectionSupport

  public String testConnection(@NotNull VcsRoot vcsRoot) throws VcsException {
    testConnection(vcsRoot.getProperties());
    return null;
  }

  // end from TestConnectionSupport
  //-------------------------------------------------------------------------------

  private void testConnection(@NotNull Map<String, String> properties) throws VcsException {
    if (!VaultApiDetector.detectApi()) {
      throw new VcsException("No Vault Java API jars are present at <TeamCity web application>/WEB-INF/lib directory. You should download them from Vault vendor manually.");
    }
    VaultConnection1.testConnection(new VaultConnectionParameters(properties));    
  }
}
