package jetbrains.buildServer.buildTriggers.vcs.vault.controllers;

import jetbrains.buildServer.controllers.BaseAjaxActionController;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.openapi.ControllerAction;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.vcs.VcsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jdom.Element;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: vbedrosova
 * Date: 03.07.2009
 * Time: 14:16:46
 */
public class EditVaultSettingsController extends BaseAjaxActionController {
  public EditVaultSettingsController(@NotNull final WebControllerManager controllerManager) {
    super(controllerManager);
    controllerManager.registerController("/admin/convertOldVaultSettings.html", this);
    registerAction(new ControllerAction() {
      public boolean canProcess(HttpServletRequest request) {
        final String oldServer = request.getParameter("vault.server.value");
        final String oldRepo = request.getParameter("vault.repo.value");
        return oldServer != null && oldServer.trim().length() != 0 && oldRepo != null && oldRepo.trim().length() != 0;
      }

      public void process(HttpServletRequest request, HttpServletResponse response, @Nullable Element ajaxResponse) {
        if (ajaxResponse == null) {
          Loggers.SERVER.debug("Error: ajaxResponse is null");
          return;
        }

        try {
          final Element vaultServer = new Element("vault.server");
          vaultServer.addContent(request.getParameter("vault.server.value"));

          final Element vaultRepo = new Element("vault.repo");
          vaultRepo.addContent(request.getParameter("vault.repo.value"));

          ajaxResponse.addContent(vaultServer);
          ajaxResponse.addContent(vaultRepo);
        } catch (Exception e) {
          final Element error = new Element("error");
          error.addContent(e.getLocalizedMessage());
          ajaxResponse.addContent(error);
        }
      }
    });
  }
}
