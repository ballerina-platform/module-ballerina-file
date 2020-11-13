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

package org.ballerinalang.stdlib.file;

import io.ballerina.runtime.api.values.BError;
import org.ballerinalang.stdlib.file.utils.Constants;
import org.ballerinalang.stdlib.file.utils.Utils;
import org.junit.jupiter.api.Test;

import java.nio.file.InvalidPathException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test utility functions in path module.
 *
 * @since 0.995.0
 */
public class UtilsTest {

    @Test
    public void testGetPathError() {
        InvalidPathException exp = new InvalidPathException("/User/ballerina/path\\test", "Invalid path format");

        // Get Path error with reason and throwable.
        BError error1 = Utils.getPathError(Constants.INVALID_PATH_ERROR, exp.getMessage());
        assertEquals("Invalid path format: /User/ballerina/path\\test", error1.getMessage());

        // Get Path error without reason.
        BError error2 = Utils.getPathError(null, exp.getMessage());
        assertEquals( "Invalid path format: /User/ballerina/path\\test", error2.getMessage());

        // Get Path error without throwable.
        BError error3 = Utils.getPathError(Constants.INVALID_PATH_ERROR, null);
        assertEquals("Unknown Error", error3.getMessage());

        // Get Path error without both reason and throwable.
        BError error4 = Utils.getPathError(null, null);
        assertEquals("Unknown Error", error4.getMessage());

        // Get Path error without throwable message.
        Exception exp2 = new Exception();
        BError error5 = Utils.getPathError(Constants.INVALID_PATH_ERROR, exp2.getMessage());
        assertEquals("Unknown Error", error5.getMessage());
    }
}
