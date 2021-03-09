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

import ballerina/io;
import ballerina/log;
import ballerina/os;
import ballerina/regex;
import ballerina/jballerina.java;

final boolean isWindows = os:getEnv("OS") != "";
final string pathSeparator = isWindows ? "\\" : "/";
final string pathListSeparator = isWindows ? ";" : ":";

# Retrieves the absolute path from the provided location.
# ```ballerina
#  string|file:Error absolutePath = file:getAbsolutePath(<@untainted> "test.txt");
# ```
#
# + path - String value of the file path free from potential malicious codes
# + return - The absolute path reference or else a `file:Error` if the path cannot be derived
public isolated function getAbsolutePath(@untainted string path) returns string|Error = @java:Method {
    name: "absolute",
    'class: "org.ballerinalang.stdlib.file.nativeimpl.FilePathUtils"
} external;

# Reports whether the path is absolute.
# A path is absolute if it is independent of the current directory.
# On Unix, a path is absolute if it starts with the root.
# On Windows, a path is absolute if it has a prefix and starts with the root: c:\windows.
# ```ballerina
#  boolean|file:Error isAbsolute = file:isAbsolutePath("/A/B/C");
# ```
#
# + path - String value of the file path
# + return - `true` if path is absolute, `false` otherwise, or else an `file:Error`
#            occurred if the path is invalid
public isolated function isAbsolutePath(string path) returns boolean|Error {
    if (path.length() <= 0) {
        return false;
    }
    if (isWindows) {
        return check getVolumnNameLength(path) > 0;
    } else {
        return check charAt(path, 0) == "/";
    }
}

# Retrieves the base name of the file from the provided location,
# which is the last element of the path.
# Trailing path separators are removed before extracting the last element.
# ```ballerina
#  string|file:Error name = file:basename("/A/B/C.txt");
# ```
#
# + path - String value of file path
# + return - The name of the file or else a `file:Error` if the path is invalid
public isolated function basename(string path) returns string|Error {
    string validatedPath = check parse(path);
    int[] offsetIndexes = check getOffsetIndexes(validatedPath);
    int count = offsetIndexes.length();
    if (count == 0) {
        return "";
    }
    if (count == 1 && validatedPath.length() > 0) {
        if !(check isAbsolutePath(validatedPath)) {
            return validatedPath;
        }
    }
    int lastOffset = offsetIndexes[count - 1];
    return validatedPath.substring(lastOffset, validatedPath.length());
}

# Returns the enclosing parent directory.
# If the path is empty, parent returns ".".
# The returned path does not end in a separator unless it is the root directory.
# ```ballerina
#  string|file:Error parentPath = file:parentPath("/A/B/C.txt");
# ```
#
# + path - String value of the file/directory path
# + return - Path of the parent directory or else a `file:Error`
#            if an error occurred while getting the parent directory
public isolated function parentPath(string path) returns string|Error {
    string validatedPath = check parse(path);
    int[] offsetIndexes = check getOffsetIndexes(validatedPath);
    int count = offsetIndexes.length();
    if (count == 0) {
        return "";
    }
    int len = offsetIndexes[count-1] - 1;
    if (len < 0) {
        return "";
    }
    int offset;
    string root;
    [root, offset] = check getRoot(validatedPath);
    if (len < offset) {
        return root;
    }
    return validatedPath.substring(0, len);
}

