// Copyright (c) 2017 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/jballerina.java;

# Returns the current working directory.
# ```ballerina
# string dirPath = file:getCurrentDir();
# ```
# 
# + return - Current working directory or else an empty string if the current working directory cannot be determined
public isolated function getCurrentDir() returns string = @java:Method {
    'class: "io.ballerina.stdlib.file.nativeimpl.Utils",
    name: "getCurrentDirectory"
} external;

# Creates a new directory with the specified name.
# ```ballerina
# check file:createDir("foo/bar");
# ```
#
# + dir - Directory name
# + option - Indicates whether the `createDir` should create non-existing parent directories. The default is only to
#            create the given current directory.
# + return - A `file:Error` if the directory creation failed
public isolated function createDir(string dir, DirOption option = NON_RECURSIVE)
returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.file.nativeimpl.Utils",
    name: "createDir"
} external;

# Removes the specified file or directory.
# ```ballerina
# check file:remove("foo/bar.txt");
# ```
#
# + path - String value of the file/directory path
# + option - Indicates whether the `remove` should recursively remove all the files inside the given directory
# + return - An `file:Error` if failed to remove
public isolated function remove(string path, DirOption option = NON_RECURSIVE)
returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.file.nativeimpl.Utils",
    name: "remove"
} external;

# Renames(Moves) the old path with the new path.
# If the new path already exists and it is not a directory, this replaces the file.
# ```ballerina
# check file:rename("/A/B/C", "/A/B/D");
# ```
#
# + oldPath - String value of the old file path
# + newPath - String value of the new file path
# + return - An `file:Error` if failed to rename
public isolated function rename(string oldPath, string newPath) returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.file.nativeimpl.Utils",
    name: "rename"
} external;

# Creates a file in the specified file path.
# Truncates if the file already exists in the given path.
# ```ballerina
# check file:create("bar.txt");
# ```
#
# + path - String value of the file path
# + return - A `file:Error` if file creation failed
public isolated function create(string path) returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.file.nativeimpl.Utils",
    name: "createFile"
} external;

isolated function getRawMetaData(string path) returns MetaData|Error = @java:Method {
    'class: "io.ballerina.stdlib.file.nativeimpl.Utils",
    name: "getMetaData"
} external;

# Returns the metadata information of the file specified in the file path.
# ```ballerina
# file:MetaData result = check file:getMetaData("foo/bar.txt");
# ```
#
# + path - String value of the file path.
# + return - The `MetaData` instance with the file metadata or else a `file:Error`
public isolated function getMetaData(string path) returns (MetaData & readonly)|Error {
    var result = getRawMetaData(path);
    if (result is MetaData) {
        return <readonly & MetaData>result.cloneReadOnly();
    } else {
        return result;
    }
}

isolated function readDirRaw(string path) returns MetaData[]|Error = @java:Method {
    'class: "io.ballerina.stdlib.file.nativeimpl.Utils",
    name: "readDir"
} external;

# Reads the directory and returns a list of metadata of files and directories
# inside the specified directory.
# ```ballerina
# file:MetaData[] results = check file:readDir("foo/bar");
# ```
#
# + path - String value of the directory path
# + return - The `MetaData` array or else a `file:Error` if there is an error
public isolated function readDir(string path) returns (MetaData[] & readonly)|Error {
    var result = readDirRaw(path);
    if result is MetaData[] {
        return <readonly & MetaData[]>result.cloneReadOnly();
    } else {
        return result;
    }
}

# Copy the file/directory in the old path to the new path.
# ```ballerina
# check file:copy("/A/B/C", "/A/B/D", true);
# ```
#
# + sourcePath - String value of the old file path
# + destinationPath - String value of the new file path
# + options - Parameter to denote how the copy operation should be done. Supported options are,
#  `REPLACE_EXISTING` - Replace the target path if it already exists,
#  `COPY_ATTRIBUTES` - Copy the file attributes as well to the target,
#  `NO_FOLLOW_LINKS` - If source is a symlink, only the link is copied, not the target of the link.
# + return - An `file:Error` if failed to copy
public isolated function copy(string sourcePath, string destinationPath,
                     CopyOption... options) returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.file.nativeimpl.Utils",
    name: "copy"
} external;

# Creates a temporary file.
# ```ballerina
# string tmpFile = check file:createTemp();
# ```
#
# + suffix - Optional file suffix
# + prefix - Optional file prefix
# + dir - The directory path where the temp file should be created. If not specified,
#         temp file will be created in the default temp directory of the OS.
# + return - Temporary file path or else a `file:Error` if there is an error
public isolated function createTemp(string? suffix = (), string? prefix = (), string? dir  = ())
                                 returns string|Error = @java:Method {
    'class: "io.ballerina.stdlib.file.nativeimpl.Utils",
    name: "createTemp"
} external;

# Creates a temporary directory.
# ```ballerina
# string tmpDir = check file:createTempDir();
# ```
#
# + suffix - Optional directory suffix
# + prefix - Optional directory prefix
# + dir - The directory path where the temp directory should be created. If not specified, temp directory
#         will be created in the default temp directory of the OS.
# + return - Temporary directory path or else a `file:Error` if there is an error
public isolated function createTempDir(string? suffix = (), string? prefix = (), string? dir  = ())
                                 returns string|Error = @java:Method {
    'class: "io.ballerina.stdlib.file.nativeimpl.Utils",
    name: "createTempDir"
} external;

# Tests a file path against a test condition .
# ```ballerina
# boolean result = check file:test("foo/bar.txt", file:EXISTS);
# ```
#
# + path - String value of the file path
# + testOption - The option to be tested upon the path. Supported options are,
#  `EXISTS` - Test whether a file path exists,
#  `IS_DIR` - Test whether a file path is a directory,
#  `IS_SYMLINK` - Test whether a file path is a symlink,
#  `READABLE` - Test whether a file path is readable,
#  `WRITABLE` - Test whether a file path is writable.
# + return - True/false depending on the option to be tested or else a `file:Error` if there is an error
public isolated function test(string path, TestOption testOption) returns boolean|Error = @java:Method {
    'class: "io.ballerina.stdlib.file.nativeimpl.Utils",
    name: "test"
} external;
