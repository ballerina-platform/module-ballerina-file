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
import ballerina/jballerina.java;
import ballerina/io;

string tmpdir = getTmpDir();
string srcDir = "tests/resources/src-dir";
string rdDir = "tests/resources/read-dir";
string emptyDir = "tests/resources/empty-dir";
string noDir = "tests/resources/no-dir";

string srcFile = "tests/resources/src-file.txt";
string srcModifiedFile = "tests/resources/src-file-modified.txt";
string noFile = "/no-file.txt";
string srcFileRaw = "/src-file.txt";
string destFile = "/dest-file.txt";
string copyFile = "/cpy-file.txt";
string fileName = "file.txt";

int srcFileLength = 0;
int srcModifiedFileLength = 0;

@test:Config {}
function testRename() {
    error? copyResult = copy(srcFile, tmpdir + srcFileRaw, REPLACE_EXISTING);
    if copyResult is error {
        test:assertFail("File not copied!");
    }

    error? renameResult = rename(tmpdir + srcFileRaw, tmpdir + destFile);
    if renameResult is error {
        test:assertFail("File not renamed!");
    }
}

@test:Config {dependsOn: [testRename]}
function testRenameExisting() {
    error? copyResult = copy(srcFile, tmpdir + srcFileRaw, REPLACE_EXISTING);
    if copyResult is error {
        test:assertFail("File not copied!");
    }

    error? renameResult = rename(tmpdir + srcFileRaw, tmpdir + destFile);
    if renameResult is error {
        string expectedErrMsg = "File already exists in the new path ";
        test:assertTrue(renameResult.message().includes(expectedErrMsg));
    }
}

@test:Config {dependsOn: [testRenameExisting]}
function testRemove() {
    error? removeResult = remove(tmpdir + destFile);
    if removeResult is error {
        test:assertFail("File not removed!");
    }
}

@test:Config {}
function testCopyDir() {
    error? copyResult = copy(srcDir, tmpdir + "/src-dir");
    if copyResult is error {
        test:assertFail("Directory not copied!");
    }
}

@test:Config {}
function testCopyDir2() returns error? {
    string targetPath = "tests/resources/temp-dir/nested-file.txt";
    error? copyResult = copy(srcDir, "tests/resources/temp-dir", REPLACE_EXISTING);
    if copyResult is error {
        test:assertFail("Directory not copied!");
    } else {
        string readContent = check io:fileReadString(targetPath);
        test:assertEquals(readContent, "Hi");
        check io:fileWriteString(targetPath, "");
    }
}

@test:Config {dependsOn: [testCopyDir]}
function testRemoverecursivefalse() {
    error? removeResult = remove(tmpdir + "/src-dir");
    if removeResult is error {
        string expectedErrMsg = "Error while deleting";
        test:assertTrue(removeResult.message().includes(expectedErrMsg));
    }
}

@test:Config {dependsOn: [testRemoverecursivefalse]}
function testRemoverecursiveTrue() {
    error? removeResult = remove(tmpdir + "/src-dir", RECURSIVE);
    if removeResult is error {
        test:assertFail("File not removed!");
    }
}

@test:Config {}
function testRemoveNonExistingFile() {
    error? removeResult = remove(tmpdir + noFile);
    if removeResult is error {
        string expectedErrMsg = "File not found";
        test:assertTrue(removeResult.message().includes(expectedErrMsg));
    }
}

@test:Config {dependsOn: [testRenameExisting]}
function testMetadata() {
    MetaData|error metadata = getMetaData(tmpdir + srcFileRaw);
    if metadata is MetaData {
        //test:assertEquals(metadata.getName(), "src-file.txt", "Incorrect file name!");
        test:assertFalse(metadata.dir, "Incorrect file info!");
        error? removeResult = remove(tmpdir + srcFileRaw);
        if removeResult is error {
            test:assertFail("Error removing test resource!");
        }
    } else {
        test:assertFail("Error retrieving file info!");
    }
}

@test:Config {}
function testMetadataNonExisting() {
    MetaData|error metadata = getMetaData(tmpdir + noFile);
    if metadata is error {
        string expectedErrMsg = "File not found";
        test:assertTrue(metadata.message().includes(expectedErrMsg));
    }
}

@test:Config {}
function testReadDir() {
    MetaData[]|error metadata = readDir(srcDir);
    if metadata is error {
        test:assertFail("Read directory failed!");
    }
}

@test:Config {}
function testReadNonExistingDir() {
    MetaData[]|error metadata = readDir(noDir);
    if metadata is error {
        string expectedErrMsg = "File not found";
        test:assertTrue(metadata.message().includes(expectedErrMsg));
    }
}

