package jetbrains.buildServer.vcs.patches.util;

/**
 * User: vbedrosova
 * Date: 22.12.2009
 * Time: 15:10:27
 */
public class AssertionFailedException extends RuntimeException {
  public AssertionFailedException() {
    super();
  }

  public AssertionFailedException(String detail) {
    super(detail);
  }
}
