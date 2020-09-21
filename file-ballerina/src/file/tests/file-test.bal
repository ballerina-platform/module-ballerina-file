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
import ballerina/io;

string tmpdir = tempDir();
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
function checkTmpDir() {
    string tmpdirJava = getTmpDir();
    test:assertEquals(tmpdir, tmpdirJava, "Temp directory mismatch!");
}

@test:Config {}
function testRename() {
    error? copyResult = copy(srcFile, tmpdir + srcFileRaw, true);
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
    error? copyResult = copy(srcFile, tmpdir + srcFileRaw, true);
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
    error? removeResult = remove(tmpdir + "/src-dir", false);
    if (removeResult is error) {
        string expectedErrMsg = "Error while deleting";
        test:assertTrue(stringutils:contains(removeResult.message(), expectedErrMsg));
    }
}

@test:Config {dependsOn: ["testRemoverecursivefalse"]}
function testRemoverecursiveTrue() {
    error? removeResult = remove(tmpdir + "/src-dir", true);
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
function testFileInfo() {
    FileInfo|error fileInfo = getFileInfo(tmpdir + srcFileRaw);
    if (fileInfo is FileInfo) {
        test:assertEquals(fileInfo.getName(), "src-file.txt", "Incorrect file name!");
        test:assertFalse(fileInfo.isDir(), "Incorrect file info!");
        error? removeResult = remove(tmpdir + srcFileRaw);
        if (removeResult is error) {
            test:assertFail("Error removing test resource!");
        }
    } else {
        test:assertFail("Error retrieving file info!");
    }
}

@test:Config {}
function testFileInfoNonExisting() {
    FileInfo|error fileInfo = getFileInfo(tmpdir + noFile);
    if (fileInfo is error) {
        string expectedErrMsg = "File not found";
        test:assertTrue(stringutils:contains(fileInfo.message(), expectedErrMsg));
    }
}

@test:Config {}
function testReadDir() {
    FileInfo[]|error fileInfo = readDir(srcDir);
    if (fileInfo is error) {
        test:assertFail("Read directory failed!");
    }
}

@test:Config {}
function testReadDirWithMaxLength() {
    FileInfo[]|error fileInfo = readDir(srcDir, 0);
    if (fileInfo is FileInfo[]) {
        test:assertEquals(fileInfo.length(), 0, "Invalid file info!");
    }
}

@test:Config {}
function testReadDirWithMaxLength1() {
    FileInfo[]|error fileInfo = readDir(rdDir, 1);
    if (fileInfo is FileInfo[]) {
        test:assertEquals(fileInfo.length(), 2, "Invalid file info!");
    }
}

@test:Config {}
function testReadNonExistingDir() {
    FileInfo[]|error fileInfo = readDir(noDir);
    if (fileInfo is error) {
        string expectedErrMsg = "File not found";
        test:assertTrue(stringutils:contains(fileInfo.message(), expectedErrMsg));
    }
}

@test:Config {}
function testCreateDir() {
    string|error crResult = createDir(emptyDir);
    if (crResult is error) {
        test:assertFail("Error creating directory!");
    }
}

@test:Config {dependsOn: ["testCreateDir"]}
function testReadEmptyDir() {
    FileInfo[]|error fileInfo = readDir(emptyDir);
    if (fileInfo is FileInfo[]) {
        test:assertEquals(fileInfo.length(), 0, "Invalid file info!");
        error? removeResult = remove(emptyDir);
    }
}

@test:Config {}
function testReadFileWithReadDir() {
    FileInfo[]|error fileInfo = readDir(srcFile);
    if (fileInfo is error) {
        string expectedErrMsg = "not a directory";
        test:assertTrue(stringutils:contains(fileInfo.message(), expectedErrMsg));
    }
}

@test:Config {}
function testFileExists() {
    boolean result = exists(srcFile);
    test:assertTrue(result, "File doesn't exist!");
}

@test:Config {}
function testFileExistsNonExistingFile() {
    boolean result = exists("src/file/tests/resources/no-file.txt");
    test:assertFalse(result, "File exists!");
}

@test:Config {}
function testCreateNonExistingPathFile() {
    string|error crResult = createFile(noDir + noFile);
    if (crResult is error) {
        string expectedErrMsg = "The file does not exist in path";
        test:assertTrue(stringutils:contains(crResult.message(), expectedErrMsg));
    }
}

@test:Config {}
function testCreateExistingFile() {
    string|error crResult = createFile(srcFile);
    if (crResult is error) {
        string expectedErrMsg = "File already exists. Failed to create the file";
        test:assertTrue(stringutils:contains(crResult.message(), expectedErrMsg));
    }
}

@test:Config {}
function testCreateDirWithParentDir() {
    string|error result = createDir(tmpdir + "/parent" + "/child", true);
    if (result is string) {
        io:println("RESPONSE :" + result);
        test:assertTrue(stringutils:contains(result, tmpdir + "/parent" + "/child"), "Directory creation not successful!");
        error? removeResult = remove(tmpdir + "/parent", true);
        if (removeResult is error) {
            test:assertFail("Error removing test resource!");
        }
    }
}

@test:Config {}
function testCreateDirWithoutParentDir() {
    string|error result = createDir(tmpdir + "/parent" + "/child", false);
    if (result is error) {
        string expectedErrMsg = "IO error while creating the file";
        io:println("ERROR :" + result.message());
        test:assertTrue(stringutils:contains(result.message(), expectedErrMsg));
    }
}

@test:Config {}
function testCopyFile() {
    FileInfo|error srcfileinfo = getFileInfo(srcFile);
    if (srcfileinfo is FileInfo) {
        srcFileLength = srcfileinfo.getSize();
    } else {
        test:assertFail("Error retrieving source file size!");
    }

    error? copyResult = copy(srcFile, tmpdir + copyFile, false);
    if (copyResult is error) {
        test:assertFail("File not copied!");
    } else {
        FileInfo|error fileinfo = getFileInfo(tmpdir + copyFile);
        if (fileinfo is FileInfo) {
            int destFileLength = fileinfo.getSize();
            test:assertEquals(destFileLength, srcFileLength, "File size mismatch!");
        } else {
            test:assertFail("Error retrieving destination file size!");
        }
    }
}

@test:Config {dependsOn: ["testCopyFile"]}
function testCopyFileReplaceFalse() {
    FileInfo|error srcMfileinfo = getFileInfo(srcModifiedFile);
    if (srcMfileinfo is FileInfo) {
        srcModifiedFileLength = srcMfileinfo.getSize();
    } else {
        test:assertFail("Error retrieving source file size!");
    }

    error? copyResult = copy(srcModifiedFile, tmpdir + copyFile, false);
    if (copyResult is error) {
        test:assertFail("File not copied!");
    } else {
        FileInfo|error fileinfo = getFileInfo(tmpdir + copyFile);
        if (fileinfo is FileInfo) {
            int destFileLength = fileinfo.getSize();
            test:assertEquals(destFileLength, srcFileLength, "File size mismatch!");
            test:assertNotEquals(destFileLength, srcModifiedFileLength);
        } else {
            test:assertFail("Error retrieving destination file size!");
        }
    }
}

@test:Config {dependsOn: ["testCopyFileReplaceFalse"]}
function testCopyFileReplaceTrue() {
    error? copyResult = copy(srcModifiedFile, tmpdir + copyFile, true);
    if (copyResult is error) {
        test:assertFail("File not copied!");
    } else {
        FileInfo|error fileinfo = getFileInfo(tmpdir + copyFile);
        if (fileinfo is FileInfo) {
            int destFileLength = fileinfo.getSize();
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
    string currentDir = getCurrentDirectory();
    string usrDir = getUserDir();
    test:assertEquals(currentDir, usrDir, "Incorrect current directory!");
}

function getTmpDir() returns string = @java:Method {
    'class: "org.ballerinalang.stdlib.file.testutils.TestUtil"
} external;

function getUserDir() returns string = @java:Method {
    'class: "org.ballerinalang.stdlib.file.testutils.TestUtil"
} external;
