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

import jetbrains.buildServer.vcs.VcsException;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

/**
 * User: vbedrosova
 * Date: 07.07.2009
 * Time: 19:32:05
 */
public final class VaultUtil {
  //        public static final byte Added = 10;
//        public static final byte BranchedFrom = 20;
//        public static final byte BranchedFromItem = 30;
//        public static final byte BranchedFromShare = 40;
//        public static final byte BranchedFromShareItem = 50;
//        public static final byte CheckIn = 60;
//        public static final byte Created = 70;
//        public static final byte Deleted = 80;
//        public static final byte Label = 90;
//        public static final byte MovedFrom = 120;
//        public static final byte MovedTo = -126;
//        public static final byte Obliterated = -116;
//        public static final byte Pinned = -106;
//        public static final byte PropertyChange = -96;
//        public static final byte Renamed = -86;
//        public static final byte RenamedItem = -76;
//        public static final byte SharedTo = -66;
//        public static final byte Snapshot = -56;
//        public static final byte SnapshotFrom = -55;
//        public static final byte SnapshotItem = -54;
//        public static final byte Undeleted = -46;
//        public static final byte UnPinned = -36;
    //TODO: process rollback
//        public static final byte Rollback = -26;

  public static final Set<String> NOT_CHANGED_CHANGE_TYPES = new HashSet<String>();

  static {
    NOT_CHANGED_CHANGE_TYPES.add("Label");

    NOT_CHANGED_CHANGE_TYPES.add("Obliterated");

    NOT_CHANGED_CHANGE_TYPES.add("Pinned");
    NOT_CHANGED_CHANGE_TYPES.add("UnPinned");

    NOT_CHANGED_CHANGE_TYPES.add("PropertyChange");

    NOT_CHANGED_CHANGE_TYPES.add("RenamedItem");

    NOT_CHANGED_CHANGE_TYPES.add("MovedTo");

    NOT_CHANGED_CHANGE_TYPES.add("Snapshot");
    NOT_CHANGED_CHANGE_TYPES.add("SnapshotFrom");
    NOT_CHANGED_CHANGE_TYPES.add("SnapshotItem");

    NOT_CHANGED_CHANGE_TYPES.add("Undeleted");

    NOT_CHANGED_CHANGE_TYPES.add("BranchedFromShare");
    NOT_CHANGED_CHANGE_TYPES.add("BranchedFromShareItem");

    NOT_CHANGED_CHANGE_TYPES.add("Rollback");
  }

  public static final Set<String> ADDED_CHANGE_TYPES =  new HashSet<String>();

  static {
    ADDED_CHANGE_TYPES.add("Created");

    ADDED_CHANGE_TYPES.add("Added");

    ADDED_CHANGE_TYPES.add("BranchedFrom");
    ADDED_CHANGE_TYPES.add("BranchedFromItem");

    ADDED_CHANGE_TYPES.add("MovedFrom");

    ADDED_CHANGE_TYPES.add("SharedTo");

    ADDED_CHANGE_TYPES.add("Renamed");
  }

  public static final Set<String> CHANGED_CHANGE_TYPES =  new HashSet<String>();

  static {
    CHANGED_CHANGE_TYPES.add("CheckIn");
  }

  public static final Set<String> REMOVED_CHANGE_TYPES =  new HashSet<String>();

  static {
    REMOVED_CHANGE_TYPES.add("Deleted");
  }

  public static XMLReader createXmlReader(@NotNull ContentHandler contentHandler) throws VcsException {
    final XMLReader xmlReader;
    try {
      xmlReader = XMLReaderFactory.createXMLReader();
      xmlReader.setContentHandler(contentHandler);
      xmlReader.setFeature("http://xml.org/sax/features/validation", false);
      return xmlReader;
    } catch (SAXException e) {
      throw new VcsException(e);
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

  public static long parseLong(@NotNull String num) throws VcsException {
    try {
      return Long.parseLong(num);
    } catch (NumberFormatException e) {
      throw new VcsException(e);
    }
  }

  public static String replaceLast(@NotNull String str, @NotNull String oldS, @NotNull String newS) {
    final int s2Start = str.lastIndexOf(oldS);
    final String s1 = str.substring(0, s2Start);
    String s2 = str.substring(s2Start);

    s2 = s2.replace(oldS, newS);
    return s1 + s2;
  }  

  public static String replaceFirst(@NotNull String str, @NotNull String oldS, @NotNull String newS) {
    final int s2Start = str.indexOf(oldS) + oldS.length();
    String s1 = str.substring(0, s2Start);
    final String s2 = str.substring(s2Start);

    s1 = s1.replace(oldS, newS);
    return s1 + s2;
  }
}
