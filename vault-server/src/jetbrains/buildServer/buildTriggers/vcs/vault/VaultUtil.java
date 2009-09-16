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
import java.util.Set;
import java.util.HashSet;
import java.io.File;

/**
 * User: vbedrosova
 * Date: 07.07.2009
 * Time: 19:32:05
 */
public final class VaultUtil {
  public static final Set<String> NOT_CHANGED_CHANGE_TYPES = new HashSet<String>();
  static {
    NOT_CHANGED_CHANGE_TYPES.add("Added");

    NOT_CHANGED_CHANGE_TYPES.add("Label");

    NOT_CHANGED_CHANGE_TYPES.add("Obliterated");

    NOT_CHANGED_CHANGE_TYPES.add("Pinned");
    NOT_CHANGED_CHANGE_TYPES.add("UnPinned");

    NOT_CHANGED_CHANGE_TYPES.add("PropertyChange");

    NOT_CHANGED_CHANGE_TYPES.add("RenamedItem");
  }

  public static final Set<String> ADDED_CHANGE_TYPES = new HashSet<String>();
  static {
    ADDED_CHANGE_TYPES.add("Created");

    ADDED_CHANGE_TYPES.add("BranchedFrom");
    ADDED_CHANGE_TYPES.add("BranchedFromItem");
    ADDED_CHANGE_TYPES.add("BranchedFromShare");
    ADDED_CHANGE_TYPES.add("BranchedFromShareItem");

    ADDED_CHANGE_TYPES.add("MovedTo");

    ADDED_CHANGE_TYPES.add("SharedTo");

    ADDED_CHANGE_TYPES.add("Snapshot");
    ADDED_CHANGE_TYPES.add("SnapshotFrom");
    ADDED_CHANGE_TYPES.add("SnapshotItem");

    ADDED_CHANGE_TYPES.add("Undeleted");
  }

  public static final Set<String> CHANGED_CHANGE_TYPES = new HashSet<String>();
  static {
    CHANGED_CHANGE_TYPES.add("CheckIn");
    CHANGED_CHANGE_TYPES.add("Renamed");
  }

  public static final Set<String> REMOVED_CHANGE_TYPES = new HashSet<String>();
  static {
    REMOVED_CHANGE_TYPES.add("Deleted");
    REMOVED_CHANGE_TYPES.add("MovedFrom");
//    TODO: process rollback
//    Rollback
  }

  public static String getDeletedName(@NotNull String actionString) {
    // actionString is "Deleted file.txt"
    //TODO: move to constants?
    final String prefix = "Deleted ";
    return "/" + actionString.substring(actionString.indexOf(prefix) + prefix.length());
  }

  public static String getMovedToName(@NotNull String actionString) {
    // actionString is "Moved file.txt to fold1/file.txt"
    //TODO: move to constants?
    final String prefix = " to ";
    return actionString.substring(actionString.indexOf(prefix) + prefix.length());
  }

  public static String getMovedFromName(@NotNull String actionString) {
    // actionString is "Moved file.txt from fold1"
    //TODO: move to constants?
    final String prefix = " from ";
    return actionString.substring(actionString.indexOf(prefix) + prefix.length());
  }

  public static String getRenamedFromName(@NotNull String actionString) {
    // actionString is "Renamed from file.txt to file3.txt"
    //TODO: move to constants?
    final String prefix = "Renamed from ";
    final String suffix = " to ";
    return actionString.substring(actionString.indexOf(prefix) + prefix.length(), actionString.indexOf(suffix));
  }
  
  public static String getRenamedToName(@NotNull String actionString) {
    // actionString is "Renamed from file.txt to file3.txt"
    //TODO: move to constants?
    final String prefix = " to ";
    return actionString.substring(actionString.indexOf(prefix) + prefix.length());
  }

  public static String getSharedToName(@NotNull String actionString) {
    // actionString is "Shared file.txt as file.txt"
    //TODO: move to constants?
    final String prefix = " as ";
    return actionString.substring(actionString.indexOf(prefix) + prefix.length());
  }

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
