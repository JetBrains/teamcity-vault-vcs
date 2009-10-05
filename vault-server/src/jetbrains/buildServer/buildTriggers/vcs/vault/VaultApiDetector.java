package jetbrains.buildServer.buildTriggers.vcs.vault;

import VaultClientNetLib.VaultConnection;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URL;

/**
 * User: vbedrosova
 * Date: 05.10.2009
 * Time: 16:09:01
 */
public class VaultApiDetector {
  private static boolean ourInited = false;
  private static boolean ourApiPresent = false;

  public static boolean detectApi() {
    if(!ourInited) {
      ourApiPresent = detect();
      ourInited = true;
    }
    return ourApiPresent;
  }

  private static boolean detect() {
    try {
      final Class<?> serverClass = Class.forName("VaultClientIntegrationLib.ServerOperations");
      final URL url = serverClass.getClassLoader().getResource(serverClass.getName().replace('.', '/')+".class");
      if (url == null) {
        return false;       
      }
    } catch (ClassNotFoundException e) {
      return false;
    }
    return true;
  }
}
