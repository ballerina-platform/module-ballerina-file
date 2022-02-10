## Package Overview

This package provides APIs, which perform file, file path, and directory operations, and a `Directory Listener`, which is used to listen to a directory in the local file system.

This provides the interface to create, delete, rename the file/directory, retrieve metadata of the given file, and manipulate the
filename paths in a way that is compatible according to the target file paths defined by the operating system.

The path of the file/directory needs to be defined with either forward slashes or back slashes depending on the operating system.

### Directory Listener

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

### Report Issues

To report bugs, request new features, start new discussions, view project boards, etc., go to the [Ballerina standard library parent repository](https://github.com/ballerina-platform/ballerina-standard-library).

## Useful Links

- Chat live with us via our [Slack channel](https://ballerina.io/community/slack/).
- Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
