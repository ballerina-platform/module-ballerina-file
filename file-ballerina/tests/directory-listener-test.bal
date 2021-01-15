// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/test;
import ballerina/lang.runtime as runtime;
import ballerina/java;

listener Listener localFolder = new ({
    path: "tests/resources",
    recursive: false
});

boolean createInvoke = false;
boolean modifyInvoke = false;
boolean deleteInvoke = false;

service "filesystem" on localFolder {

    remote function onCreate(FileEvent m) {
        createInvoke = true;
    }

    remote function onModify(FileEvent m) {
        modifyInvoke = true;
    }

    remote function onDelete(FileEvent m) {
        deleteInvoke = true;
    }
}

@test:Config {}
function isCreateInvoked() {
    error? fileResult = createTestFile();
    if (fileResult is error) {
        test:assertFail("File not opened!");
    } else {
        runtime:sleep(2);
        test:assertTrue(createInvoke, "File creation event not captured!");
    }
}

@test:Config { dependsOn: [isCreateInvoked]}
function isModifyInvoked() {
     error? fileResult = modifyTestFile();
    if (fileResult is error) {
        test:assertFail("File not modified!");
    } else {
        runtime:sleep(2);
        test:assertTrue(modifyInvoke, "File modification event not captured!");
    }
}

@test:Config { dependsOn: [isCreateInvoked, isModifyInvoked]}
function isDeleteInvoked() {
    error? fileResult = deleteTestFile();
    if (fileResult is error) {
        test:assertFail("File not deleted!");
    } else {
        runtime:sleep(2);
        test:assertTrue(deleteInvoke, "File deletion event not captured!");
    }
}

function createTestFile() returns error? = @java:Method {
    'class: "org.ballerinalang.stdlib.file.testutils.TestUtil"
} external;

function modifyTestFile() returns error? = @java:Method {
    'class: "org.ballerinalang.stdlib.file.testutils.TestUtil"
} external;

function deleteTestFile() returns error? = @java:Method {
    'class: "org.ballerinalang.stdlib.file.testutils.TestUtil"
} external;
