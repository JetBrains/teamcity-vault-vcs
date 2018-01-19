/*
 * Copyright 2000-2018 JetBrains s.r.o.
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
package jetbrains.buildServer.vcs.patches;

import jetbrains.buildServer.vcs.patches.fs.MemoryFileSystemImplTest;
import jetbrains.buildServer.vcs.patches.fs.MemoryFileSystemTest;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Maxim Podkolzine (maxim.podkolzine@gmail.com)
 */
public class AllTests extends TestSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTestSuite(MemoryFileSystemTest.class);
    suite.addTestSuite(MemoryFileSystemImplTest.class);
    suite.addTestSuite(ChangesPatchBuilderTest.class);

    return suite;
  }
}
