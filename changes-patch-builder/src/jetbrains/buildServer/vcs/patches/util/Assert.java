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
