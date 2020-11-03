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
import ballerina/java;
import ballerina/stringutils;

string tmpdir = getTmpDir();
string srcDir = "src/file/tests/resources/src-dir";
string rdDir = "src/file/tests/resources/read-dir";
string emptyDir = "src/file/tests/resources/empty-dir";
string noDir = "src/file/tests/resources/no-dir";

string srcFile = "src/file/tests/resources/src-file.txt";
string srcModifiedFile = "src/file/tests/resources/src-file-modified.txt";
string noFile = "/no-file.txt";
string srcFileRaw = "/src-file.txt";
string destFile = "/dest-file.txt";
string copyFile = "/cpy-file.txt";

int srcFileLength = 0;
int srcModifiedFileLength = 0;

@test:Config {}
function testRename() {
    error? copyResult = copy(srcFile, tmpdir + srcFileRaw, REPLACE_EXISTING);
    if (copyResult is error) {
        test:assertFail("File not copied!");
    }

    error? renameResult = rename(tmpdir + srcFileRaw, tmpdir + destFile);
    if (renameResult is error) {
        test:assertFail("File not renamed!");
    }
}

@test:Config {dependsOn: ["testRename"]}
function testRenameExisting() {
    error? copyResult = copy(srcFile, tmpdir + srcFileRaw, REPLACE_EXISTING);
    if (copyResult is error) {
        test:assertFail("File not copied!");
    }

    error? renameResult = rename(tmpdir + srcFileRaw, tmpdir + destFile);
    if (renameResult is error) {
        string expectedErrMsg = "File already exists in the new path ";
        test:assertTrue(stringutils:contains(renameResult.message(), expectedErrMsg));
    }
}

@test:Config {dependsOn: ["testRenameExisting"]}
function testRemove() {
    error? removeResult = remove(tmpdir + destFile);
    if (removeResult is error) {
        test:assertFail("File not removed!");
    }
}

@test:Config {}
function testCopyDir() {
    error? copyResult = copy(srcDir, tmpdir + "/src-dir");
    if (copyResult is error) {
        test:assertFail("Directory not copied!");
    }
}

@test:Config {dependsOn: ["testCopyDir"]}
function testRemoverecursivefalse() {
    error? removeResult = remove(tmpdir + "/src-dir");
    if (removeResult is error) {
        string expectedErrMsg = "Error while deleting";
        test:assertTrue(stringutils:contains(removeResult.message(), expectedErrMsg));
    }
}

@test:Config {dependsOn: ["testRemoverecursivefalse"]}
function testRemoverecursiveTrue() {
    error? removeResult = remove(tmpdir + "/src-dir", RECURSIVE);
    if (removeResult is error) {
        test:assertFail("File not removed!");
    }
}

@test:Config {}
function testRemoveNonExistingFile() {
    error? removeResult = remove(tmpdir + noFile);
    if (removeResult is error) {
        string expectedErrMsg = "File not found";
        test:assertTrue(stringutils:contains(removeResult.message(), expectedErrMsg));
    }
}

@test:Config {dependsOn: ["testRenameExisting"]}
function testMetadata() {
    MetaData|error metadata = getMetaData(tmpdir + srcFileRaw);
    if (metadata is MetaData) {
        //test:assertEquals(metadata.getName(), "src-file.txt", "Incorrect file name!");
        test:assertFalse(metadata.dir, "Incorrect file info!");
        error? removeResult = remove(tmpdir + srcFileRaw);
        if (removeResult is error) {
            test:assertFail("Error removing test resource!");
        }
    } else {
        test:assertFail("Error retrieving file info!");
    }
}

@test:Config {}
function testMetadataNonExisting() {
    MetaData|error metadata = getMetaData(tmpdir + noFile);
    if (metadata is error) {
        string expectedErrMsg = "File not found";
        test:assertTrue(stringutils:contains(metadata.message(), expectedErrMsg));
    }
}

@test:Config {}
function testReadDir() {
    MetaData[]|error metadata = readDir(srcDir);
    if (metadata is error) {
        test:assertFail("Read directory failed!");
    }
}

@test:Config {}
function testReadNonExistingDir() {
    MetaData[]|error metadata = readDir(noDir);
    if (metadata is error) {
        string expectedErrMsg = "File not found";
        test:assertTrue(stringutils:contains(metadata.message(), expectedErrMsg));
    }
}

@test:Config {}
function testCreateDir() {
    error? crResult = createDir(emptyDir, NON_RECURSIVE);
    if (crResult is error) {
        test:assertFail("Error creating directory!");
    }
}

@test:Config {dependsOn: ["testCreateDir"]}
function testReadEmptyDir() {
    MetaData[]|error metadata = readDir(emptyDir);
    if (metadata is MetaData[]) {
        test:assertEquals(metadata.length(), 0, "Invalid file info!");
        error? removeResult = remove(emptyDir);
    }
}

@test:Config {}
function testReadFileWithReadDir() {
    MetaData[]|error metadata = readDir(srcFile);
    if (metadata is error) {
        string expectedErrMsg = "not a directory";
        test:assertTrue(stringutils:contains(metadata.message(), expectedErrMsg));
    }
}

