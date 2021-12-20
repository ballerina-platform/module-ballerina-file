# Overview

This application shows how to implement a simple file management system by using `file`, `http`, `io` and `email` Ballerina packages.
There are two subparts involved in this system:
* observer: It listens to a directory in the local file system and notifies via email when new files are created in the directory or when the existing files are deleted or modified.
* manager: This is used to manage the files in the directory in the local file system by creating, modifying, or deleting files.

## Prerequisite

Update all the configurations in the `Config.toml` file of `observer` and `manager`.

## Run the example

First, clone this repository, and then, run the following commands to run this example in your local machine. Use separate terminals for each step.

1. Run the observer.
```ballerina
$ cd examples/file_management_system/observer
$ bal run
```

2. Run the manager.
```ballerina
$ cd examples/file_management_system/manager
$ bal run
```

3. Send a request to manage files using curl.
    * Create the file
      ```
      curl -v -X POST http://localhost:9090/file/create/test.txt
      ```
    * Modify the file
      ```
      curl -v -X PUT http://localhost:9090/file/edit/test.txt --data "hi" 
      ```
    * Delete the file
      ```
      curl -v -X DELETE http://localhost:9090/file/delete/test.txt
      ```
