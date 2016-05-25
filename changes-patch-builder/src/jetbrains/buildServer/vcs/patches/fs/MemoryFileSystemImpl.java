/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package jetbrains.buildServer.vcs.patches.fs;

import jetbrains.buildServer.vcs.patches.util.Assert;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Stores the paths (denoting files or directories) along with several additional flags
 * in a prefix tree (or trie, see <a href="http://en.wikipedia.org/wiki/Trie">Trie</a>
 * for more details).
 * <p/>
 * In few words, the string is split by slash symbol (<code>'/'</code>), and the result component
 * set denotes a path in a trie (from the root to a leaf). Thus each path can be mapped
 * to a leaf node, and addition/deletion of the path results in addition/deletion of the leaf.
 * <p/>
 * Example: the following trie
 * <pre>
 * foo/
 *     bar/
 *         File1
 *         File2
 *     baz/
 *         File3
 *     File4
 * </pre>
 * corresponds to a series of additions: <code>foo/bar/</code>, <code>foo/bar/File1</code>,
 * <code>foo/bar/File2</code>, <code>foo/baz/File3</code> and <code>foo/File4</code>.
 * <p/>
 * The nodes that correspond to "foo" and "baz" are auxiliary. They haven't been added
 * explicitly, but are needed to store other nodes. For each of these paths a <code>contains</code>
 * call will return <code>false</code>, but <code>containsNode</code> will return <code>true</code>.
 * <p/>
 * The node "bar" and all file nodes are not auxiliary (let's name them "marked") as they've been
 * added explicitly. For each of them both <code>contains</code> and <code>containsNode</code> calls
 * return <code>true</code>.
 *
 * @author Maxim Podkolzine (maxim.podkolzine@gmail.com)
 */
public class MemoryFileSystemImpl {
  private final Node root = new Node();

  /*************************************************************************************************
   * Public API.
   ************************************************************************************************/

  /**
   * Adds a path to the trie.
   * <p/>
   * Note: <code>isNew</code> makes sense only if <code>isFile</code> is <code>true</code>,
   * because directory can not be modified.
   *
   * @param path   the path to add
   * @param isFile specifies if it is a file or a directory
   * @param isNew  specifies if the file is new
   * @return true if the trie already contains such node, false otherwise
   */
  public boolean add(String path, boolean isFile, boolean isNew) {
    String[] components = path.split("/");
    Node node = root;
    for (int i = 0; i < components.length; ++i) {
      Edge edge = node.findEdge(components[i]);
      if (edge != null) {
        node = edge.to;
      } else {
        appendString(components, i, node, isFile, isNew);
        return false;
      }
    }
    return true;
  }

  /**
   * A shortcut (mostly for testing purposes).
   *
   * @see #add(String, boolean, boolean)
   */
  public boolean add(String path) {
    boolean isNew = isNew(path);
    boolean isFile = isFile(path);
    String refinedPath = path.substring(isNew ? 1 : 0, path.length() - (isFile ? 0 : 1));
    return add(refinedPath, isFile, isNew);
  }

  /**
   * Removes a path from the trie.
   * <p/>
   * Along with the actual node representing <code>path</code> several auxiliary nodes
   * might be removed. For instance, consider the trie:
   * <pre>
   * foo/bar/baz/File1
   * foo/File2
   * </pre>
   * Then the call <code>remove("foo/bar/baz/File1", true)</code> will remove 3 nodes ("bar",
   * "baz", and "File1"), while <code>remove("foo/File2", true)</code> will remove only one node
   * ("File2").
   * <p/>
   * Also note that a directory deletion leads to all inner files and directories deletion.
   * That is <code>remove("foo", false)</code> will clear the trie (assuming that "foo" node is
   * marked).
   *
   * @param path   the path to remove
   * @param isFile specifies if it is a file or a directory
   * @return true if the trie contained such node and it was actually removed, false otherwise
   */
  public boolean remove(String path, boolean isFile) {
    Node node = findString(path, isFile);
    if (node == null) {
      return false;
    }
    do {
      Node parent = node.parent.from;
      parent.removeEdge(node.parent.value);
      node.nullify();
      node = parent;
    } while (node != root && node.children.size() == 0 && !node.marker);
    return true;
  }

  /**
   * A shortcut (mostly for testing purposes).
   *
   * @see #remove(String, boolean)
   */
  public boolean remove(String path) {
    return remove(path, isFile(path));
  }

  /**
   * Removes the path from the trie.
   * <p/>
   * This is a more effifient version of {@link #remove(String, boolean)} method. No nodes are
   * actually removed, the target node is just set unmarked. So the result is that a
   * <code>contains</code> will no longer return <code>true</code>.
   *
   * @param path   the path to remove
   * @param isFile specifies if it is a file or a directory
   * @return true if the trie contained such node, false otherwise
   */
  public boolean removeMarker(String path, boolean isFile) {
    Node node = findString(path, isFile);
    if (node == null) {
      return false;
    }
    node.marker = false;
    return true;
  }

  /**
   * Returns whether the trie contains a specified path or not, i.e. contains a marked node
   * corresponding to <code>path</code>.
   * <p/>
   * Note: <code>isNew</code> makes sense only if <code>isFile</code> is <code>true</code>.
   *
   * @param path   the path to check
   * @param isFile specifies if it is a file or a directory
   * @param isNew  specifies if the file is new
   * @return true if the trie contains a <code>path</code>, false otherwise
   * @see #containsNode(String)
   */
  public boolean contains(String path, boolean isFile, boolean isNew) {
    Node node = findString(path, isFile);
    return node != null && node.marker && node.isNew == isNew;
  }

  /**
   * A shortcut (when there is no need to distinguish if file is new).
   *
   * @see #contains(String, boolean, boolean)
   */
  public boolean contains(String path, boolean isFile) {
    Node node = findString(path, isFile);
    return node != null && node.marker;
  }

  /**
   * A shortcut (mostly for testing purposes).
   *
   * @see #contains(String, boolean, boolean)
   */
  public boolean contains(String path) {
    return contains(path, isFile(path));
  }

  /**
   * Returns whether the trie contains an ancestor directory of the <code>path</code>.
   * <p/>
   * For instance, consider the trie
   * <pre>
   * foo/bar/baz/File1
   * foo/File2
   * </pre>
   * in which "bar" directory is marked, others are auxiliary. Then
   * <code>containsAncestor("foo") == false</code>,
   * <code>containsAncestor("foo/bar") == false</code>,
   * <code>containsAncestor("foo/bar/baz") == true</code>, and
   * <code>containsAncestor("foo/bar/baz/any/path/here") == true</code>.
   *
   * @param path the path to check
   * @return true if the trie contains an ancestor directory, false otherwise
   */
  public boolean containsAncestor(String path) {
    String[] components = path.split("/");
    Node node = root;
    for (int i = 0; i < components.length - 1; i++) {
      String component = components[i];
      Edge edge = node.findEdge(component);
      if (edge != null) {
        node = edge.to;
        if (!node.isFile && node.marker) {
          return true;
        }
      } else {
        return false;
      }
    }
    return false;
  }

  /**
   * Returns whether the trie contains a node corresponding to the <code>path</code>,
   * no matter if the node is marked or not.
   *
   * @param path the path to check
   * @return true if the trie contains a node corresponding to the <code>path</code>,
   *         false otherwise
   * @see #contains(String, boolean)
   */
  public boolean containsNode(String path) {
    return findString(path, true) != null || findString(path, false) != null;
  }

  /**
   * Fills the specified collections with marked nodes of three types:
   * <ul>
   * <li> directory nodes (go to <code>newDirectories</code>),
   * <li> file nodes added with <code>isNew == true</code> flag (go to <code>newFiles</code>),
   * <li> file nodes added with <code>isNew == false</code> flag (go to <code>modifiedFiles</code>).
   * </ul>
   * Auxiliary nodes are ignored.
   *
   * @param newFiles       the collection which is filled with new files
   * @param modifiedFiles  the collection which is filled with modified files
   * @param newDirectories the collection which is filled with new directories
   * @see #add(String, boolean, boolean)
   */
  public void toCollections(final Collection<String> newFiles,
                            final Collection<String> modifiedFiles,
                            final Collection<String> newDirectories) {
    traverseAndRun(root, new StringBuilder(), new NodeVisitor() {
      public void visit(Node node, String fullPath) {
        if (node.marker) {
          if (node.isFile) {
            if (node.isNew) {
              newFiles.add(fullPath);
            } else {
              modifiedFiles.add(fullPath);
            }
          } else {
            newDirectories.add(fullPath);
          }
        }
      }
    });
  }

  /**
   * A shorcut (when there is no need to distinguish new files and modified files).
   *
   * @see #toCollections(java.util.Collection, java.util.Collection, java.util.Collection)}
   */
  public void toCollections(final Collection<String> files, final Collection<String> directories) {
    toCollections(files, files, directories);
  }

  /**
   * A shorcut (when there is no need to distinguish files and directories).
   *
   * @see #toCollections(java.util.Collection, java.util.Collection, java.util.Collection)}
   */
  public void toCollection(final Collection<String> files) {
    toCollections(files, files, files);
  }

  /**
   * Prints the trie to stdout. Mostly for debugging purposes.
   *
   * @param verbose prints more information if this flag is true
   */
  public void print(final boolean verbose) {
    traverseAndRun(root, new StringBuilder(), new NodeVisitor() {
      public void visit(Node node, String fullPath) {
        if (!verbose) {
          if (node.marker) {
            System.out.println(fullPath);
          }
        } else {
          System.out.print(fullPath);
          if (!node.isFile) {
            System.out.print("/");
          }
          if (node.marker) {
            System.out.print("$");
          }
          System.out.println();
        }
      }
    });
  }

  /**
   * A shortcut for non-verbose print.
   *
   * @see #print(boolean)
   */
  public void print() {
    print(false);
  }

  /**
   * **********************************************************************************************
   * Helper methods and classes.
   * **********************************************************************************************
   */

  private boolean isFile(String path) {
    return !path.endsWith("/");
  }

  private boolean isNew(String path) {
    return path.startsWith("+");
  }

  private void appendString(String[] components, int from, Node node,
                            boolean isFile, boolean isNew) {
    for (int i = from; i < components.length; ++i) {
      node = node.addEdge(components[i]).to;
    }
    node.marker = true;
    node.isFile = isFile;
    node.isNew = isNew;
  }

  private Node findString(String path, boolean isFile) {
    String[] components = path.split("/");
    Node node = root;
    for (String component : components) {
      Edge edge = node.findEdge(component);
      if (edge != null) {
        node = edge.to;
      } else {
        return null;
      }
    }
    return (node.isFile == isFile) ? node : null;
  }

  private void traverseAndRun(Node node, StringBuilder fullPath, NodeVisitor visitor) {
    visitor.visit(node, fullPath.toString());
    int length = fullPath.length();
    for (Edge edge : node.children.values()) {
      fullPath.append(length > 0 ? "/" : "")      // add a separator (except for the leading one)
        .append(edge.value);                // add current edge value
      traverseAndRun(edge.to, fullPath, visitor);
      fullPath.setLength(length);                 // restore the previous value
    }
  }

  private static class Node {
    Edge parent = null;
    Map<String, Edge> children = new TreeMap<String, Edge>();
    boolean isFile = false;
    boolean isNew = false;
    boolean marker = false;

    Edge findEdge(String value) {
      return children.get(value);
    }

    Edge addEdge(String value) {
      Assert.assertFalse(isFile);  // only directory nodes can have children
      Edge result = new Edge();
      result.value = value;
      result.from = this;
      result.to = new Node();
      result.to.parent = result;
      children.put(value, result);
      return result;
    }

    void removeEdge(String value) {
      children.remove(value);
    }

    void nullify() {
      parent = null;
      children = null;
    }
  }

  private static class Edge {
    Node from;
    Node to;
    String value;
  }

  private static interface NodeVisitor {
    void visit(Node node, String fullPath);
  }
}