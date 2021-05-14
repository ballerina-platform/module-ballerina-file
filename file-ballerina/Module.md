## Overview

This module provides APIs which perform file, file path, and directory operations and a `Directory Listener` which is used to listen a directory in the local file system.

This provide the interface to create, delete, rename the file/directory, retrieve metadata of the given file, and manipulate 
filename paths in a way compatible according to the target operating system-defined file paths.

The path of the file/directory needs to be defined with either forward slashes or backslashes depending on the operating system.

### Directory Listener

The `file:Listener` is used to monitor all the files and subdirectories inside the specified directory. 

A `Listener` endpoint can be defined using the mandatory parameter `path` and the optional parameter `recursive` as follows:

```ballerina
listener file:Listener inFolder = new ({
    path: "<The directory path>",
    recursive: false
});
```

If the listener needs to monitor subdirectories of the given directory, needs to be set `recursive` to true. The default value of this is false.

A `Service` has the defined remote methods with the `file:FileEvent` and can be exposed via a `Listener` endpoint. 
When there are changes in the listening directory, the `file:FileEvent` will be triggered with the action of the file 
such as creating, modifying or deleting. 

The remote methods supported by the `Service`:

**onCreate:** This method is invoked once a new file is created in the listening directory.

**onDelete:** This method is invoked once an existing file is deleted from the listening directory.

**onDelete:** This method is invoked once an existing file is modified in the listening directory.

The following code sample shows how to create a `Service` with `onCreate` remote method and attach it to the above `Listener` endpoint:

```ballerina
service "localObserver" on inFolder {

    remote function onCreate(file:FileEvent m) {
        string msg = "Create: " + m.name;
        log:printInfo(msg);
    }
}
```

For information on the operations, which you can perform with the regex module, see the below **Functions**.
