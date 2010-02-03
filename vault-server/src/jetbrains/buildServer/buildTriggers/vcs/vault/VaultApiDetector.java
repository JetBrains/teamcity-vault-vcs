/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
    } catch (NoClassDefFoundError e) {
      return false;
    }
    return true;
  }
}
