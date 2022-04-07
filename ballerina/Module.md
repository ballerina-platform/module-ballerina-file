## Overview

This module provides APIs to create, delete, rename the file/directory, retrieve metadata of the given file, and manipulate the file paths in a way that is compatible with the operating system, and a `Directory Listener`, which is used to listen to the file changes in a directory in the local file system.

This also provides the following separators which are widely used in file path creation:
-  `file:pathSeparator`: It is a character used to separate the parent directories that make up the path to a specific location. For windows, it’s ‘\’ and for UNIX it’s ‘/’
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

    remote function onCreate(file:FileEvent m) {
        string msg = "Create: " + m.name;
        log:printInfo(msg);
    }
}
```

For information on the operations, which you can perform with the file module, see the below **Functions**.
