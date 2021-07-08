/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.file.nativeimpl;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.file.utils.FileConstants;
import io.ballerina.stdlib.file.utils.FileUtils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotLinkException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Native function implementations for the filepath module APIs.
 *
 * @since 1.0.5
 */
public class FilePathUtils {

    public static Object absolute(BString inputPath) {
        try {
            return StringUtils.fromString(
                    FileSystems.getDefault().getPath(inputPath.getValue()).toAbsolutePath().toString());
        } catch (InvalidPathException ex) {
            return FileUtils.getPathError(FileConstants.INVALID_PATH_ERROR, "Invalid path " + inputPath);
        }
    }

    public static Object resolve(BString inputPath) {
        try {
            Path realPath = Files.readSymbolicLink(Paths.get(inputPath.getValue()).toAbsolutePath());
            return StringUtils.fromString(realPath.toString());
        } catch (NotLinkException ex) {
            return FileUtils.getPathError(FileConstants.NOT_LINK_ERROR, "Path is not a symbolic link " + inputPath);
        } catch (NoSuchFileException ex) {
            return FileUtils.getPathError(FileConstants.FILE_NOT_FOUND_ERROR, "File does not exist at " + inputPath);
        } catch (IOException ex) {
            return FileUtils.getPathError(FileConstants.IO_ERROR, "IO error for " + inputPath);
        } catch (SecurityException ex) {
            return FileUtils.getPathError(FileConstants.SECURITY_ERROR, "Security error for " + inputPath);
        }
    }

    private FilePathUtils() {}
}
