/*
 * Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.file.testutils;

import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Utils needed for Ballerina tests.
 */
public class TestUtil {
    private static Path file;
    private static Path symLinkPath;

    public static void createTestFile() throws IOException {
        file = Files.createFile(Paths.get("tests", "resources", "test1.txt"));
    }

    public static void modifyTestFile() throws IOException {
        Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
    }

    public static void deleteTestFile() throws IOException {
        Files.deleteIfExists(file);
    }

    public static BString getTmpDir() {
        return StringUtils.fromString(System.getProperty("java.io.tmpdir"));
    }

    public static BString getUserDir() {
        return StringUtils.fromString(System.getProperty("user.dir"));
    }

    public static BString getAbsPath(String path) {
        String abs = Paths.get(path).toAbsolutePath().toString();
        return StringUtils.fromString(abs);
    }

    public static void createLink() {
        Path filePath = Paths.get("tests", "resources", "test.txt");
        symLinkPath = Paths.get(System.getProperty("java.io.tmpdir"), "test_link.txt");
        try {
            Files.deleteIfExists(symLinkPath);
            Files.createSymbolicLink(symLinkPath, filePath);
        } catch (IOException e) {
            ErrorCreator.createError(StringUtils.fromString("Error creating symlink!"), e);
        }
    }

    public static void removeLink() {
        if (symLinkPath != null) {
            try {
                Files.deleteIfExists(symLinkPath);
            } catch (IOException e) {
                ErrorCreator.createError(StringUtils.fromString("Error removing symlink!"), e);
            }
        }
    }

    public static Object createFileAt(BString path, BString content) {
        try {
            Files.writeString(Paths.get(path.getValue()), content.getValue());
            return null;
        } catch (IOException e) {
            return toError(e);
        }
    }

    public static Object createEmptyFileAt(BString path) {
        try {
            Files.createFile(Paths.get(path.getValue()));
            return null;
        } catch (IOException e) {
            return toError(e);
        }
    }

    public static Object touchFile(BString path) {
        try {
            Files.setLastModifiedTime(Paths.get(path.getValue()), FileTime.fromMillis(System.currentTimeMillis()));
            return null;
        } catch (IOException e) {
            return toError(e);
        }
    }

    public static Object createDirAt(BString path) {
        try {
            Files.createDirectories(Paths.get(path.getValue()));
            return null;
        } catch (IOException e) {
            return toError(e);
        }
    }

    public static Object createSymLinkAt(BString link, BString target) {
        try {
            Files.createSymbolicLink(Paths.get(link.getValue()), Paths.get(target.getValue()).toAbsolutePath());
            return null;
        } catch (IOException | UnsupportedOperationException e) {
            return toError(e);
        }
    }

    public static Object deleteTree(BString path) {
        Path root = Paths.get(path.getValue());
        if (Files.notExists(root)) {
            return null;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
            return null;
        } catch (IOException e) {
            return toError(e);
        }
    }

    public static Object getRealPath(BString path) {
        try {
            return StringUtils.fromString(Paths.get(path.getValue()).toRealPath().toString());
        } catch (IOException e) {
            return toError(e);
        }
    }

    private static BError toError(Exception e) {
        return ErrorCreator.createError(StringUtils.fromString(e.getClass().getSimpleName() + ": " + e.getMessage()));
    }

    public static BString getSymLink() {

        String link = null;
        try {
            link = Files.readSymbolicLink(symLinkPath).toString();
        } catch (IOException e) {
            ErrorCreator.createError(StringUtils.fromString("Error retrieving symlink!"), e);
        }
        return StringUtils.fromString(link);
    }
}
