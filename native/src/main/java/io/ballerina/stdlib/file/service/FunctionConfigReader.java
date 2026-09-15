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

import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.file.utils.ModuleUtils;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static io.ballerina.stdlib.file.service.DirectoryListenerConstants.ANNOTATION_AFTER_ERROR;
import static io.ballerina.stdlib.file.service.DirectoryListenerConstants.ANNOTATION_AFTER_PROCESS;
import static io.ballerina.stdlib.file.service.DirectoryListenerConstants.ANNOTATION_MOVE_TO;
import static io.ballerina.stdlib.file.service.DirectoryListenerConstants.ANNOTATION_PRESERVE_SUB_DIRS;
import static io.ballerina.stdlib.file.service.DirectoryListenerConstants.FUNCTION_CONFIG_ANNOTATION;
import static io.ballerina.stdlib.file.service.DirectoryListenerConstants.RESOURCE_NAME_ON_CREATE;
import static io.ballerina.stdlib.file.service.DirectoryListenerConstants.RESOURCE_NAME_ON_MODIFY;

/**
 * Reads the {@code @file:FunctionConfig} annotations of a service and validates them against the listener.
 */
public final class FunctionConfigReader {

    private FunctionConfigReader() {
    }

    /**
     * Reads the post-processing actions declared on the remote functions of a service.
     *
     * @param serviceType The service object type
     * @param watchRoot   The directory watched by the listener
     * @param recursive   Whether the listener watches subdirectories
     * @return The declared actions
     * @throws InvalidFunctionConfigException if an annotation is misplaced or its values are not usable
     */
    public static PostProcessConfig read(ObjectType serviceType, Path watchRoot, boolean recursive)
            throws InvalidFunctionConfigException {
        PostProcessConfig config = new PostProcessConfig();
        for (MethodType method : serviceType.getMethods()) {
            Optional<BMap<BString, Object>> annotation = getFunctionConfigAnnotation(method);
            if (annotation.isEmpty()) {
                continue;
            }
            String methodName = method.getName();
            if (!RESOURCE_NAME_ON_CREATE.equals(methodName) && !RESOURCE_NAME_ON_MODIFY.equals(methodName)) {
                throw new InvalidFunctionConfigException("'" + FUNCTION_CONFIG_ANNOTATION + "' annotation is not "
                        + "allowed on the '" + methodName + "' remote function, only '" + RESOURCE_NAME_ON_CREATE
                        + "' and '" + RESOURCE_NAME_ON_MODIFY + "' support post-processing actions");
            }
            PostProcessAction afterProcess = parsePostProcessAction(annotation.get(), ANNOTATION_AFTER_PROCESS,
                    methodName, watchRoot, recursive);
            PostProcessAction afterError = parsePostProcessAction(annotation.get(), ANNOTATION_AFTER_ERROR,
                    methodName, watchRoot, recursive);
            config.put(methodName, afterProcess, afterError);
        }
        return config;
    }

    @SuppressWarnings("unchecked")
    private static Optional<BMap<BString, Object>> getFunctionConfigAnnotation(MethodType method) {
        Module module = ModuleUtils.getModule();
        BString packagePath = StringUtils.fromString(String.format("%s/%s:%s", module.getOrg(), module.getName(),
                module.getMajorVersion()));
        Object annotation = method.getAnnotation(packagePath, StringUtils.fromString(FUNCTION_CONFIG_ANNOTATION));
        if (annotation instanceof BMap) {
            return Optional.of((BMap<BString, Object>) annotation);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static PostProcessAction parsePostProcessAction(BMap<BString, Object> annotation, String fieldName,
                                                            String methodName, Path watchRoot, boolean recursive)
            throws InvalidFunctionConfigException {
        Object action = annotation.get(StringUtils.fromString(fieldName));
        if (action == null) {
            return null;
        }
        if (action instanceof BString) {
            return PostProcessAction.delete();
        }
        BMap<BString, Object> moveRecord = (BMap<BString, Object>) action;
        Object moveToValue = moveRecord.get(StringUtils.fromString(ANNOTATION_MOVE_TO));
        String moveTo = moveToValue instanceof BString value ? value.getValue() : "";
        if (moveTo.trim().isEmpty()) {
            throw new InvalidFunctionConfigException("Move action in '" + fieldName + "' for remote function '"
                    + methodName + "' has an empty '" + ANNOTATION_MOVE_TO + "' path");
        }
        Object preserveValue = moveRecord.get(StringUtils.fromString(ANNOTATION_PRESERVE_SUB_DIRS));
        boolean preserveSubDirs = !(preserveValue instanceof Boolean preserve) || preserve;
        validateMoveTo(moveTo, fieldName, methodName, watchRoot, recursive);
        return PostProcessAction.move(moveTo, preserveSubDirs);
    }

    private static void validateMoveTo(String moveTo, String fieldName, String methodName, Path watchRoot,
                                       boolean recursive) throws InvalidFunctionConfigException {
        String prefix = "Move action in '" + fieldName + "' for remote function '" + methodName + "': '"
                + ANNOTATION_MOVE_TO + "' path '" + moveTo + "' ";
        Path destination;
        try {
            destination = PathUtil.resolve(Paths.get(moveTo));
        } catch (InvalidPathException e) {
            throw new InvalidFunctionConfigException(prefix + "is not a valid path: " + e.getReason());
        }
        Path root = PathUtil.resolve(watchRoot);
        if (destination.equals(root)) {
            throw new InvalidFunctionConfigException(prefix + "is the watched directory '" + watchRoot + "'");
        }
        if (recursive && destination.startsWith(root)) {
            throw new InvalidFunctionConfigException(prefix + "is inside the recursively watched directory '"
                    + watchRoot + "'");
        }
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(destination)) {
            throw new InvalidFunctionConfigException(prefix + "is not a directory");
        }
    }
}
