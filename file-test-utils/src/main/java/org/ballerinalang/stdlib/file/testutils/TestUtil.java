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

package org.ballerinalang.stdlib.file.testutils;

import io.ballerina.runtime.api.StringUtils;
import io.ballerina.runtime.api.values.BString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

public class TestUtil {
    public static Path file;

    public static void createTestFile() throws Exception {
        file = Files.createFile(Paths.get("src", "file", "tests", "resources", "test.txt"));
    }

    public static void modifyTestFile() throws Exception {
        Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
    }

    public static void deleteTestFile() throws Exception {
        Files.deleteIfExists(file);
    }

    public static BString getTmpDir() {
        return StringUtils.fromString(System.getProperty("java.io.tmpdir"));
    }

    public static BString getUserDir() {
        return StringUtils.fromString(System.getProperty("user.dir"));
    }
}
