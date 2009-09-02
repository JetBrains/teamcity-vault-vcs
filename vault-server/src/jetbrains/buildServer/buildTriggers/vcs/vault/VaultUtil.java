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

import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;
import org.jetbrains.annotations.NotNull;
import jetbrains.buildServer.vcs.VcsException;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * User: vbedrosova
 * Date: 07.07.2009
 * Time: 19:32:05
 */
public final class VaultUtil {
  public static XMLReader createXmlReader(@NotNull ContentHandler contentHandler) throws VcsException {
    final XMLReader xmlReader;
    try {
      xmlReader = XMLReaderFactory.createXMLReader();
      xmlReader.setContentHandler(contentHandler);
      xmlReader.setFeature("http://xml.org/sax/features/validation", false);
      return xmlReader;
    } catch (SAXException e) {
      throw  new VcsException(e);
    }
  }

  public static Date getDate(@NotNull String dateString) throws VcsException {
    try {
      return DateFormat.getDateTimeInstance().parse(dateString);
    } catch (ParseException e) {
      throw new VcsException(e);
    }
  }

  public static String getDateString(@NotNull Date date) {
    return DateFormat.getDateTimeInstance().format(date);
  }

  public static int parseInt(@NotNull String num) throws VcsException {
    try {
      return Integer.parseInt(num);
    } catch (NumberFormatException e) {
      throw new VcsException(e);
    }
  }
}
