/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.stdlib.filepath.testutils;

import org.ballerinalang.jvm.StringUtils;
import org.ballerinalang.jvm.util.exceptions.BallerinaException;
import org.ballerinalang.jvm.values.api.BString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestUtil {
    private static Path symLinkPath;

    public static BString getAbsPath(String path) {
        String abs = Paths.get(path).toAbsolutePath().toString();
        return StringUtils.fromString(abs);
    }

    public static boolean isAbsPath(String path) {
        boolean isAbs = Paths.get(path).isAbsolute();
        return isAbs;
    }

    public static void createLink() {
        Path filePath = Paths.get("src", "filepath", "tests", "resources", "test.txt");
        symLinkPath = Paths.get(System.getProperty("java.io.tmpdir"), "test_link.txt");
        try {
            Files.deleteIfExists(symLinkPath);
            Files.createSymbolicLink(symLinkPath, filePath);
        } catch (IOException e) {
            throw new BallerinaException("Error creating symlink!");
        }
    }

    public static void removeLink() {
        if (symLinkPath != null) {
            try {
                Files.deleteIfExists(symLinkPath);
            } catch (IOException e) {
                throw new BallerinaException("Error removing symlink!");
            }
        }
    }

    public static BString getSymLink() {

        String link = null;
        try {
            link = Files.readSymbolicLink(symLinkPath).toString();
        } catch (IOException e) {
            throw new BallerinaException("Error retrieving symlink!");
        }
        return StringUtils.fromString(link);
    }
}
