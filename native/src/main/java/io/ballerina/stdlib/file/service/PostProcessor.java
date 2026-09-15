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

import io.ballerina.stdlib.file.utils.FileConstants;
import io.ballerina.stdlib.file.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Executes post-processing actions on the file of a delivered event.
 */
public final class PostProcessor {

    private static final Logger log = LoggerFactory.getLogger(PostProcessor.class);

    private PostProcessor() {
    }

    /**
     * Executes a post-processing action. Failures are printed and never propagated.
     *
     * @param action        The action to execute
     * @param filePath      The path delivered with the event
     * @param watchRoot     The directory watched by the listener
     * @param recursive     Whether the listener watches subdirectories
     * @param actionContext The annotation field the action came from, for messages
     * @param methodName    The remote function the action belongs to, for messages
     */
    public static void executePostProcessAction(PostProcessAction action, String filePath, Path watchRoot,
                                                boolean recursive, String actionContext, String methodName) {
        Path source = Paths.get(filePath);
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(source, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (NoSuchFileException e) {
            log.debug("Skipping {} action of {}: file no longer exists: {}", actionContext, methodName, filePath);
            return;
        } catch (IOException | RuntimeException e) {
            printFailure(action, source, actionContext, methodName, e.getClass().getSimpleName() + ": "
                    + e.getMessage());
            return;
        }
        if (!attributes.isRegularFile()) {
            log.debug("Skipping {} action of {}: not a regular file: {}", actionContext, methodName, filePath);
            return;
        }
        try {
            if (action.isDelete()) {
                executeDeleteAction(source, actionContext);
            } else if (action.isMove()) {
                executeMoveAction(source, watchRoot, recursive, action, actionContext);
            }
        } catch (IOException | RuntimeException e) {
            printFailure(action, source, actionContext, methodName, e.getClass().getSimpleName() + ": "
                    + e.getMessage());
        } catch (InvalidFunctionConfigException e) {
            printFailure(action, source, actionContext, methodName, e.getMessage());
        }
    }

    private static void executeDeleteAction(Path source, String actionContext) throws IOException {
        Files.deleteIfExists(source);
        log.debug("Deleted file during {}: {}", actionContext, source);
    }

    private static void executeMoveAction(Path source, Path watchRoot, boolean recursive, PostProcessAction action,
                                          String actionContext) throws IOException, InvalidFunctionConfigException {
        Path destination = calculateMoveDestination(source, watchRoot, action);
        if (recursive && PathUtil.resolve(destination).startsWith(PathUtil.resolve(watchRoot))) {
            throw new InvalidFunctionConfigException("destination '" + destination
                    + "' is inside the recursively watched directory '" + watchRoot + "'");
        }
        Path parent = destination.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.move(source, destination);
        log.debug("Moved file during {}: {} -> {}", actionContext, source, destination);
    }

    private static Path calculateMoveDestination(Path source, Path watchRoot, PostProcessAction action) {
        Path moveTo = Paths.get(action.getMoveTo()).toAbsolutePath().normalize();
        Path absoluteSource = source.toAbsolutePath().normalize();
        Path root = watchRoot.toAbsolutePath().normalize();
        if (action.isPreserveSubDirs() && absoluteSource.startsWith(root)) {
            return moveTo.resolve(root.relativize(absoluteSource));
        }
        Path fileName = absoluteSource.getFileName();
        return fileName == null ? moveTo : moveTo.resolve(fileName);
    }

    private static void printFailure(PostProcessAction action, Path source, String actionContext, String methodName,
                                     String reason) {
        FileUtils.getBallerinaError(FileConstants.FILE_SYSTEM_ERROR, "Failed to execute " + actionContext
                + " action " + action + " of " + methodName + " on file '" + source + "': " + reason).printStackTrace();
    }
}
