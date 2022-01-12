// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

# Represents an event, which will trigger when there is a change to the listening directory.
#
# + name - Absolute file URI for triggered event
# + operation - Triggered event action. This can be create, delete or modify
public type FileEvent record {|
    string name;
    string operation;
|};

# Represents the options that can be passed to the `normalizePath` function.
#
# + CLEAN - Get the shortest path name equivalent to the given path by eliminating multiple separators, '.', and '..'
# + SYMLINK - Evaluate a symlink
# + NORMCASE - Normalize the case of a pathname. On windows, all the characters are converted to lowercase and "/" is
# converted to "\".
public enum NormOption {
    CLEAN,
    SYMLINK,
    NORMCASE
}

# Represents options that can be used when creating or removing directories.
#
# + RECURSIVE - Create non-existing parent directories or remove all the files inside the given directory
# + NON_RECURSIVE - Create/remove only the given directory
public enum DirOption {
    RECURSIVE,
    NON_RECURSIVE
}

# Represents the options that can be passed to the test function.
#
# + EXISTS - Test whether a file path exists
# + IS_DIR - Test whether a file path is a directory
# + IS_SYMLINK - Test whether a file path is a symlink
# + READABLE - Test whether a file path is readable
# + WRITABLE - Test whether a file path is writable
public enum TestOption {
    EXISTS,
    IS_DIR,
    IS_SYMLINK,
    READABLE,
    WRITABLE
}

# Represents options that can be used when copying files/directories
#
# + REPLACE_EXISTING - Replace the target path if it already exists
# + COPY_ATTRIBUTES - Copy the file attributes as well to the target
# + NO_FOLLOW_LINKS - If source is a symlink, only the link is copied, not the target of the link
public enum CopyOption {
    REPLACE_EXISTING,
    COPY_ATTRIBUTES,
    NO_FOLLOW_LINKS
}

# Represents a File service.
public type Service distinct service object {};
