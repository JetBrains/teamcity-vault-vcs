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
import jetbrains.buildServer.serverSide.ServerPaths;
import org.apache.log4j.Logger;
import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * User: vbedrosova
 * Date: 15.05.2009
 * Time: 18:41:12
 */
public final class VaultConnection implements TestConnectionSupport {
  private static VaultConnection outInstance = new VaultConnection();

  public static VaultConnection getConnection() {
    return outInstance;
  }

  public static final SAXException SUCCESS = new SAXException("Success");
  public static abstract class Handler extends DefaultHandler {};

  private VaultConnection() {}

  public void runCommand(GeneralCommandLine cl, Handler handler) throws VcsException {
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
        final XMLReader reader = VaultUtil.createReader(handler);
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
        }
      }
    };
    runCommand(cl, handler);
    return handler.getResult();
  }

  public String getCurrentDate(VcsRoot root) throws VcsException {
    return getCurrentDate(root.getProperties());
  }

  public String getVersionByDate(VcsRoot root, final String dateString) throws VcsException {
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
        }
      }
    };
    runCommand(cl, handler);
    return handler.getResult();
  }
}
