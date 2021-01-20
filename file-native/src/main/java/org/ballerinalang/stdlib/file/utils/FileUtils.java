/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinalang.stdlib.file.utils;

import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.stdlib.time.util.TimeUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.ballerinalang.stdlib.file.utils.FileConstants.ABS_PATH;
import static org.ballerinalang.stdlib.file.utils.FileConstants.DIR;
import static org.ballerinalang.stdlib.file.utils.FileConstants.METADATA;
import static org.ballerinalang.stdlib.file.utils.FileConstants.MODIFIED_TIME;
import static org.ballerinalang.stdlib.file.utils.FileConstants.READABLE;
import static org.ballerinalang.stdlib.file.utils.FileConstants.SIZE;
import static org.ballerinalang.stdlib.file.utils.FileConstants.WRITABLE;

/**
 * @since 0.94.1
 */
public class FileUtils {

    private static final String UNKNOWN_MESSAGE = "Unknown Error";

    /**
     * Returns error object for input reason.
     * Error type is generic ballerina error type. This utility to construct error object from message.
     *
     * @param error Reason for creating the error object. If the reason is null, "UNKNOWN" sets by
     *              default.
     * @param ex    Java throwable object to capture description of error struct. If throwable object is null,
     *              "Unknown Error" sets to message by default.
     * @return Ballerina error object.
     */
    public static BError getBallerinaError(String error, Throwable ex) {
        String errorMsg = error != null && ex.getMessage() != null ? ex.getMessage() : UNKNOWN_MESSAGE;
        return getBallerinaError(error, errorMsg);
    }

    /**
     * Returns error object for input reason and details.
     * Error type is generic ballerina error type. This utility to construct error object from message.
     *
     * @param error   The specific error type.
     * @param message Error message. "Unknown Error" is set to message by default.
     * @return Ballerina error object.
     */
    public static BError getBallerinaError(String error, String message) {
        return ErrorCreator.createDistinctError(error, ModuleUtils.getModule(),
                                                 StringUtils.fromString(message != null ? message : UNKNOWN_MESSAGE));
    }

    public static Object getMetaData(File inputFile) throws IOException {
        BMap<BString, Object> lastModifiedInstance;
        FileTime lastModified = Files.getLastModifiedTime(inputFile.toPath());
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(lastModified.toString());
        lastModifiedInstance = TimeUtils.createTimeRecord(lastModified.toMillis(),
                StringUtils.fromString(zonedDateTime.getZone().toString()));
        Map<String, Object> metadataRecord = new HashMap<>();
        metadataRecord.put(ABS_PATH, inputFile.getAbsolutePath());
        metadataRecord.put(SIZE, inputFile.length());
        metadataRecord.put(MODIFIED_TIME, lastModifiedInstance);
        metadataRecord.put(DIR, inputFile.isDirectory());
        metadataRecord.put(READABLE, Files.isReadable(inputFile.toPath()));
        metadataRecord.put(WRITABLE, Files.isWritable(inputFile.toPath()));
        return ValueCreator.createRecordValue(ModuleUtils.getModule(), METADATA, metadataRecord);
    }


    /**
     * Returns the system property which corresponds to the given key.
     *
     * @param key system property key
     * @return system property as a {@link String} or {@code PredefinedTypes.TYPE_STRING.getZeroValue()} if
     * the property does not exist.
     */
    public static String getSystemProperty(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            return io.ballerina.runtime.api.PredefinedTypes.TYPE_STRING.getZeroValue();
        }
        return value;
    }

    /**
     * Returns error record for input reason and details. This utility to construct error struct from the reason and
     * message description.
     *
     * @param reason  Valid error reason. If the reason is null, "{ballerina/filepath}GenericError" is set as the reason
     *                by default
     * @param details Description of the error message. If the message is null, "Unknown Error" is set to message by
     *                default.
     * @return Ballerina error object.
     */
    public static BError getPathError(String reason, String details) {
        if (reason == null) {
            reason = FileConstants.GENERIC_ERROR;
        }
        if (details == null) {
            details = UNKNOWN_MESSAGE;
        }
        return ErrorCreator.createDistinctError(reason, ModuleUtils.getModule(), StringUtils.fromString(details));
    }

    private FileUtils() {
    }
}
