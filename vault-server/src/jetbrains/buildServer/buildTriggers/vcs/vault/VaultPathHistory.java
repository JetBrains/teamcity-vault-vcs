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

package jetbrains.buildServer.buildTriggers.vcs.vault;

import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 20.10.2009
 * Time: 18:42:19
 */
public class VaultPathHistory {
  private final Map<String, Node> myPathMap = new HashMap<String, Node>();
  private Node myRoot = new Node(VaultUtil.ROOT, VaultUtil.ROOT, null);

  public void clear() {
    myPathMap.clear();
    myRoot = new Node(VaultUtil.ROOT, VaultUtil.ROOT, null);
  }

  public void rename(@NotNull String parent, @NotNull String fromName, @NotNull String toName) {
    final String from = parent + "/" + fromName;
    final String to = parent + "/" + toName;

    final Node node = getTreeNode(to);
    if (node == null) {
      final String newPath = getNewPath(to);
      myPathMap.put(newPath, addTreeNode(from, newPath));
    } else {
      node.setName(fromName);
      node.getParent().childRenamed(toName, fromName);
      if (!myPathMap.containsKey(node.getNewPath())) {
        myPathMap.put(node.getNewPath(), node);        
      }
    }
  }

  public void move(@NotNull String fromParent, @NotNull String toParent, @NotNull String name) {
    final String from = fromParent + "/" + name;
    final String to = toParent + "/" + name;

    final Node node = getTreeNode(to);
    if (node == null) {
      final String newPath = getNewPath(to);
      myPathMap.put(newPath, addTreeNode(from, newPath));
    } else {
      removeTreeNode(node);
      final Node oldParentNode = addTreeNode(fromParent, getNewPath(fromParent));
      oldParentNode.addChild(node);
      node.setParent(oldParentNode);
      if (!myPathMap.containsKey(node.getNewPath())) {
        myPathMap.put(node.getNewPath(), node);
      }
    }
  }

  public void delete(@NotNull String path) {
    final Node node = getTreeNode(path);
    if (node != null) {
      removeTreeNode(node);
      myPathMap.remove(node.getNewPath());
    }
  }

  public String getOldPath(@NotNull String newPath) {
    final String[] components = newPath.split("/");
    String path = newPath;
    final StringBuffer suffix = new StringBuffer();  
    for (int i = components.length - 1; i > 0 ; --i) {
      if (myPathMap.containsKey(path)) {
        return getTreeNodePath(myPathMap.get(path)) + suffix.toString();
      }
      final String name = components[i];
      path = path.substring(0, path.lastIndexOf(name) - 1);
      suffix.insert(0, name).insert(0, "/");
    }
    return newPath;
  }

  public String getNewPath(@NotNull String oldPath) {
    final String[] components = oldPath.split("/");
    Node node = myRoot;
    String newPath = VaultUtil.ROOT;
    for (int i = 1; i < components.length; ++i) {
      final String name = components[i];
      if (node == null || !node.hasChild(name)) {
        newPath += "/" + name;
        node = null;
      } else {
        node = node.getChild(name);
        newPath = node.getNewPath();
      }
    }
    return newPath;
  }

  private Node getTreeNode(@NotNull String path) {
    final String[] components = path.split("/");
    Node node = myRoot;
    for (int i = 1; i < components.length; ++i) {
      final String name = components[i];
      if (node.hasChild(name)) {
        node = node.getChild(name);
      } else {
        return null;
      }
    }
    return node;    
  }

  private Node addTreeNode(@NotNull String oldPath, @NotNull String newPath) {
    final String[] components = oldPath.split("/");
    Node node = myRoot;
    String np = VaultUtil.ROOT;
    for (int i = 1; i < components.length; ++i) {
      final String name = components[i];
      if (!node.hasChild(name)) {
        if (i < (components.length - 1)) {
          np += "/" + name;
        } else {
          np = newPath;
        }
        final Node child = new Node(name, np, node);
        node.addChild(child);
        node = child;
      } else {
        node = node.getChild(name);
        np = node.getNewPath();
      }
    }
    return node;
  }

  private void removeTreeNode(@NotNull Node node) {
    do {
      final String name = node.getName();
      node = node.getParent();
      node.removeChild(name);
      if (myPathMap.containsKey(node.getNewPath()) || node.hasChildren()) {
        break;
      }
    } while (node != myRoot);
  }  

  private String getTreeNodePath(@NotNull Node node) {
    final StringBuffer path = new StringBuffer(node.getName());
    do {
      node = node.getParent();
      path.insert(0, "/").insert(0, node.getName());
    } while (node != myRoot);
    return path.toString();
  }

  private static final class Node {
    @NotNull private String myName;  
    @NotNull private final String myNewPath;
    private Node myParent;
    @NotNull private final Map<String, Node> myChildren;

    private Node(@NotNull String name, @NotNull String newPath, Node parent) {
      myName = name;
      myChildren = new HashMap<String, Node>();
      myParent = parent;
      myNewPath = newPath;
    }

    @NotNull
    public String getName() {
      return myName;
    }

    public void setName(@NotNull String name) {
      myName = name;
    }

    @NotNull
    public String getNewPath() {
      return myNewPath;
    }

    public Node getParent() {
      return myParent;
    }

    public void setParent(Node parent) {
      myParent = parent;
    }

    public boolean hasChildren() {
      return myChildren.size() > 0;
    }

    public boolean hasChild(@NotNull String name) {
      return myChildren.containsKey(name);
    }

    public Node getChild(@NotNull String name) {
      return myChildren.get(name);
    }

    public void removeChild(@NotNull String name) {
      if (!myChildren.containsKey(name)) {
        throw new RuntimeException("Unable to remove child node for name " + name
          + ", child node with this name doesn't exist");
      }
      myChildren.remove(name);
    }

    public void addChild(@NotNull Node child) {
      final String name = child.getName();
      if (myChildren.containsKey(name)) {
        throw new RuntimeException("Unable to add child node for name " + name
          + ", child node with this name already exists");
      }
      myChildren.put(name, child);
    }

    public void childRenamed(@NotNull String oldName, @NotNull String newName) {
      if (!myChildren.containsKey(oldName)) {
        throw new RuntimeException("Unable to rename child node for name " + oldName
          + ", child node with this name doesn't exist");
      }
      myChildren.put(newName, myChildren.get(oldName));
      myChildren.remove(oldName);
    }
  }
}