@test:Config {}
function testFileExists() {
    boolean|error result = test(srcFile, EXISTS);
    if (result is boolean) {
        test:assertTrue(result, "File doesn't exist!");
    } else {
        test:assertFail("Error testing file!");
    }
}

@test:Config {}
function testFileExistsNonExistingFile() {
    boolean|error result = test("src/file/tests/resources/no-file.txt", EXISTS);
    if (result is boolean) {
            test:assertFalse(result, "File exists!");
    } else {
        test:assertFail("Error testing file!");
    }
}

@test:Config {}
function testCreateNonExistingPathFile() {
    error? crResult = create(noDir + noFile);
    if (crResult is error) {
        string expectedErrMsg = "The file does not exist in path";
        test:assertTrue(stringutils:contains(crResult.message(), expectedErrMsg));
    }
}

@test:Config {}
function testCreateExistingFile() {
    error? crResult = create(srcFile);
    if (crResult is error) {
        string expectedErrMsg = "File already exists. Failed to create the file";
        test:assertTrue(stringutils:contains(crResult.message(), expectedErrMsg));
    }
}

@test:Config {}
function testCreateDirWithParentDir() {
    error? result = createDir(tmpdir + "/parent" + "/child", RECURSIVE);
    if (result is error) {
        test:assertFail("Directory creation not successful!");
    } else {
        error? removeResult = remove(tmpdir + "/parent", RECURSIVE);
        if (removeResult is error) {
            test:assertFail("Error removing test resource!");
        }
    }
}

@test:Config {}
function testCreateDirWithoutParentDir() {
    error? result = createDir(tmpdir + "/parent" + "/child", NON_RECURSIVE);
    if (result is error) {
        string expectedErrMsg = "IO error while creating the file";
        test:assertTrue(stringutils:contains(result.message(), expectedErrMsg));
    }
}

@test:Config {}
function testCopyFile() {
    MetaData|error srcmetadata = getMetaData(srcFile);
    if (srcmetadata is MetaData) {
        srcFileLength = srcmetadata.size;
    } else {
        test:assertFail("Error retrieving source file size!");
    }

    error? copyResult = copy(srcFile, tmpdir + copyFile);
    if (copyResult is error) {
        test:assertFail("File not copied!");
    } else {
        MetaData|error metadata = getMetaData(tmpdir + copyFile);
        if (metadata is MetaData) {
            int destFileLength = metadata.size;
            test:assertEquals(destFileLength, srcFileLength, "File size mismatch!");
        } else {
            test:assertFail("Error retrieving destination file size!");
        }
    }
}

@test:Config {dependsOn: ["testCopyFile"]}
function testCopyFileReplaceFalse() {
    MetaData|error srcMmetadata = getMetaData(srcModifiedFile);
    if (srcMmetadata is MetaData) {
        srcModifiedFileLength = srcMmetadata.size;
    } else {
        test:assertFail("Error retrieving source file size!");
    }

    error? copyResult = copy(srcModifiedFile, tmpdir + copyFile);
    if (copyResult is error) {
        test:assertFail("File not copied!");
    } else {
        MetaData|error metadata = getMetaData(tmpdir + copyFile);
        if (metadata is MetaData) {
            int destFileLength = metadata.size;
            test:assertEquals(destFileLength, srcFileLength, "File size mismatch!");
            test:assertNotEquals(destFileLength, srcModifiedFileLength);
        } else {
            test:assertFail("Error retrieving destination file size!");
        }
    }
}

@test:Config {dependsOn: ["testCopyFileReplaceFalse"]}
function testCopyFileReplaceTrue() {
    error? copyResult = copy(srcModifiedFile, tmpdir + copyFile, REPLACE_EXISTING);
    if (copyResult is error) {
        test:assertFail("File not copied!");
    } else {
        MetaData|error metadata = getMetaData(tmpdir + copyFile);
        if (metadata is MetaData) {
            int destFileLength = metadata.size;
            test:assertEquals(destFileLength, srcModifiedFileLength, "File size mismatch!");
            test:assertNotEquals(destFileLength, srcFileLength);
            error? removeResult = remove(tmpdir + copyFile);
            if (removeResult is error) {
                test:assertFail("Error removing test resource!");
            }
        } else {
            test:assertFail("Error retrieving destination file size!");
        }
    }
}

@test:Config {}
function testCopyFileNonExistSource() {
    error? copyResult = copy("src/file/tests/resources/no-file.txt", tmpdir + noFile);
    if (copyResult is error) {
        string expectedErrMsg = "File not found";
        test:assertTrue(stringutils:contains(copyResult.message(), expectedErrMsg));
    }
}

@test:Config {}
function testGetCurrentDirectory() {
    string currentDir = getCurrentDir();
    string usrDir = getUserDir();
    test:assertEquals(currentDir, usrDir, "Incorrect current directory!");
}

function getTmpDir() returns string = @java:Method {
    'class: "org.ballerinalang.stdlib.file.testutils.TestUtil"
} external;

function getUserDir() returns string = @java:Method {
    'class: "org.ballerinalang.stdlib.file.testutils.TestUtil"
} external;
