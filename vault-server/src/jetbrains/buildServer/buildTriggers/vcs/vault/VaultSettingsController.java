

package jetbrains.buildServer.buildTriggers.vcs.vault;

import java.io.File;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.buildTriggers.vcs.vault.connection.TeamCityVaultConnectionProxy;
import jetbrains.buildServer.controllers.BaseController;
import jetbrains.buildServer.plugins.PluginManager;
import jetbrains.buildServer.serverSide.ServerPaths;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.ModelAndView;

/**
 * Created by Victory.Bedrosova on 9/30/13.
 */
public class VaultSettingsController extends BaseController {
  @NotNull
  private final File myDataDirPath;
  @NotNull
  private final String myJspPath;
  @NotNull
  private final PluginManager myPluginManager;

  public VaultSettingsController(@NotNull PluginManager pluginManager,
                                 @NotNull PluginDescriptor descriptor,
                                 @NotNull WebControllerManager web,
                                 @NotNull ServerPaths serverPaths) {
    myDataDirPath = serverPaths.getDataDirectory();
    myJspPath = descriptor.getPluginResourcesPath("vaultSettings.jsp");
    myPluginManager = pluginManager;

    web.registerController(descriptor.getPluginResourcesPath("vaultSettings.html"), this);
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) throws Exception {
    final ModelAndView mv = new ModelAndView(myJspPath);
    mv.getModel().put("vaultApiPresent", vaultApiPresent());
    mv.getModel().put("teamCityDataDir", myDataDirPath);
    return mv;
  }

  private boolean vaultApiPresent() {
    final File vaultApiFolder = TeamCityVaultConnectionProxy.getVaultApiFolder(myPluginManager);
    return vaultApiFolder == null ? false : new File(vaultApiFolder, "VaultClientOperationsLib.jar").isFile();
  }
}