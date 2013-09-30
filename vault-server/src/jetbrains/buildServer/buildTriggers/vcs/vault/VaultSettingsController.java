package jetbrains.buildServer.buildTriggers.vcs.vault;

import java.io.File;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jetbrains.buildServer.controllers.BaseController;
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
  private final VaultApiConnector myApiConnector;
  @NotNull
  private final File myDataDirPath;
  @NotNull
  private final String myJspPath;

  public VaultSettingsController(@NotNull PluginDescriptor descriptor,
                                 @NotNull WebControllerManager web,
                                 @NotNull ServerPaths serverPaths,
                                 @NotNull VaultApiConnector vaultApiConnector) {
    myApiConnector = vaultApiConnector;
    myDataDirPath = serverPaths.getDataDirectory();
    myJspPath = descriptor.getPluginResourcesPath("vaultSettings.jsp");

    web.registerController(descriptor.getPluginResourcesPath("vaultSettings.html"), this);
  }

  @Nullable
  @Override
  protected ModelAndView doHandle(@NotNull final HttpServletRequest request, @NotNull final HttpServletResponse response) throws Exception {
    final ModelAndView mv = new ModelAndView(myJspPath);
    mv.getModel().put("vaultApiPresent", myApiConnector.detectApi());
    mv.getModel().put("teamCityDataDir", myDataDirPath);
    return mv;
  }
}
