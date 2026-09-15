## Overview

This module provides APIs to create, delete, and rename files and directories, retrieve file metadata, manipulate file paths in an OS-compatible way, and listen to file system changes via a Directory Listener.

### Key Features

- Create, delete, and rename files and directories
- Retrieve file metadata
- OS-compatible file path manipulation, including path and path-list separators
- Directory Listener for reacting to file create, modify, and delete events
- Post-processing actions to delete or move a file after a listener remote method completes

### Path separators

This module provides the following separators which are widely used in file path creation:
-  `file:pathSeparator`: It is a character used to separate the parent directories that make up the path to a specific location. For Windows, it’s ‘\’ and for UNIX it’s ‘/’
-  `file:pathListSeparator`: It is a character commonly used by the operating system to separate paths in the path list. For windows, it’s ‘;‘ and for UNIX it’s ‘:’

### Directory listener

The `file:Listener` is used to monitor all the files and subdirectories inside the specified directory. 

A `Listener` endpoint can be defined using the mandatory `path` parameter and the optional `recursive` parameter as follows.

```ballerina
listener file:Listener inFolder = new ({
    path: "<The directory path>",
    recursive: false
});
```

If the listener needs to monitor subdirectories of the given directory, `recursive` needs to be set to `true`. The default value of this is `false`.

A `Service` has the defined remote methods with the `file:FileEvent` and can be exposed via a `Listener` endpoint. 
When there are changes in the listening directory, the `file:FileEvent` will be triggered with the action of the file 
such as creating, modifying, or deleting. 

The remote methods supported by the `Service` are as follows.

**onCreate:** This method is invoked once a new file is created in the listening directory.

**onDelete:** This method is invoked once an existing file is deleted from the listening directory.

**onModify:** This method is invoked once an existing file is modified in the listening directory.

The following code sample shows how to create a `Service` with the `onCreate` remote method and attach it to the above `Listener` endpoint:

```ballerina
service "localObserver" on inFolder {

    remote function onCreate(file:FileEvent m) returns error? {
        string msg = "Create: " + m.name;
        log:printInfo(msg);
    }
}
```

### Post-processing actions

The `@file:FunctionConfig` annotation on `onCreate` or `onModify` declares what happens to the file after the remote
method completes. `afterProcess` runs when the method returns successfully and `afterError` runs when it returns an
error or panics. Each can be `file:DELETE` or a `file:Move` record whose `moveTo` directory receives the file. With
`preserveSubDirs` (default `true`) the path relative to the listener's `path` is kept under `moveTo`. Missing
destination directories are created, and an existing file at the destination is not overwritten.

```ballerina
service "localObserver" on inFolder {

    @file:FunctionConfig {
        afterProcess: {moveTo: "/data/archive"},
        afterError: {moveTo: "/data/failed"}
    }
    remote function onCreate(file:FileEvent m) returns error? {
        check process(m.name);
    }
}
```
