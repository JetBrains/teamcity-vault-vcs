/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import java.net.URL;
import jetbrains.buildServer.serverSide.ServerPaths;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 05.10.2009
 * Time: 16:09:01
 */
public class VaultApiDetector {
  private static final Logger LOG = Logger.getLogger(VaultApiDetector.class);

  private static boolean ourInited = false;
  private static boolean ourApiPresent = false;
  private static ServerPaths ourServerPaths;

  static synchronized void setServerPaths(@NotNull ServerPaths serverPaths) {
    ourServerPaths = serverPaths;
  }

  public synchronized static String getDataDirectoryPath() {
    return ourServerPaths.getDataDirectory().getAbsolutePath();
  }

  public synchronized static boolean detectApi() {
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
      LOG.debug(e.getMessage(), e);
      return false;
    } catch (NoClassDefFoundError e) {
      LOG.debug(e.getMessage(), e);
      return false;
    }
    return true;
  }
}
