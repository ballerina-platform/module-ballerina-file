# Specification: Ballerina File Library

_Owners_: @daneshk @kalaiyarasiganeshalingam  
_Reviewers_: @daneshk  
_Created_: 2021/12/09   
_Updated_: 2021/12/09  
_Issue_: [#2324](https://github.com/ballerina-platform/ballerina-standard-library/issues/2324)

# Introduction
This is the specification for the File standard library, which is used to  perform file, file path, and directory
operation in the [Ballerina programming language](https://ballerina.io/), which is an open-source programming language
for the cloud that makes it easier to use, combine, and create network services.

# Contents
1. [Overview](#1-overview)
3. [File Metadata](#2-file-metadata)
3. [File & Directory Operations](#3-file-and-directory-operations)
4. [Path Operations](#4-path-operations)
5. [Directory Listener](#5-directory-listener)

# 1. Overview
Ballerina file standard library provides functionalities related to manipulating and working with files and directories.
All operations are supported and both Windows and Unix-based operating systems. 

# 2. File Metadata
Metadata information of files and directories will contain the following
* Absolute path
* Size in bytes
* Last modified time
* Whether it is a file or directory
* Read permission
* Write permission
```ballerina
public type MetaData record{|
    string absPath;
    int size;
    time:Utc modifiedTime;
    boolean dir;
    boolean readable;
    boolean writable;
|}; 
```

# 3. File and Directory Operations
The following operations are used to manipulate files and directories.

# 3.1 Get Current Directory
This is used to obtain the absolute path of the current working directory.
```ballerina
public isolated function getCurrentDir() returns string;
```

#3.2 Create Directory
This is used to create a new directory. An option can be passed to configure whether non-existent parent directories
will be created or not during this process.
```ballerins
public isolated function createDir(string dir, DirOption option);
```

#3.3 Create File
This is used to create a new file in the provided path.
```ballerina
public isolated function create(string path) returns Error?;
```

#3.4 Rename
This is used to rename (move) a file or directory. If the newPath provided already exists and is not a directory, it
will be replaced. 
```ballerina
public isolated function rename(string oldPath, string newPath) returns Error?;
```

#3.5 Copy
This is used to copy the file or directory in the provided path to a new location as specified in the new path. Options
can be passed to define how this operation is executed. 

Possible options
* Whether existing files/directories should be replaced
* Whether file attributes should be copied
* If the source is a symbolic link, whether the link should be copied, or the target file.
```ballerina
public isolated function copy(string sourcePath, string destinationPath, CopyOption... options) returns Error?;
```

#3.6 Remove
This is used to remove a file or directory. If the provided path is a directory, an option can be passed to configure
whether all files and directories inside the given directory should be recursively removed.
```ballerina
public isolated function remove(string path, DirOption option) returns Error?'
```

#3.7 Get Metadata
This is used to obtain the metadata information of the file specified in the provided path.
```ballerina
public isolated function getMetaData(string path) returns MetaData|Error;
```

#3.8 Read Directory
This is used to obtain a list of files and directories in the provided path with the relevant metadata information.
```ballerina
public isolated function readDir(string path) returns MetaData[]|Error;
```

#3.9 Create Temporary File
This is used to create a temporary file. An optional prefix and suffix may be defined. If the directory in which the
temporary file is to be created is not defined, the default temp directory of the OS will be used.
```ballerina
public isolated function createTemp(string? suffix = (), string? prefix = (), string? dir  = ()) returns string|Error;
```

#3.10 Create Temporary File
This is used to create a temporary directory. An optional prefix and suffix may be defined. If the directory in which
the temporary directory is to be created is not defined, the default temp directory of the OS will be used.
```ballerina
public isolated function createTempDir(string? suffix = (), string? prefix = (), string? dir  = ()) returns string|Error;
```

#3.11 Test
This is used test whether a file or directory meets a particular condition. Possible test conditions are,
* Whether the file or directory exists
* Whether the provided path is a directory
* Whether the provided path is a symbolic link
* Read permission
* Write permission
```ballerina
public isolated function test(string path, TestOption testOption) returns boolean|Error;
```

#4 Path Operations
The following are used to create and manipulate paths. Compatibility with both Windows and Unix-based operating 
systems are ensured.

#4.1 Get Absolute Path
This is used to retrieve the absolute path reference from the provided relative path.
```ballerina
public isolated function getAbsolutePath(string path) returns string|Error;
```

#4.2 Is Absolute
This is used to determine whether the provided path is absolute or not.
```ballerina
public isolated function isAbsolutePath(string path) returns boolean|Error;
```

#4.3 Get Basename
This is used to retrieve the base name of the file or directory at the provided path. 
```ballerina
public isolated function basename(string path) returns string|Error;
```

#4.4 Get Parent Path
This is used to retrieve the parent directory of the provided file or directory.
```ballerina
public isolated function parentPath(string path) returns string|Error;
```

#4.5 Normalize Path
This is used to normalize the provided path value. Options can be provided to indicate how the normalization should
be performed.
* Get shortest name equivalent
* Evaluate symbolic links
* Normalize case
```ballerina
public isolated function normalizePath(string path, NormOption option) returns string|Error;
```

#4.6 Split Path
This is used to split the provided path into an array of path components.
```ballerina
public isolated function splitPath(string path) returns string[]|Error;
```

#4.7 Join Path
This is used to combine multiple path components to create a single path.
```ballerina
public isolated function joinPath(string... parts) returns string|Error;
```

#4.8 Get Relative Path
This is used to generate a logically equivalent relative path to the provided target path from the provided base path. 
```ballerina
public isolated function relativePath(string base, string target) returns string|Error;
```

#5 Directory Listener
The directory listener can be used to monitor a specified directory for changes. This listener will emit an event once
a change is detected within the directory and can be configured to check within subdirectories for changes as well. The
supported events are
* On file create
* On file delete
* On file modification
