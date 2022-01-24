/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.vcs.patches.util;

/**
 * User: vbedrosova
 * Date: 22.12.2009
 * Time: 15:10:20
 */
public abstract class Assert {

  public Assert() {
  }

  public static void assertTrue(boolean condition) {
    if (!condition)
      throw new AssertionFailedException();
  }

  public static void assertTrue(boolean condition, String message) {
    if (!condition)
      throw new AssertionFailedException(message);
  }

  public static void assertFalse(boolean condition) {
    if (condition)
      throw new AssertionFailedException();
  }

  public static void assertFalse(boolean condition, String message) {
    if (condition)
      throw new AssertionFailedException(message);
  }

  public static void isNotNull(Object object) {
    if (object == null)
      throw new AssertionFailedException("Null argument");
  }

  public static void isNotNull(Object object, String message) {
    if (object == null)
      throw new AssertionFailedException(message);
  }
}
