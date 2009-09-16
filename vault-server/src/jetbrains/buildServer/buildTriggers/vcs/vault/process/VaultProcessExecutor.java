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

package jetbrains.buildServer.buildTriggers.vcs.vault.process;

import com.intellij.execution.configurations.GeneralCommandLine;
import jetbrains.buildServer.vcs.VcsException;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

/**
 * User: vbedrosova
 * Date: 07.07.2009
 * Time: 12:23:10
 */
public final class VaultProcessExecutor {
  public static InputStream runProcess(@NotNull GeneralCommandLine cl) throws VcsException {
    final Process p;
    try {
      p = cl.createProcess();
//      TODO: wait or not wait
//      p.waitFor();
      return p.getInputStream();
    } catch (Exception e) {
      throw new VcsException(e);
    }
  }
}
