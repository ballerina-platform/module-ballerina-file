/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.file.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Path normalization shared by the attach-time checks and the post-processing executor.
 */
final class PathUtil {

    private PathUtil() {
    }

    /**
     * Resolves a path to an absolute, normalized form with symbolic links resolved for the part that exists.
     *
     * @param path The path to resolve
     * @return The resolved path
     */
    static Path resolve(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        Path existing = absolute;
        Path missing = null;
        while (existing != null && Files.notExists(existing, LinkOption.NOFOLLOW_LINKS)) {
            Path name = existing.getFileName();
            if (name == null) {
                return absolute;
            }
            missing = missing == null ? name : name.resolve(missing);
            existing = existing.getParent();
        }
        if (existing == null) {
            return absolute;
        }
        try {
            Path real = existing.toRealPath();
            return missing == null ? real : real.resolve(missing);
        } catch (IOException e) {
            return absolute;
        }
    }
}
