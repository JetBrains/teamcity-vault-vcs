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
import jetbrains.buildServer.vcs.VcsRoot;
import jetbrains.buildServer.vcs.VcsException;
import jetbrains.buildServer.vcs.TestConnectionSupport;
import jetbrains.buildServer.buildTriggers.vcs.vault.process.VaultProcessExecutor;
import jetbrains.buildServer.buildTriggers.vcs.vault.VaultUtil;
import jetbrains.buildServer.util.FileUtil;
import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.jetbrains.annotations.NotNull;
import org.apache.log4j.Logger;

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

  public static abstract class Handler extends DefaultHandler {};

  private final String myCachesPath;

  public VaultConnection(@NotNull String cachesPath) {
    myCachesPath = cachesPath;
    final File caches = new File(myCachesPath);
    FileUtil.delete(caches);
    if (!caches.mkdir()) {
      LOG.warn("Unable to create caches dir: " + caches.getAbsolutePath());
    }
  }

  public void runCommand(@NotNull GeneralCommandLine cl, Handler handler) throws VcsException {
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
//      reader.parse(new InputSource(inputStream));
        reader.parse(new InputSource(new BufferedReader(new FileReader(f))));
      }
    } catch (SAXException se) {
      if (se != SUCCESS) {
        throw new VcsException(se);
      }
    } catch (Exception e) {
      throw  new VcsException(e);
    }
  }

  public void setWorkingFolder(@NotNull VcsRoot root, @NotNull String path) throws VcsException {
    final GeneralCommandLine cl = new GeneralCommandLine();
    cl.setExePath(root.getProperty("vault.path"));
    cl.addParameter("setworkingfolder");
    cl.addParameter("-server");
    cl.addParameter(root.getProperty("vault.server"));
    cl.addParameter("-user");
    cl.addParameter(root.getProperty("vault.user"));
    cl.addParameter("-password");
    cl.addParameter(root.getProperty("secure:vault.password"));
    cl.addParameter("-repository");
    cl.addParameter(root.getProperty("vault.repo"));

    cl.addParameter("$");
    cl.addParameter("\"" + path + '\"');

    runCommand(cl, null);  
  }

  public String testConnection(@NotNull VcsRoot vcsRoot) throws VcsException {
    testConnection(vcsRoot.getProperties());
    return null;
  }

  private static abstract class VersionHandler extends Handler {
    protected String myResult;

    public String getResult() {
      return myResult;
    }
  }

  public void testConnection(Map<String, String> properties) throws VcsException {
    final GeneralCommandLine cl = new GeneralCommandLine();
    cl.setExePath(properties.get("vault.path"));

    cl.addParameter("versionhistory");
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
    cl.addParameter("-rowlimit");
    cl.addParameter("1");
    cl.addParameter("$");

    try {
      runCommand(cl, new VersionHandler() {
        protected StringBuffer myCData = new StringBuffer();

        public void endElement (String uri, String localName, String qName)
        throws SAXException
        {
          if ("exception".equals(localName)) {
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

  public String getCurrentDate(Map<String, String> properties) throws VcsException {
    final GeneralCommandLine cl = new GeneralCommandLine();
    cl.setExePath(properties.get("vault.path"));

    cl.addParameter("versionhistory");
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
    cl.addParameter("-rowlimit");
    cl.addParameter("1");
    cl.addParameter("$");

    final VersionHandler handler = new VersionHandler() {
      public void startElement (String uri, String localName,
                    String qName, Attributes attributes)
      throws SAXException
      {
        if ("item".equals(localName)) {
          myResult = attributes.getValue("date");
          throw SUCCESS;
        }
      }
    };
    runCommand(cl, handler);
    return handler.getResult();
  }

  public String getCurrentDate(VcsRoot root) throws VcsException {
    return getCurrentDate(root.getProperties());
  }

  public int getVersionByDate(VcsRoot root, final String dateString) throws VcsException {
    return VaultUtil.parseInt(getVersionStringByDate(root, dateString));
  }

  public String getVersionStringByDate(VcsRoot root, final String dateString) throws VcsException {
    final GeneralCommandLine cl = new GeneralCommandLine();
    cl.setExePath(root.getProperty("vault.path"));
    cl.addParameter("versionhistory");
    cl.addParameter("-server");
    cl.addParameter(root.getProperty("vault.server"));
    cl.addParameter("-user");
    cl.addParameter(root.getProperty("vault.user"));
    cl.addParameter("-password");
    cl.addParameter(root.getProperty("secure:vault.password"));
    cl.addParameter("-repository");
    cl.addParameter(root.getProperty("vault.repo"));
    if (root.getProperty("vault.enable.ssl") != null) {
      cl.addParameter("-ssl");
    }
    cl.addParameter("$");
    final VersionHandler handler = new VersionHandler() {
      public void startElement (String uri, String localName,
                    String qName, Attributes attributes)
      throws SAXException
      {
        if ("item".equals(localName) && dateString.equals(attributes.getValue("date"))) {
          myResult = attributes.getValue("version");
          throw SUCCESS;
        }
      }
    };
    runCommand(cl, handler);
    return handler.getResult();
  }

  private static String versionToFolderName(@NotNull String version) {
    return version.replace(":", ".");
  }

  private boolean hasCache(@NotNull String folderName) {
    return new File(myCachesPath, folderName).isDirectory();
  }

  public File getCache(@NotNull VcsRoot root, @NotNull String path, @NotNull String version) throws VcsException {
    String folderName = versionToFolderName(version);
    if (!hasCache(folderName)) {
      buildCache(root, version, myCachesPath + File.separator + folderName);
    }
    File cachedFile = new File(myCachesPath + File.separator + folderName, path);
    if (!cachedFile.exists()) {
      buildCache(root, version, myCachesPath + File.separator + folderName);
    }
    return cachedFile;
  }

  private void buildCache(@NotNull VcsRoot root, @NotNull String version, @NotNull String destDir) throws VcsException {
//    setWorkingFolder(root, destDir);
    final GeneralCommandLine cl = new GeneralCommandLine();
    cl.setExePath(root.getProperty("vault.path"));
    cl.addParameter("getversion");
    cl.addParameter("-server");
    cl.addParameter(root.getProperty("vault.server"));
    cl.addParameter("-user");
    cl.addParameter(root.getProperty("vault.user"));
    cl.addParameter("-password");
    cl.addParameter(root.getProperty("secure:vault.password"));
    cl.addParameter("-repository");
    cl.addParameter(root.getProperty("vault.repo"));
    cl.addParameter("-setfiletime");
    cl.addParameter("modification");

    cl.addParameter(getVersionStringByDate(root, version));
    cl.addParameter("$");
    cl.addParameter(destDir);

    runCommand(cl, null);
  }
}
