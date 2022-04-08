# Proposal: Introduce the constants for path and path list separators

_Owners_: @daneshk @kalaiyarasiganeshalingam  
_Reviewers_: @daneshk  
_Created_: 2022/04/07   
_Updated_: 2022/04/07  
_Issues_: [#2834](https://github.com/ballerina-platform/ballerina-standard-library/issues/2834)

## Summary

The following two separators are widely used in the file system, and these values depend on the OS in the system as well.
-  Path separator: It is a character used to separate the parent directories that make up the path to a specific location. For windows, it’s ‘\’ and for UNIX it’s ‘/’
-  Path list separator: It is a character commonly used by the operating system to separate paths in the path list. For windows, it’s ‘;‘ and for UNIX it’s ‘:’

The file module has these constants for internal use. Now, this proposal is going to introduce these constants to be accessed by the user.

## Goals

Provide a way to get the separators according to the OS system.

## Motivation

When users use file APIs, they have to initialize the separator as a file path can't be created without these. If we introduce this in the file module, users can easily use it without initialization.

Note: This feature is also required by the Choreo team

## Description

The constants initialization is as follows:

```ballerina
final boolean isWindows = os:getEnv("OS") != "";
// OS-specific path separator.
public final string pathSeparator = isWindows ? "\\" : "/";
// OS-specific path list separator.
public final string pathListSeparator = isWindows ? ";" : ":";
````
