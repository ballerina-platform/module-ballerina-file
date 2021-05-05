/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.file.compiler;

/**
 * Error codes of file compiler plugin validation.
 */
public enum ErrorCodes {

    FILE_101("invalid parameter type `{0}` provided for remote function. Only file:FileEvent is allowed as " +
            "the parameter type", "FILE_101"),
    FILE_102("missing remote keyword in the remote function `{0}`", "FILE_102"),
    FILE_103("invalid function name `{0}`, file listener only supports " +
            "`onCreate`, `onModify` and `onDelete` remote functions", "FILE_103"),
    FILE_104("return types are not allowed in the remote function `{0}`", "FILE_104"),
    FILE_105("the remote function should only contain file:FileEvent parameter", "FILE_105"),
    FILE_106("at least a single remote function required in the service", "FILE_106");

    private final String error;
    private final String errorCode;

    ErrorCodes(String error, String errorCode) {
        this.error = error;
        this.errorCode = errorCode;
    }

    public String getError() {
        return error;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
