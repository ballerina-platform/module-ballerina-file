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

# Represents an error that occurs when a file system operation is denied due to invalidity.
public type InvalidOperationError distinct Error;

# Represents an error that occurs when a file system operation is denied, due to the absence of file permission.
public type PermissionError distinct Error;

# Represents an error that occurs when a file system operation fails.
public type FileSystemError distinct Error;

# Represents an error, which occurs when the file/directory does not exist in the given file path.
public type FileNotFoundError distinct Error;

# Represents an error, which occurs when the file in the given file path is not a symbolic link.
public type NotLinkError distinct Error;

# Represents an IO error, which occurs when trying to access the file in the given file path.
public type IOError distinct Error;

# Represents a security error, which occurs when trying to access the file in the given file path.
public type SecurityError distinct Error;

# Represents an error, which occurs when the given file path is invalid.
public type InvalidPathError distinct Error;

# Represents an error, which occurs when the given pattern is not a valid file path pattern.
public type InvalidPatternError distinct Error;

# Represents an error, which occurs when the given target file path cannot be derived relative to the base file path.
public type RelativePathError distinct Error;

# Represents an error, which occurs in the UNC path.
public type UNCPathError distinct Error;

# Represents a generic error for the file path.
public type GenericError distinct Error;

# Represents file system related errors.
public type Error distinct error;