# Normalizes a path value.
# ```ballerina
#  string|file:Error normalizedPath = file:normalizePath("foo/../bar", file:CLEAN);
# ```
#
# + path - String value of the file path
# + option - Normalization option. Supported options are,
#  `CLEAN` - Get the shortest path name equivalent to the given path by eliminating multiple separators, '.', and '..',
#  `SYMLINK` - Evaluate a symlink,
#  `NORMCASE` - Normalize the case of a pathname. On windows, all the characters are converted to lowercase and "/" is
# converted to "\\".
# + return - Normalized file path or else a `file:Error` if the path is invalid
public isolated function normalizePath(string path, NormOption option) returns string|Error {
    match option {

        CLEAN => {
            string validatedPath = check parse(path);
            int[] offsetIndexes = check getOffsetIndexes(validatedPath);
            int count = offsetIndexes.length();
            if (count == 0 || isEmpty(validatedPath)) {
                return validatedPath;
            }

            string root;
            int offset;
            [root, offset] = check getRoot(validatedPath);
            string c0 = check charAt(path, 0);

            int i = 0;
            string[] parts = [];
            boolean[] ignore = [];
            boolean[] parentRef = [];
            int remaining = count;
            while (i < count) {
                int begin = offsetIndexes[i];
                int length;
                ignore[i] = false;
                parentRef[i] = false;
                if (i == (count - 1)) {
                    length = validatedPath.length() - begin;
                    parts[i] = validatedPath.substring(begin, validatedPath.length());
                } else {
                    length = offsetIndexes[i + 1] - begin - 1;
                    parts[i] = validatedPath.substring(begin, offsetIndexes[i + 1] - 1);
                }
                if (check charAt(validatedPath, begin) == ".") {
                    if (length == 1) {
                        ignore[i] = true;
                        remaining = remaining - 1;
                    } else if (length == 2 && check charAt(validatedPath, begin + 1) == ".") {
                        parentRef[i] = true;
                        int j = i - 1;
                        boolean hasPrevious = false;
                        while (j >= 0) {
                            // A/B/<ignore>/..
                            if (ignore.length() > 0 && !parentRef[j] && !ignore[j]) {
                                ignore[j] = true;
                                remaining = remaining - 1;
                                hasPrevious = true;
                                break;
                            }
                            j = j - 1;
                        }
                        if (hasPrevious || (offset > 0) || isSlash(c0)) {
                            ignore[i] = true;
                            remaining = remaining - 1;
                        }
                    }
                }
                i = i + 1;
            }

            if (remaining == count) {
                return validatedPath;
            }

            if (remaining == 0) {
                return root;
            }

            string normalizedPath = "";
            if (root != "") {
                normalizedPath = normalizedPath + root;
            }
            i = 0;
            while (i < count) {
                if (!ignore[i] && (offset <= offsetIndexes[i])) {
                    normalizedPath = normalizedPath + parts[i] + pathSeparator;
                }
                i = i + 1;
            }
            return parse(normalizedPath);
        }

        SYMLINK => {
            return resolve(path);
        }

        NORMCASE => {
            if (isWindows) {
                string lowerCasePath = path.toLowerAscii();
                lowerCasePath = regex:replaceAll(lowerCasePath, "/", "\\\\");
                return lowerCasePath;
            }
            return path;
        }
        
        _ => {
            return error InvalidOperationError("Unsupported normalization option!");
        }
    }
}

# Splits a list of paths joined by the OS-specific path separator.
# ```ballerina
#  string[]|file:Error parts = file:splitPath("/A/B/C");
# ```
#
# + path - String value of the file path
# + return - String array of the part components or else a `file:Error` if the path is invalid
public isolated function splitPath(string path) returns string[]|Error {
    string validatedPath = check parse(path);
    int[] offsetIndexes = check getOffsetIndexes(validatedPath);
    int count = offsetIndexes.length();

    string[] parts = [];
    int i = 0;
    while (i < count) {
        int begin = offsetIndexes[i];
        int length;
        if (i == (count - 1)) {
            length = validatedPath.length() - begin;
            parts[i] = check parse(validatedPath.substring(begin, validatedPath.length()));
        } else {
            length = offsetIndexes[i + 1] - begin - 1;
            parts[i] = check parse(validatedPath.substring(begin, offsetIndexes[i + 1] - 1));
        }
        i = i + 1;
    }
    return parts;
}

# Joins any number of path elements into a single path.
# ```ballerina
#  string|file:Error path = file:joinPath("/", "foo", "bar");
# ```
#
# + parts - String values of the file path parts
# + return - String value of the file path or else a `file:Error` if the parts are invalid
public isolated function joinPath(string... parts) returns string|Error {
    if (isWindows) {
        return check buildWindowsPath(...parts);
    } else {
        return check buildUnixPath(...parts);
    }
}

