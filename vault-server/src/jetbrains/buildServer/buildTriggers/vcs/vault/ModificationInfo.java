/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

import java.util.Date;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Victory.Bedrosova on 8/20/13.
 *
 * Represents group of VCS changes in one commit
 * @see ChangeInfo
 */
public class ModificationInfo {
  @NotNull
  private final String myVersion;
  @NotNull
  private final String myDisplayVersion;
  @NotNull
  private final String myUser;
  @NotNull
  private final String myComment;
  @NotNull
  private final Date myDate;

  public ModificationInfo(@NotNull String version, @NotNull String displayVersion, @NotNull String user, @Nullable String comment, @NotNull Date date) {
    myVersion = version;
    myDisplayVersion = displayVersion;
    myUser = user;
    myComment = fixComment(comment);
    myDate = date;
  }

  @SuppressWarnings("ConstantConditions")
  @NotNull
  private String fixComment(@Nullable String comment) {
    return StringUtil.isEmpty(comment) ? "No comment" : comment;
  }

  @NotNull
  public String getVersion() {
    return myVersion;
  }

  @NotNull
  public String getPrevVersion() {
    return String.valueOf(Long.parseLong(myVersion) - 1);
  }

  @NotNull
  public String getDisplayVersion() {
    return myDisplayVersion;
  }

  @NotNull
  public String getUser() {
    return myUser;
  }

  @NotNull
  public Date getDate() {
    return myDate;
  }

  @NotNull
  public String getComment() {
    return myComment;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ModificationInfo that = (ModificationInfo) o;

    if (!myComment.equals(that.myComment)) return false;
    if (!myDate.equals(that.myDate)) return false;
    if (!myUser.equals(that.myUser)) return false;
    return myVersion.equals(that.myVersion);
  }

  @Override
  public int hashCode() {
    int result = myVersion.hashCode();
    result = 31 * result + myUser.hashCode();
    result = 31 * result + myComment.hashCode();
    result = 31 * result + myDate.hashCode();
    return result;
  }
}
