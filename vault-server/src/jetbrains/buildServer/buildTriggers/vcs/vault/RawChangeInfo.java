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

import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.filters.Filter;
import jetbrains.buildServer.vcs.VcsChangeInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Date;

/**
 * Created by Victory.Bedrosova on 8/23/13.
 */
public class RawChangeInfo {
  @NotNull private final String myPath;
  @Nullable private final String myAdditionalPath1;
  @Nullable private final String myAdditionalPath2;

  @NotNull private final String myVersion;
  @NotNull private final Date myDate;

  @NotNull private final String myUser;
  @Nullable private final String myChangeName;
  @Nullable private final String myComment;

  @NotNull private final RawChangeInfoType myType;

  public RawChangeInfo(@NotNull String path,
                       @Nullable String additionalPath1,
                       @Nullable String additionalPath2,
                       @NotNull String version,
                       @NotNull Date date,
                       @NotNull String user,
                       @Nullable String changeName,
                       @Nullable String comment,
                       @NotNull RawChangeInfoType type) {
    myPath = path;
    myAdditionalPath1 = additionalPath1;
    myAdditionalPath2 = additionalPath2;
    myVersion = version;
    myDate = date;
    myUser = user;
    myChangeName = changeName;
    myComment = comment;
    myType = type;
  }

  @NotNull
  public String getPath() {
    return myPath;
  }

  @Nullable
  public String getAdditionalPath1() {
    return myAdditionalPath1;
  }

  @Nullable
  public String getAdditionalPath2() {
    return myAdditionalPath2;
  }

  @NotNull
  public String getVersion() {
    return myVersion;
  }

  @NotNull
  public Date getDate() {
    return myDate;
  }

  @NotNull
  public String getUser() {
    return myUser;
  }

  @Nullable
  public String getChangeName() {
    return myChangeName;
  }

  @Nullable
  public String getComment() {
    return myComment;
  }

  @NotNull
  public RawChangeInfoType getType() {
    return myType;
  }

  @Override
  public String toString() {
    return "RawChangeInfo{" +
      "myPath='" + myPath + '\'' +
      ", myAdditionalPath1='" + myAdditionalPath1 + '\'' +
      ", myAdditionalPath2='" + myAdditionalPath2 + '\'' +
      ", myTxId=" + myVersion +
      ", myDate=" + myDate +
      ", myUser='" + myUser + '\'' +
      ", myChangeName='" + myChangeName + '\'' +
      ", myComment='" + myComment + '\'' +
      ", myType=" + myType +
      '}';
  }

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
//        public static final byte Rollback = -26;

  public static enum RawChangeInfoType {
    ADDED(10, "Added"),
    DELETED(80, "Deleted"),
    RENAMED(-86, "Renamed"),
    RENAMED_ITEM(-76, "RenamedItem"),
    MOVED_TO(-126, "MovedTo"),
    SHARED_TO(-66, "SharedTo"),
    CHECK_IN(60, "CheckIn"),
    UNDELETED(-46, "Undeleted"),
    /* ----*/
    BRANCHED_FROM_ITEM(30, "BranchedFromItem"),
    ROLLBACK(-26, "Rollback"),

    NOT_CHANGED(0, "NotChanged");

    private final int myId;
    @NotNull private final String myName;

    @NotNull
    public static RawChangeInfoType getType(@NotNull final String name) {
      final RawChangeInfoType type = CollectionsUtil.findFirst(Arrays.asList(values()), new Filter<RawChangeInfoType>() {
        public boolean accept(@NotNull RawChangeInfoType data) {
          return name.equals(data.getName());
        }
      });
      return type == null ? NOT_CHANGED : type;
    }

    private RawChangeInfoType(int id, @NotNull String name) {
      myId = id;
      myName = name;
    }

    private int getId() {
      return myId;
    }

    @NotNull
    public String getName() {
      return myName;
    }

    @NotNull
    public VcsChangeInfo.Type getChangeInfoType(boolean isFile) {
      switch (this) {
        case ADDED:
          return getAddedType(isFile);
        case DELETED:
          return getRemovedType(isFile);
        case RENAMED:
          return getAddedType(isFile);
        case RENAMED_ITEM:
          return getAddedType(isFile);
        case MOVED_TO:
          return getAddedType(isFile);
        case SHARED_TO:
          return getAddedType(isFile);
        case CHECK_IN:
          return getChangedType(isFile);
        case UNDELETED:
          return getAddedType(isFile);
        case BRANCHED_FROM_ITEM:
          return getAddedType(isFile);
        case ROLLBACK:
          return getChangedType(isFile);
        default:
          return VcsChangeInfo.Type.NOT_CHANGED;
      }
    }

    @NotNull
    private VcsChangeInfo.Type getChangedType(boolean isFile) {
      return isFile ? VcsChangeInfo.Type.CHANGED : VcsChangeInfo.Type.DIRECTORY_CHANGED;
    }

    @NotNull
    private VcsChangeInfo.Type getRemovedType(boolean isFile) {
      return isFile ? VcsChangeInfo.Type.REMOVED : VcsChangeInfo.Type.DIRECTORY_REMOVED;
    }

    @NotNull
    private VcsChangeInfo.Type getAddedType(boolean isFile) {
      return isFile ? VcsChangeInfo.Type.ADDED : VcsChangeInfo.Type.DIRECTORY_ADDED;
    }
  }
}
