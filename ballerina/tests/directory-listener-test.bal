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
//

import ballerina/test;
import ballerina/lang.runtime as runtime;
import ballerina/jballerina.java;

listener Listener localFolder = new ({
    path: "tests/resources",
    recursive: false
});

boolean createInvoke = false;
boolean modifyInvoke = false;
boolean deleteInvoke = false;

service Service "filesystem" on localFolder {

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
    if fileResult is error {
        test:assertFail("File not opened!");
    } else {
        runtime:sleep(2);
        test:assertTrue(createInvoke, "File creation event not captured!");
    }
}

@test:Config { dependsOn: [isCreateInvoked]}
function isModifyInvoked() {
     error? fileResult = modifyTestFile();
    if fileResult is error {
        test:assertFail("File not modified!");
    } else {
        runtime:sleep(2);
        test:assertTrue(modifyInvoke, "File modification event not captured!");
    }
}

@test:Config { dependsOn: [isCreateInvoked, isModifyInvoked]}
function isDeleteInvoked() {
    error? fileResult = deleteTestFile();
    if fileResult is error {
        test:assertFail("File not deleted!");
    } else {
        runtime:sleep(2);
        test:assertTrue(deleteInvoke, "File deletion event not captured!");
    }
}

Listener|error localFolder1 = new ({
    path: "tests/test",
    recursive: false
});

@test:Config {}
function testDirectoryNotExist() {
    Listener|error temporaryLoader = localFolder1;
    if temporaryLoader is error {
        test:assertTrue(temporaryLoader.message().includes("Folder does not exist: tests/test"));
    } else {
        test:assertFail("Test Failed!");
    }
}

Listener|error localFolder2 = new ({
    path: "",
    recursive: false
});

@test:Config {}
function testDirectoryEmpty() {
    Listener|error temporaryLoader = localFolder2;
    if temporaryLoader is error {
        test:assertTrue(temporaryLoader.message().includes("'path' field is empty"));
    } else {
        test:assertFail("Test Failed!");
    }
}

Listener|error localFolder3 = new ({
    path: "tests/resources/test.txt",
    recursive: false
});

@test:Config {}
function testNotDirectory() {
    Listener|error temporaryLoader = localFolder3;
    if temporaryLoader is error {
        test:assertTrue(temporaryLoader.message().includes("Unable to find a directory: tests/resources/test.txt"));
    } else {
        test:assertFail("Test Failed!");
    }
}


Listener|error localFolder5 = new ({
    path: "tests/resources",
    recursive: false
});

Service attachService = service object {
};

@test:Config {}
function testAttachEmptyService() {
    Listener|error temporaryLoader = localFolder5;
    if temporaryLoader is Listener {
        error? result = trap temporaryLoader.attach(attachService);
        if result is error {
            test:assertTrue(result.message().includes("At least a single resource required from following"));
        } else {
            test:assertFail("Attach service test Failed!");
        }
    } else {
        test:assertFail("Test Failed!");
    }
}

Service attachService1 = service object {

    remote function onCreate(FileEvent m) {
        createInvoke = true;
    }
};

@test:Config {}
function testService() returns error? {
    Listener|error temporaryLoader = localFolder5;
    if temporaryLoader is Listener {
        check temporaryLoader.attach(attachService1);
        check temporaryLoader.'start();
        check temporaryLoader.detach(attachService1);
        check temporaryLoader.immediateStop();
    } else {
        test:assertFail("Test Failed!");
    }
}

function createTestFile() returns error? = @java:Method {
    'class: "io.ballerina.stdlib.file.testutils.TestUtil"
} external;

function modifyTestFile() returns error? = @java:Method {
    'class: "io.ballerina.stdlib.file.testutils.TestUtil"
} external;

function deleteTestFile() returns error? = @java:Method {
    'class: "io.ballerina.stdlib.file.testutils.TestUtil"
} external;
