/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.gapid.server;

import static java.util.logging.Level.INFO;

import com.google.common.collect.ImmutableList;
import com.google.gapid.models.Settings;
import com.google.gapid.util.Flags;
import com.google.gapid.util.Flags.Flag;
import com.google.gapid.util.OS;

import java.io.File;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Utility for locating the various GAPID binaries and associated files.
 */
public final class GapiPaths {
  public static final Flag<String> gapidPath =
      Flags.value("gapid", "", "Path to the gapid binaries.");
  public static final Flag<String> adbPath = Flags.value("adb", "", "Path to the adb binary.");

  private static final Logger LOG = Logger.getLogger(GapiPaths.class.getName());

  private static final String GAPIS_EXECUTABLE_NAME = "gapis" + OS.exeExtension;
  private static final String GAPIT_EXECUTABLE_NAME = "gapit" + OS.exeExtension;
  private static final String STRINGS_DIR_NAME = "strings";
  private static final String USER_HOME_GAPID_ROOT = "gapid";
  private static final String GAPID_PKG_SUBDIR = "pkg";
  private static final String GAPID_ROOT_ENV_VAR = "GAPID";

  private static File baseDir;
  private static File gapisPath;
  private static File gapitPath;
  private static File stringsPath;

  public static synchronized boolean isValid() {
    if (allExist()) {
      return true;
    }
    findTools();
    return allExist();
  }

  public static synchronized File base() {
    isValid();
    return baseDir;
  }

  public static synchronized File gapis() {
    isValid();
    return gapisPath;
  }

  public static synchronized File gapit() {
    isValid();
    return gapitPath;
  }

  public static synchronized File strings() {
    isValid();
    return stringsPath;
  }

  public static String adb(Settings settings) {
    String adb = adbPath.get();
    return adb.isEmpty() ? settings.adb : adb;
  }

  private static boolean checkForTools(File dir) {
    if (dir == null) {
      return false;
    }

    baseDir = dir;
    gapisPath = new File(dir, GAPIS_EXECUTABLE_NAME);
    gapitPath = new File(dir, GAPIT_EXECUTABLE_NAME);
    stringsPath = new File(dir, STRINGS_DIR_NAME);

    LOG.log(INFO, "Looking for GAPID in " + dir + " -> " + gapisPath);
    return allExist();
  }

  private static File join(File root, String... paths) {
    if (paths.length == 0) {
      return root;
    }
    StringBuilder sb = new StringBuilder().append(paths[0]);
    for (int i = 1; i < paths.length; i++) {
      sb.append(File.separatorChar).append(paths[i]);
    }
    return new File(root, sb.toString());
  }

  private static boolean allExist() {
    // We handle a missing strings and gapit explicitly, so ignore if they're missing.
    return gapisPath != null && gapisPath.exists();
  }

  private static void findTools() {
    ImmutableList.<Supplier<File>>of(
      () -> {
        String gapidRoot = gapidPath.get();
        return "".equals(gapidRoot) ? null : new File(gapidRoot);
      }, () -> {
        String gapidRoot = System.getenv(GAPID_ROOT_ENV_VAR);
        return gapidRoot != null && gapidRoot.length() > 0 ? new File(gapidRoot) : null;
      },
      () -> join(new File(OS.userHomeDir), USER_HOME_GAPID_ROOT),
      () -> join(new File(OS.userHomeDir), USER_HOME_GAPID_ROOT, GAPID_PKG_SUBDIR)
      //TODO GapiPaths::getSdkPath
    ).stream().filter(p -> checkForTools(p.get())).findFirst();
  }
}
