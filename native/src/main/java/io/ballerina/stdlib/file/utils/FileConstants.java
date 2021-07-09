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

package io.ballerina.stdlib.file.utils;


/**
 * Constants for file package functions.
 *
 * @since 0.995.0
 */
public class FileConstants {
    public static final String METADATA = "MetaData";

    // File error type IDs
    public static final String INVALID_OPERATION_ERROR = "InvalidOperationError";
    public static final String PERMISSION_ERROR = "PermissionError";
    public static final String FILE_SYSTEM_ERROR = "FileSystemError";
    public static final String FILE_NOT_FOUND_ERROR = "FileNotFoundError";
    public static final String NOT_LINK_ERROR = "NotLinkError";
    public static final String IO_ERROR = "IOError";
    public static final String SECURITY_ERROR = "SecurityError";
    public static final String INVALID_PATH_ERROR = "InvalidPathError";
    public static final String GENERIC_ERROR = "GenericError";
    static final String ERROR_DETAILS = "Detail";
    static final String ERROR_MESSAGE = "message";

    // System constant fields
    public static final int MAX_DEPTH = 1;

    // Enum constants

    public static final String REPLACE_EXISTING = "REPLACE_EXISTING";
    public static final String COPY_ATTRIBUTES = "COPY_ATTRIBUTES";
    public static final String NO_FOLLOW_LINKS = "NO_FOLLOW_LINKS";
    public static final String EXISTS = "EXISTS";
    public static final String IS_DIR = "IS_DIR";
    public static final String IS_SYMLINK = "IS_SYMLINK";
    public static final String READABLE = "READABLE";
    public static final String WRITABLE = "WRITABLE";
    public static final String RECURSIVE = "RECURSIVE";

    // Metadata fields

    public static final String ABS_PATH = "absPath";
    public static final String SIZE = "size";
    public static final String MODIFIED_TIME = "modifiedTime";
    public static final String DIR = "dir";
    public static final String META_DATA_READABLE = "readable";
    public static final String META_DATA_WRITABLE = "writable";

    // FileEvent struct field names
    public static final String FILE_EVENT_NAME = "name";

    public static final String FILE_EVENT_OPERATION = "operation";

    private FileConstants() {
    }
}