@test:Config {}
function testCreateDir() {
    error? crResult = createDir(emptyDir, NON_RECURSIVE);
    if crResult is error {
        test:assertFail("Error creating directory!");
    }
}

@test:Config {dependsOn: [testCreateDir]}
function testReadEmptyDir() returns error? {
    MetaData[]|error metadata = readDir(emptyDir);
    if metadata is MetaData[] {
        test:assertEquals(metadata.length(), 0, "Invalid file info!");
        check remove(emptyDir);
    }
}

@test:Config {}
function testReadFileWithReadDir() {
    MetaData[]|error metadata = readDir(srcFile);
    if metadata is error {
        string expectedErrMsg = "not a directory";
        test:assertTrue(metadata.message().includes(expectedErrMsg));
    }
}

@test:Config {}
function testFileExists() {
    boolean|error result = test(srcFile, EXISTS);
    if result is boolean {
        test:assertTrue(result, "File doesn't exist!");
    } else {
        test:assertFail("Error testing file!");
    }
}

@test:Config {}
isolated function testFileExistsNonExistingFile() {
    boolean|error result = test("tests/resources/no-file.txt", EXISTS);
    if result is boolean {
            test:assertFalse(result, "File exists!");
    } else {
        test:assertFail("Error testing file!");
    }
}

@test:Config {}
function testCreateNonExistingPathFile() {
    error? crResult = create(noDir + noFile);
    if crResult is error {
        string expectedErrMsg = "The file does not exist in path";
        test:assertTrue(crResult.message().includes(expectedErrMsg));
    }
}

@test:Config {}
function testCreateExistingFile() {
    error? crResult = create(srcFile);
    if crResult is error {
        string expectedErrMsg = "File already exists. Failed to create the file";
        test:assertTrue(crResult.message().includes(expectedErrMsg));
    }
}