# Returns a relative path, which is logically equivalent to the target path when joined to the base path with an
# intervening separator.
# An error is returned if the target path cannot be made relative to the base path.
# ```ballerina
#  string|file:Error relative = file:relativePath("a/b/e", "a/c/d");
# ```
#
# + base - String value of the base file path
# + target - String value of the target file path
# + return - The target path relative to the base path, or else an
#            `file:Error` if target path cannot be made relative to the base path
public isolated function relativePath(string base, string target) returns string|Error {
    string cleanBase = check normalizePath(base, CLEAN);
    string cleanTarget = check normalizePath(target, CLEAN);
    if (isSamePath(cleanBase, cleanTarget)) {
        return ".";
    }
    string baseRoot;
    int baseOffset;
    [baseRoot, baseOffset] = check getRoot(cleanBase);
    string targetRoot;
    int targetOffset;
    [targetRoot, targetOffset] = check getRoot(cleanTarget);
    if (!isSamePath(baseRoot, targetRoot)) {
        return error RelativePathError("Can't make: " + target + " relative to " + base);
    }
    int b0 = baseOffset;
    int bi = baseOffset;
    int t0 = targetOffset;
    int ti = targetOffset;
    int bl = cleanBase.length();
    int tl = cleanTarget.length();
    while (true) {
        while (bi < bl && !isSlash(check charAt(cleanBase, bi))) {
            bi = bi + 1;
        }
        while (ti < tl && !isSlash(check charAt(cleanTarget, ti))) {
            ti = ti + 1;
        }
        if (!isSamePath(cleanBase.substring(b0, bi), cleanTarget.substring(t0, ti))) {
            break;
        }
        if (bi < bl) {
           bi = bi + 1;
        }
        if (ti < tl) {
            ti = ti + 1;
        }
        b0 = bi;
        t0 = ti;
    }
    if (cleanBase.substring(b0, bi) == "..") {
        return error RelativePathError("Can't make: " + target + " relative to " + base);
    }
    if (b0 != bl) {
        string remainder = cleanBase.substring(b0, bl);
        int[] offsets = check getOffsetIndexes(remainder);
        int noSeparators = offsets.length() - 1;
        string relativePath = "..";
        int i = 0;
        while (i < noSeparators) {
            relativePath = relativePath + pathSeparator + "..";
            i = i + 1;
        }
        if (t0 != tl) {
            relativePath = relativePath + pathSeparator + cleanTarget.substring(t0, tl);
        }
        return relativePath;
    }
    return cleanTarget.substring(t0, tl);
}

# Returns the filepath after the evaluation of any symbolic links.
# If the path is relative, the result will be relative to the current directory
# unless one of the components is an absolute symbolic link.
# Resolves normalising the calls on the result.
#
# + path - Security-validated string value of the file path
# + return - Resolved file path or else a `file:Error` if the path is invalid
isolated function resolve(@untainted string path) returns string|Error = @java:Method {
    name: "resolve",
    'class: "org.ballerinalang.stdlib.file.nativeimpl.FilePathUtils"
} external;

# Parses the give path and remove redundent slashes.
#
# + input - String path value
# + return - Parsed path or else a `file:Error` if the given path is invalid
isolated function parse(string input) returns string|Error {
    if (input.length() <= 0) {
        return input;
    }
    if (isWindows) {
        int offset = 0;
        string root = "";
        [root, offset] = check getRoot(input);
        return root + check parseWindowsPath(input, offset);
    } else {
        int n = input.length();
        string prevC = "";
        int i = 0;
        while (i < n) {
            string c = check charAt(input, i);
            if ((c == "/") && (prevC == "/")) {
                return parsePosixPath(input, i - 1);
            }
            prevC = c;
            i = i + 1;
        }
        if (prevC == "/") {
            return parsePosixPath(input, n - 1);
        }
        return input;
    }
}

isolated function getRoot(string input) returns [string,int]|Error {
    if (isWindows) {
        return getWindowsRoot(input);
    } else {
        return getUnixRoot(input);
    }
}

isolated function isSlash(string c) returns boolean {
    if (isWindows) {
        return isWindowsSlash(c);
    } else {
        return isPosixSlash(c);
    }
}

isolated function nextNonSlashIndex(string path, int offset, int end) returns int|Error {
    int off = offset;
    while(off < end && isSlash(check charAt(path, off))) {
        off = off + 1;
    }
    return off;
}

isolated function nextSlashIndex(string path, int offset, int end) returns int|Error {
    int off = offset;
    while(off < end && !isSlash(check charAt(path, off))) {
        off = off + 1;
    }
    return off;
}

isolated function isLetter(string c) returns boolean {
    string regEx = "^[a-zA-Z]{1}$";
    boolean|error letter = regex:matches(c,regEx);
    if (letter is error) {
        log:printError("Error while checking input character is string", err = letter);
        return false;
    } else {
        return letter;
    }
}

isolated function isUNC(string path) returns boolean|Error {
    return check getVolumnNameLength(path) > 2;
}

isolated function isEmpty(string path) returns boolean {
    return path.length() == 0;
}

isolated function getOffsetIndexes(string path) returns int[]|Error {
    if (isWindows) {
        return check getWindowsOffsetIndex(path);
    } else {
        return check getUnixOffsetIndex(path);
    }
}

isolated function charAt(string input, int index) returns string|Error {
    int length = input.length();
    if (index > length) {
        return error GenericError(io:sprintf("Character index %d is greater then path string length %d",
        index, length));
    }
    return input.substring(index, index + 1);
}

isolated function isSamePath(string base, string target) returns boolean {
    if (isWindows) {
        return base.equalsIgnoreCaseAscii(target);
    } else {
        return base == target;
    }
}
