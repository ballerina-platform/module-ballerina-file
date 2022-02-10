// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/http;
import ballerina/file;
import ballerina/io;

configurable string basePath = ?;

service /file_manager on new http:Listener(9090) {
    resource function post file/[string fileName]() returns string|error {
        string filePath = check file:joinPath(basePath, fileName);
        check file:create(filePath);
        return "File is created successfully";
    }

    resource function delete file/[string fileName]() returns string|error {
        string filePath = check file:joinPath(basePath, fileName);
        check file:remove(filePath);
        return "File is deleted successfully";
    }

    resource function put file/[string fileName](@http:Payload string content) returns string|error {
        string filePath = check file:joinPath(basePath, fileName);
        check io:fileWriteString(filePath, content);
        return "File is modified successfully";
    }

    resource function post directory/[string directoryName]() returns string|error {
        string dirPath = check file:joinPath(basePath, directoryName);
        check file:createDir(dirPath, file:RECURSIVE);
        return "Directory is created successfully";
    }

    resource function get directory/metadata/[string directoryName]() returns file:MetaData[]|error {
        string filePath = check file:joinPath(basePath, directoryName);
        file:MetaData[] readDirResults = check file:readDir(filePath);
        return readDirResults;
    }

    resource function delete directory/[string directoryName]() returns string|error {
        string directoryPath = check file:joinPath(basePath, directoryName);
        check file:remove(directoryPath, file:RECURSIVE);
        return "Directory is deleted successfully";
    }

    resource function post directory/[string sourceDirectoryName]/[string destinationDirectoryName]()
                       returns string|error {
        string sourceDirectoryPath = check file:joinPath(basePath, sourceDirectoryName);
        string destinationDirectoryPath = check file:joinPath(basePath, destinationDirectoryName);
        check file:copy(sourceDirectoryPath, destinationDirectoryPath, file:REPLACE_EXISTING);
        return "Dirctory is copied successfully";
    }

    resource function put directory/[string oldDirectoryName]/[string newDirectoryName]() returns string|error {
        string oldDirectoryPath = check file:joinPath(basePath, oldDirectoryName);
        string newDirectoryPath = check file:joinPath(basePath, newDirectoryName);
        check file:rename(oldDirectoryPath, newDirectoryPath);
        return "The " + oldDirectoryPath + " directory is renamed successfully.";
    }
}
