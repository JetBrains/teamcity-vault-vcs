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

package jetbrains.buildServer.buildTriggers.vcs.vault;

import com.intellij.execution.configurations.GeneralCommandLine;
import jetbrains.buildServer.buildTriggers.vcs.vault.process.VaultProcessExecutor;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.vcs.TestConnectionSupport;
import jetbrains.buildServer.vcs.VcsException;
import jetbrains.buildServer.vcs.VcsRoot;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.*;
import java.util.Map;

/**
 * User: vbedrosova
 * Date: 15.05.2009
 * Time: 18:41:12
 */
public final class VaultConnection implements TestConnectionSupport {
  private static final Logger LOG = Logger.getLogger(VaultConnection.class);

  public static final SAXException SUCCESS = new SAXException("Success");
  public static final String ROOT = "$";

  public static abstract class Handler extends DefaultHandler {};

  private static abstract class ResultHandler extends Handler {
    protected String myResult;

    public String getResult() {
      return myResult;
    }
  }

  private final String myCachesPath;

  public VaultConnection(@NotNull String cachesPath) {
    myCachesPath = cachesPath;

    final File caches = new File(myCachesPath);
    FileUtil.delete(caches);
    if (!caches.mkdir()) {
      LOG.warn("Unable to create caches dir: " + caches.getAbsolutePath());
    }
  }

  private static void runCommand(@NotNull GeneralCommandLine cl, Handler handler) throws VcsException {
    try {
      final InputStream inputStream = VaultProcessExecutor.runProcess(cl);

      final File f = FileUtil.createTempFile(cl.toString(), "");
      final BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
      final Writer w = new BufferedWriter(new FileWriter(f));
      String line = r.readLine();
      while (line != null) {
        System.out.println(line);
        w.write(line + "\n");
        line = r.readLine();
      }
      w.flush();
      inputStream.close();

      if (handler != null) {
        final XMLReader reader = VaultUtil.createXmlReader(handler);
        reader.parse(new InputSource(new BufferedReader(new FileReader(f))));
      }
    } catch (SAXException se) {
      if (se != SUCCESS) {
        throw new VcsException(se);
      }
    } catch (Exception e) {
      throw new VcsException(e);
    }
  }

  public String testConnection(@NotNull VcsRoot vcsRoot) throws VcsException {
    testConnection(vcsRoot.getProperties());
    return null;
  }

  public void testConnection(@NotNull Map<String, String> properties) throws VcsException {
    try {
      runRootVersionHistoryCommand(properties, 1, new Handler() {
        protected StringBuffer myCData = new StringBuffer();

        public void endElement(String uri, String localName, String qName)
          throws SAXException {
          if ("exception".equals(localName) || "error".equals(localName)) {
            throw new SAXException(myCData.toString().trim());
          }
          myCData.delete(0, myCData.length());
        }

        public void characters(char ch[], int start, int length) throws SAXException {
          myCData.append(ch, start, length);
        }
      });
    } catch (Exception e) {
      throw new VcsException(e);
    }
  }

  private void runRootVersionHistoryCommand(@NotNull Map<String, String> properties, int rowLimit, @NotNull Handler handler) throws VcsException {
    final GeneralCommandLine cl = new GeneralCommandLine();

    cl.setExePath(properties.get("vault.path"));

    cl.addParameter("versionhistory");

    addRepositoryProperties(cl, properties);

    if (rowLimit != -1) {
      cl.addParameter("-rowlimit");
      cl.addParameter("" + rowLimit);
    }
    cl.addParameter(ROOT);

    runCommand(cl, handler);
  }