@test:Config {}
function testCreateDirWithParentDir() {
    error? result = createDir(tmpdir + "/parent" + "/child", RECURSIVE);
    if result is error {
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
    if result is error {
        string expectedErrMsg = "IO error while creating the file";
        test:assertTrue(result.message().includes(expectedErrMsg));
    }
}

@test:Config {}
function testCopyFile() {
    MetaData|error srcmetadata = getMetaData(srcFile);
    if srcmetadata is MetaData {
        srcFileLength = srcmetadata.size;
    } else {
        test:assertFail("Error retrieving source file size!");
    }

    error? copyResult = copy(srcFile, tmpdir + copyFile);
    if copyResult is error {
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

@test:Config {dependsOn: [testCopyFile]}
function testCopyFileReplaceFalse() {
    MetaData|error srcMmetadata = getMetaData(srcModifiedFile);
    if srcMmetadata is MetaData {
        srcModifiedFileLength = srcMmetadata.size;
    } else {
        test:assertFail("Error retrieving source file size!");
    }

    error? copyResult = copy(srcModifiedFile, tmpdir + copyFile);
    if copyResult is error {
        test:assertFail("File not copied!");
    } else {
        MetaData|error metadata = getMetaData(tmpdir + copyFile);
        if metadata is MetaData {
            int destFileLength = metadata.size;
            test:assertEquals(destFileLength, srcFileLength, "File size mismatch!");
            test:assertNotEquals(destFileLength, srcModifiedFileLength);
        } else {
            test:assertFail("Error retrieving destination file size!");
        }
    }
}

@test:Config {dependsOn: [testCopyFileReplaceFalse]}
function testCopyFileReplaceTrue() {
    error? copyResult = copy(srcModifiedFile, tmpdir + copyFile, REPLACE_EXISTING);
    if copyResult is error {
        test:assertFail("File not copied!");
    } else {
        MetaData|error metadata = getMetaData(tmpdir + copyFile);
        if metadata is MetaData {
            int destFileLength = metadata.size;
            test:assertEquals(destFileLength, srcModifiedFileLength, "File size mismatch!");
            test:assertNotEquals(destFileLength, srcFileLength);
            error? removeResult = remove(tmpdir + copyFile);
            if removeResult is error {
                test:assertFail("Error removing test resource!");
            }
        } else {
            test:assertFail("Error retrieving destination file size!");
        }
    }
}

@test:Config {}
function testCopyFileNonExistSource() {
    error? copyResult = copy("tests/resources/no-file.txt", tmpdir + noFile);
    if copyResult is error {
        string expectedErrMsg = "File not found";
        test:assertTrue(copyResult.message().includes(expectedErrMsg));
    }
}

@test:Config {}
function testGetCurrentDirectory() {
    string currentDir = getCurrentDir();
    string usrDir = getUserDir();
    test:assertEquals(currentDir, usrDir, "Incorrect current directory!");
}

@test:Config {}
isolated function testCreateTempFile() {
    string|error result = createTemp();
    if result is string {
        boolean|error ss = test(result, EXISTS);
        if ss is boolean {
            test:assertTrue(ss);
        } else {
            test:assertFail("Error testing file existance!");
        }
    } else {
        test:assertFail("Error creating temporary file!");
    }
}

@test:Config {}
isolated function testCreateTempFileWithArguments() returns error? {
    string|error result = createTemp("-surfix", "prefix-", "tests/resources/src-dir");
    if result is string {
        boolean|error isExist = test(result, EXISTS);
        boolean|error isReadable = test(result, READABLE);
        boolean|error isWriteable = test(result, WRITABLE);
        if isExist is boolean && isReadable is boolean && isWriteable is boolean {
            test:assertTrue(isExist);
            test:assertTrue(isReadable);
            test:assertTrue(isWriteable);
            check remove(result);
        } else {
            test:assertFail("Error testing file existance!");
        }
    } else {
        test:assertFail("Error creating temporary file!");
    }
}

@test:Config {}
isolated function testCreateTempDirWithArguments() returns error? {
    string|error result = createTempDir("-surfix", "prefix-", "tests/resources/src-dir");
    if result is string {
        boolean|error isExist = test(result, EXISTS);
        boolean|error isDir = test(result, IS_DIR);
        if isExist is boolean && isDir is boolean {
            test:assertTrue(isExist);
            test:assertTrue(isDir);
            check remove(result);
        } else {
            test:assertFail("Error testing file existance!");
        }
    } else {
        test:assertFail("Error creating temporary file!");
    }
}

@test:Config {}
isolated function testCreateTempDir() returns error? {
    string|error result = createTempDir();
    if result is string {
        boolean|error isExist = test(result, EXISTS);
        boolean|error isSymLink = test(result, IS_SYMLINK);
        if isExist is boolean && isSymLink is boolean {
            test:assertTrue(isExist);
            test:assertFalse(isSymLink);
            check remove(result);
        } else {
            test:assertFail("Error testing file existance!");
        }
    } else {
        test:assertFail("Error creating temporary file!");
    }
}

@test:Config {
    groups: ["create", "file"]
}
function testCreateFile() returns error? {
    error? crResult = create(fileName);
    if crResult is error {
        test:assertFail("File not created!");
    }
}

@test:Config {
    groups: ["rename", "negative"],
    dependsOn: [testCreateFile]
}
function negativeTestRenameFile() returns error? {
    error? renameResult = rename(fileName, srcDir +"/dir/file.txt");
    if renameResult is error {
        //io:println(renameResult.toString());
        test:assertTrue(renameResult.toString().includes("FileSystemError"));
    } else {
        test:assertFail("Test failed!");
    }
    check remove(fileName);
}
@test:Config {}
function testCopyAttribute() returns error? {
    error? copyResult = copy(srcFile, srcDir + srcFileRaw, COPY_ATTRIBUTES);
    if copyResult is error {
        test:assertFail("File not copied!");
    }
    check remove(srcDir + srcFileRaw);
}

@test:Config {}
function testCopyNoFollowsLink() returns error? {
    string newFileName = srcDir + "/" + fileName;
    error? copyResult = copy(srcFile, newFileName, NO_FOLLOW_LINKS);
    if copyResult is error {
        test:assertFail("File not copied!");
    }
    check remove(newFileName);
}

@test:Config {
    groups: ["dir", "negative"]
}
function negativeTestCreateDir() {
    error? result = createDir(srcDir);
    if result is error {
        test:assertTrue(result.message().includes("File already exists."));
    } else {
         test:assertFail("Test failed!");
    }
}

@test:Config {
    groups: ["rename", "negative"]
}
function negativeTestRename() {
    error? renameResult = rename(srcDir + "file.txt", srcDir + "file.txt");
    if renameResult is error {
        test:assertTrue(renameResult.message().includes("File not found"));
    } else {
        test:assertFail("Test failed!");
    }
}

@test:Config {}
function negTestCreateDir() returns error? {
    error? result = createDir("/tests/resources/dir/newDir");
    if result is error {
        test:assertTrue(result.message().includes("IO error while creating the file"));
    } else {
        test:assertFail("Test failed.");
    }
}

function getTmpDir() returns string = @java:Method {
    'class: "io.ballerina.stdlib.file.testutils.TestUtil"
} external;

function getUserDir() returns string = @java:Method {
    'class: "io.ballerina.stdlib.file.testutils.TestUtil"
} external;
