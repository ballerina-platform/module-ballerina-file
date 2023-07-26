# Overview

This application shows how to implement a simple file and directory management system by using `file`, `http`, `io` and `email` Ballerina packages.
There are two subparts involved in this system:
- **Observer**: It listens to a directory in the local file system and notifies via email when new files are created in the directory or when the existing files are deleted or modified.
- **Manager**: This is used to manage the files or directories in the given base directory in the local file system by creating, modifying, or deleting files/directories.

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
      curl -v -X POST http://localhost:9090/file_manager/file/test.txt
      ```
    * Modify the file
      ```
      curl -v -X PUT http://localhost:9090/file_manager/file/test.txt --data "hi" 
      ```
    * Delete the file
      ```
      curl -v -X DELETE http://localhost:9090/file_manager/file/test.txt
      ```
    * Create the directory
      ```
      curl -v -X POST http://localhost:9090/file_manager/directory/file_dir
      ```
    * Get metadata of the given directory
      ```
      curl -v -X GET http://localhost:9090/file_manager/directory/metadata/file_dir
      ```
    * Copy the directory
      ```
      curl -v -X POST http://localhost:9090/file_manager/directory/file_dir/new_dir
      ```
    * Rename the directory
      ```
      curl -v -X PUT http://localhost:9090/file_manager/directory/new_dir/new_diretory
      ```
    * Delete the directory
      ```
      curl -v -X DELETE http://localhost:9090/file_manager/directory/file_dir
      ```