  public void runRootHistoryCommand(@NotNull Map<String, String> properties, String beginDate, String endDate, @NotNull Handler handler) throws VcsException {
    final GeneralCommandLine cl = new GeneralCommandLine();
    cl.setExePath(properties.get("vault.path"));

    cl.addParameter("history");

    addRepositoryProperties(cl, properties);
    if (beginDate != null) {
      cl.addParameter("-begindate");
      cl.addParameter(beginDate);
    }
    if (endDate != null) {
      cl.addParameter("-begindate");
      cl.addParameter(endDate);
    }

//    TODO: add this
//    -excludeactions action,action,...
//
//    A comma-separated list of actions that will be excluded from
//    the history query. Valid actions to exclude are:
//    add, branch, checkin, create, delete, label, move, obliterate, pin,
//    propertychange, rename, rollback, share, snapshot, undelete.

//    cl.addParameter("-excludeactions");
//    cl.addParameter("label,obliterate,pin,propertychange");

    cl.addParameter(ROOT);
    runCommand(cl, handler);
  }

  public void runListFolderCommand(@NotNull String path, @NotNull Map<String, String> properties, @NotNull Handler handler) throws VcsException {
    final GeneralCommandLine cl = new GeneralCommandLine();
    cl.setExePath(properties.get("vault.path"));
    cl.addParameter("listfolder");
    addRepositoryProperties(cl, properties);
    cl.addParameter(path);
  }

  private void addRepositoryProperties(@NotNull GeneralCommandLine cl, @NotNull Map<String, String> properties) {
    cl.addParameter("-server");
    cl.addParameter(properties.get("vault.server"));
    cl.addParameter("-user");
    cl.addParameter(properties.get("vault.user"));
    cl.addParameter("-password");
    cl.addParameter(properties.get("secure:vault.password"));
    cl.addParameter("-repository");
    cl.addParameter(properties.get("vault.repo"));
    if (properties.get("vault.enable.ssl") != null) {
      cl.addParameter("-ssl");
    }
  }

  public String getCurrentDate(@NotNull VcsRoot root) throws VcsException {
    final ResultHandler handler = new ResultHandler() {
      public void startElement(String uri, String localName,
                               String qName, Attributes attributes)
        throws SAXException {
        if ("item".equals(localName)) {
          myResult = attributes.getValue("date");
          throw SUCCESS;
        }
      }
    };
    runRootVersionHistoryCommand(root.getProperties(), 1, handler);
    return handler.getResult();
  }

  private String getRootVersionByDate(@NotNull VcsRoot root, @NotNull final String dateString) throws VcsException {
    final ResultHandler handler = new ResultHandler() {
      public void startElement(String uri, String localName,
                               String qName, Attributes attributes)
        throws SAXException {
        if ("item".equals(localName) && dateString.equals(attributes.getValue("date"))) {
          myResult = attributes.getValue("version");
          throw SUCCESS;
        }
      }
    };
    runRootVersionHistoryCommand(root.getProperties(), -1, handler);
    return handler.getResult();
  }

  private static String versionToFolderName(@NotNull String version) {
    return version.replace(":", "_").replace(" ", "_");
  }

  private boolean hasCacheFolder(@NotNull String folderName) {
    return new File(myCachesPath, folderName).isDirectory();
  }

  public File getObject(@NotNull VcsRoot root, @NotNull String path, @NotNull String version) throws VcsException {
    String folderName = versionToFolderName(version);
    if (!hasCacheFolder(folderName)) {
      buildCache(root, version, myCachesPath + File.separator + folderName);
    }
    File cachedFile = new File(myCachesPath + File.separator + folderName, path);
    if (!cachedFile.exists()) {
      buildCache(root, version, myCachesPath + File.separator + folderName);
    }
    return cachedFile;
  }

  private void buildCache(@NotNull VcsRoot root, @NotNull String version, @NotNull String destDir) throws VcsException {
    final GeneralCommandLine cl = new GeneralCommandLine();
    cl.setExePath(root.getProperty("vault.path"));
    cl.addParameter("getversion");
    addRepositoryProperties(cl, root.getProperties());
    cl.addParameter("-setfiletime");
    cl.addParameter("modification");
    cl.addParameter(getRootVersionByDate(root, version));
    cl.addParameter(ROOT);
    cl.addParameter(destDir);

    runCommand(cl, null);
  }
}
