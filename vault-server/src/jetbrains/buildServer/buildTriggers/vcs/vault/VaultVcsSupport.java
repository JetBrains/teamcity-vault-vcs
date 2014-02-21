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

package jetbrains.buildServer.buildTriggers.vcs.vault;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jetbrains.buildServer.buildTriggers.vcs.AbstractVcsPropertiesProcessor;
import jetbrains.buildServer.parameters.ReferencesResolverUtil;
import jetbrains.buildServer.serverSide.CachePaths;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.vcs.*;
import jetbrains.buildServer.vcs.patches.PatchBuilder;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * User: vbedrosova
 * Date: 15.05.2009
 * Time: 18:22:17
 */
public final class VaultVcsSupport extends ServerVcsSupport implements CollectSingleStateChangesByIncludeRules,
                                                                       BuildPatchByIncludeRules,
                                                                       TestConnectionSupport,
                                                                       LabelingSupport,
                                                                       ListDirectChildrenPolicy,
                                                                       UrlSupport {
  private static final Logger LOG = Logger.getLogger(VaultVcsSupport.class);

  private static final String HTTP_PEFIX = "http://";
  private static final String HTTPS_PEFIX = "https://";
  private static final String VAULT_SERVICE_SUFFIX = "/VaultService";

  @NotNull
  private final VaultConnectionFactory myConnectionFactory;
  @NotNull
  private final File myCacheFolder;

  public VaultVcsSupport(@NotNull CachePaths cachePaths, @NotNull VaultConnectionFactory connectionFactory) {
    LOG.debug("Vault plugin is working");
    myCacheFolder = cachePaths.getCacheDirectory("vault");
    myConnectionFactory = connectionFactory;
  }

  //-------------------------------------------------------------------------------
  // from VcsSupportContext

  @NotNull
  public VcsFileContentProvider getContentProvider() {
    return new VcsFileContentProvider() {
      @NotNull
      public byte[] getContent(@NotNull VcsModification vcsModification, @NotNull VcsChangeInfo change, @NotNull VcsChangeInfo.ContentType contentType, @NotNull VcsRoot vcsRoot) throws VcsException {
        return getContent(
          change.getRelativeFileName(),
          vcsRoot,
          contentType == VcsChangeInfo.ContentType.BEFORE_CHANGE ? change.getBeforeChangeRevisionNumber() : change.getAfterChangeRevisionNumber());
      }

      @NotNull
      public byte[] getContent(@NotNull final String filePath, @NotNull VcsRoot versionedRoot, @NotNull final String version) throws VcsException {
        final VaultConnection connection = getOrCreateConnection(versionedRoot);

        try {
          final File object = connection.getExistingObject(filePath, version);
          return FileUtil.loadFileBytes(object);
        } catch (IOException e) {
          throw new VcsException(e);
        } finally {
          connection.resetCaches();
        }
      }
    };
  }

  @NotNull
  public CollectChangesPolicy getCollectChangesPolicy() {
    return this;
  }

  @NotNull
  public BuildPatchPolicy getBuildPatchPolicy() {
    return this;
  }

  @Override
  @Nullable
  public TestConnectionSupport getTestConnectionSupport() {
    return this;
  }

  @Override
  @Nullable
  public LabelingSupport getLabelingSupport() {
    return this;
  }

  @Nullable
  @Override
  public ListFilesPolicy getListFilesPolicy() {
    return this;
  }

  @Nullable
  @Override
  public UrlSupport getUrlSupport() {
    return this;
  }

  //  VcsSupportCore 	getCore(); default this
//  VcsPersonalSupport 	getPersonalSupport(); default null
//  RootMerger 	getRootMerger(); default null

  // end from VcsSupportContext
  //-------------------------------------------------------------------------------


  //-------------------------------------------------------------------------------
  // from VcsSupportCore

  @NotNull
  @Override
  @SuppressWarnings("deprecation")
  public String getCurrentVersion(@NotNull VcsRoot root) throws VcsException {
    return getOrCreateConnection(root).getFolderVersion(VaultUtil.ROOT);
  }

  public boolean sourcesUpdatePossibleIfChangesNotFound(@NotNull VcsRoot root) {
    return false; //TODO: false for now till labeling not supported
  }

  @Override
  public boolean isCurrentVersionExpensive() {
    return true;
  }

  @Override
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
    return "SourceGear Vault (experimental)";
  }

  public PropertiesProcessor getVcsPropertiesProcessor() {
    return new AbstractVcsPropertiesProcessor() {
      public Collection<InvalidProperty> process(final Map<String, String> properties) {
        final List<InvalidProperty> invalids = new ArrayList<InvalidProperty>();
        String prop;
        prop = properties.get(VaultUtil.SERVER);
        if (isEmpty(prop)) {
          invalids.add(new InvalidProperty(VaultUtil.SERVER, "Vault server URL must be specified"));
        } else if (!ReferencesResolverUtil.mayContainReference(prop) && (!prop.startsWith(HTTP_PEFIX) && !prop.startsWith(HTTPS_PEFIX) || !prop.endsWith(VAULT_SERVICE_SUFFIX))) {
          invalids.add(new InvalidProperty(VaultUtil.SERVER,
            "Vault server URL must have http://hostname[:port]/VaultService or https://hostname[:port]/VaultService structure"));
        }
        prop = properties.get(VaultUtil.REPO);
        if (isEmpty(prop)) {
          invalids.add(new InvalidProperty(VaultUtil.REPO, "The repository name must be specified"));
        }
        prop = properties.get(VaultUtil.USER);
        if (isEmpty(prop)) {
          invalids.add(new InvalidProperty(VaultUtil.USER, "The user name must be specified"));
        }
        return invalids;
      }
    };
  }

  @NotNull
  public String getVcsSettingsJspFilePath() {
    return "vaultSettings.html";
  }

  @NotNull
  public String describeVcsRoot(@NotNull VcsRoot vcsRoot) {
    final String server = vcsRoot.getProperty(VaultUtil.SERVER); 
    return (server == null) ? "vault" : ("vault: " + server);
  }

  public Map<String, String> getDefaultVcsProperties() {
    return new HashMap<String, String>();
  }

  @NotNull
  public String getVersionDisplayName(@NotNull String version, @NotNull VcsRoot root) throws VcsException {
    final Long folderVersion = getOrCreateConnection(root).getFolderDisplayVersion(VaultUtil.ROOT, version);

    if (folderVersion == null) throw new VcsException("Unexisting $ revision " + version);

    return String.valueOf(folderVersion);
  }

  @NotNull
  public Comparator<String> getVersionComparator() {
    return new VcsSupportUtil.IntVersionComparator();
  }

  @NotNull
  @Override
  public Map<String, String> getCheckoutProperties(@NotNull final VcsRoot root) {
    return new VaultConnectionParameters(root, myCacheFolder).asMap();
  }

  //  boolean 	isAgentSideCheckoutAvailable(); default false

  // end from VcsSupportConfig
  //-------------------------------------------------------------------------------


  //-------------------------------------------------------------------------------
  // from CollectChangesByIncludeRules

  @NotNull
  public IncludeRuleChangeCollector getChangeCollector(@NotNull final VcsRoot root,
                                                       @NotNull final String fromVersion,
                                                       @Nullable final String currentVersion) throws VcsException {
    final VaultConnection connection = getOrCreateConnection(root);

    return new IncludeRuleChangeCollector() {
      @NotNull
      public List<ModificationData> collectChanges(@NotNull final IncludeRule includeRule) throws VcsException {
        final String targetPath = includeRule.getFrom();

        if (connection.objectExists(targetPath, null)) {
          final String toVersion = currentVersion == null ? connection.getFolderVersion(targetPath) : currentVersion;

          if (fromVersion.equals(toVersion)) {
            return Collections.emptyList();
          }

          return VaultUtil.groupChanges(root, new VaultChangeCollector(connection, fromVersion, toVersion, targetPath).collectChanges());
        }

        return Collections.emptyList();
      }

      public void dispose() {
//        try to preserve caches for patch building
//        connection.resetCaches();
      }
    };
  }

  // end from CollectChangesByIncludeRules
  //-------------------------------------------------------------------------------


  //-------------------------------------------------------------------------------
  // from BuildPatchByIncludeRules

  @NotNull
  public IncludeRulePatchBuilder getPatchBuilder(@NotNull final VcsRoot root, @Nullable final String fromVersion, @NotNull final String toVersion) {
    final VaultConnection connection = getOrCreateConnection(root);

    return new IncludeRulePatchBuilder() {
      public void buildPatch(@NotNull final PatchBuilder builder, @NotNull final IncludeRule includeRule) throws VcsException, IOException {
        final String targetPath = includeRule.getFrom();

        if (connection.objectExists(targetPath, null)) {
          final VaultPatchBuilder patchBuilder = new VaultPatchBuilder(connection, builder, targetPath);

          if (StringUtil.isNotEmpty(fromVersion)) {
            //noinspection ConstantConditions
            if (!fromVersion.equals(toVersion)) {
             patchBuilder.buildIncrementalPatch(fromVersion, toVersion);
            }
          } else {
            patchBuilder.buildCleanPatch(toVersion);
          }
        }
      }

      public void dispose() throws VcsException {
        connection.resetCaches();
      }
    };
  }

  // end from BuildPatchByIncludeRules
  //-------------------------------------------------------------------------------

  // end from PropertiesProcessor
  //-------------------------------------------------------------------------------

  //-------------------------------------------------------------------------------
  // from TestConnectionSupport

  public String testConnection(@NotNull VcsRoot vcsRoot) throws VcsException {
    final VaultConnection connection = getOrCreateConnection(vcsRoot);
    return "$ at revision " + connection.getFolderDisplayVersion(VaultUtil.ROOT, connection.getFolderVersion(VaultUtil.ROOT));
  }

  // end from TestConnectionSupport
  //-------------------------------------------------------------------------------

  //-------------------------------------------------------------------------------
  // from LabelingSupport

  @NotNull
  public String label(@NotNull String label,
                      @NotNull String version,
                      @NotNull VcsRoot root,
                      @NotNull CheckoutRules checkoutRules) throws VcsException {
    if (StringUtil.isEmpty(label)) throw new VcsException("Label is empty");

    for (final IncludeRule rule : checkoutRules.getIncludeRules()) {
      getOrCreateConnection(root).labelFolder(rule.getFrom(), version, label);
    }

    return label;
  }

  // end from LabelingSupport
  //-------------------------------------------------------------------------------


  //-------------------------------------------------------------------------------
  // from ListDirectChildrenPolicy

  @NotNull
  public Collection<VcsFileData> listFiles(@NotNull final VcsRoot root, @NotNull final String directoryPath) throws VcsException {
    final VaultConnection connection = getOrCreateConnection(root);
    final File folder = connection.getExistingObject(directoryPath, connection.getFolderVersion(directoryPath));

    final File[] files = folder.listFiles();

    return files == null ? Collections.<VcsFileData>emptyList() : CollectionsUtil.convertCollection(Arrays.asList(files), new Converter<VcsFileData, File>() {
      public VcsFileData createFrom(@NotNull final File source) {
        return new VcsFileData(source.getName(), source.isDirectory());
      }
    });
  }

  // end from ListDirectChildrenPolicy
  //-------------------------------------------------------------------------------

  //-------------------------------------------------------------------------------
  // from UrlSupport

  @Nullable
  public Map<String, String> convertToVcsRootProperties(@NotNull final VcsUrl vcsUrl) throws VcsException {
    if (vcsUrl.getCredentials() == null) return null;

    try {
      final String url = vcsUrl.getUrl();
      final String server = getServer(url);

      if (StringUtil.isNotEmpty(server)) {

        final String repoIdStr = getRepoId(url);
        if (StringUtil.isNotEmpty(repoIdStr)) {

          final int repoId = Integer.parseInt(repoIdStr);
          final String username = vcsUrl.getCredentials().getUsername();
          final String password = vcsUrl.getCredentials().getPassword();

          final List<RepositoryInfo> repos = myConnectionFactory.getOrCreateConnection(
            new VaultConnectionParameters(server,
                                          StringUtil.EMPTY,
                                          username,
                                          password,
                                          vcsUrl.toString(),
                                          myCacheFolder)).getRepositories();
          for (RepositoryInfo repo : repos) {
            if (repo.getId() == repoId) {
              final Map<String, String> res = new HashMap<String, String>(4);
              res.put(VaultUtil.SERVER, server);
              res.put(VaultUtil.REPO, repo.getName());
              res.put(VaultUtil.USER, username);
              res.put(VaultUtil.PASSWORD, password);
              return res;
            }
          }
        }
      }
    } catch (Throwable t) {
      LOG.warn(t.getMessage(), t);
      throw new VcsException(t.getMessage(), t);
    }
    return null;
  }

  @Nullable
  private String getServer(@NotNull String url) {
    final Matcher matcher = Pattern.compile("^((http://|https://)[^/]+/VaultService).+$").matcher(url);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    return null;
  }

  @Nullable
  private String getRepoId(@NotNull String url) {
    final Matcher matcher = Pattern.compile(".+[?&]repid=(\\d+)(&.+)*+$").matcher(url);
    if (matcher.matches()) {
      return matcher.group(1);
    }
    return null;
  }

  // end from UrlSupport
  //-------------------------------------------------------------------------------

  @NotNull
  private VaultConnection getOrCreateConnection(@NotNull VcsRoot root) {
    return myConnectionFactory.getOrCreateConnection(new VaultConnectionParameters(root, myCacheFolder));
  }
}
