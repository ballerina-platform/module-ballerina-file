// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/test;
import ballerina/system;
import ballerina/java;
import ballerina/stringutils;

boolean isWin = system:getEnv("OS") != "";

@test:Config {}
function testPathSeparator() {
    string sep = getPathSeparator();
    if (isWin) {
        test:assertEquals(sep, "\\");
    } else {
        test:assertEquals(sep, "/");
    }
}

@test:Config {}
function testPathListSeparator() {
    string sep = getPathListSeparator();
    if (isWin) {
        test:assertEquals(sep, ";");
    } else {
        test:assertEquals(sep, ":");
    }
}

@test:Config {}
function testGetAbsolutePath() {
    string absPathFrmUtil = getAbsPath(java:fromString("test.txt"));
    string|error absPath = absolute("test.txt");
    if (absPath is string) {
        test:assertEquals(absPath, absPathFrmUtil);
    } else {
        test:assertFail("Error retrieving absolute path");
    }
}

@test:Config {}
function testAbsolutePath() {
    string absPathFrmUtil = getAbsPath(java:fromString("/test.txt"));
    string|error absPath = absolute("/test.txt");
    if (absPath is string) {
        test:assertEquals(absPath, absPathFrmUtil);
    } else {
        test:assertFail("Error retrieving absolute path");
    }
}

@test:Config {}
function testIllegalWindowsPath() {
    string illegal = "/C:/Users/Desktop/workspaces/dk/ballerina/stdlib-path/target/test-classes/absolute" +
                "\\swagger.json";
    string|error absPath = absolute(illegal);
    if (isWin) {
        test:assertTrue(absPath is error);
        if(absPath is error) {
            test:assertTrue(stringutils:contains(absPath.message(), "Invalid path"));
        }
    } else {
        if(absPath is string) {
            test:assertEquals(absPath, getAbsPath(java:fromString(illegal)));
        }
    }
}

@test:Config {
    dataProvider: "testIsAbsPathDataProvider"
}
function testIsAbsolutePath(string path, string posixOutput, string windowsOutput) {
    if(isWin) {
        validateAbsolutePath(path, windowsOutput);
    } else {
        validateAbsolutePath(path, posixOutput);
    }
}

@test:Config {
    dataProvider: "getFileNameDataset"
}
function testGetFileName(string path, string posixOutput, string windowsOutput){
    if(isWin) {
        validateFilename(path, windowsOutput);
    } else {
        validateFilename(path, posixOutput);
    }
}

@test:Config {
    dataProvider: "getParentDataset"
}
function testGetParent(string path, string posixOutput, string windowsOutput){
    if(isWin) {
        validateParent(path, windowsOutput);
    } else {
        validateParent(path, posixOutput);
    }
}

@test:Config {
    dataProvider: "getNormalizedDataset"
}
function testPosixNormalizePath(string path, string posixOutput, string windowsOutput){
    if(isWin) {
        validateNormalizePath(path, windowsOutput);
    } else {
        validateNormalizePath(path, posixOutput);
    }
}

@test:Config {
    dataProvider: "getSplitDataset"
}
function testSplitPath(string path, string posixOutput, string windowsOutput){
    if(isWin) {
        validateSplitPath(path, windowsOutput);
    } else {
        validateSplitPath(path, posixOutput);
    }
}

@test:Config {
    dataProvider: "getPosixFileParts"
}
function testPosixBuildPath([string[], string] params){
    string[] a;
    string b;
    [a, b] = params;
    if (!isWin) {
        validateBuildPath(a, b);
    }
}

@test:Config {
    dataProvider: "getWindowsFileParts"
}
function testBuildPath([string[], string] params){
    string[] a;
    string b;
    [a, b] = params;
    if (isWin) {
        validateBuildPath(a, b);
    }
}

@test:Config {
    dataProvider: "getExtensionsSet"
}
function testPathExtension(string path, string posixOutput, string windowsOutput) {
    if(isWin) {
        validateFileExtension(path, windowsOutput);
    } else {
        validateFileExtension(path, posixOutput);
    }
}

@test:Config {
    dataProvider: "getRelativeSet"
}
function testRelativePath(string path, string targetPath, string posixOutput, string windowsOutput) {
    if(isWin) {
        validateRelativePath(path, targetPath, windowsOutput);
    } else {
        validateRelativePath(path, targetPath, posixOutput);
    }
}

@test:Config {
    dataProvider: "getMatchesSet"
}
function testPathMatch(string pattern, string path, string posixOutput, string windowsOutput) {
    if(isWin) {
        validatePathMatch(pattern, path, windowsOutput);
    } else {
        validatePathMatch(pattern, path, posixOutput);
    }
}

@test:Config {}
function testResolvePath() {
    if (!isWin) {
        error? pathCreated = trap createLink();
        if (pathCreated is error) {
            test:assertFail("Error creating symlink!");
        } else {
            string path = tempDir() + "/test_link.txt";
            string|error resPath = resolve(path);
            if(resPath is string) {
                string|error expected = getSymLink();
                if(expected is string) {
                    test:assertEquals(resPath, expected);
                    error? removeLinkResult = trap removeLink();
                    if(removeLinkResult is error) {
                        test:assertFail("Error removing symlink!");
                    }
                } else {
                    test:assertFail("Error retrieving symlink!");
                }
            } else {
                test:assertFail(resPath.message());
            }
        }
    }
}

@test:Config {}
function testResolveNotLinkPath() {
    string path = "src/file/tests/resources/test.txt";
    string|error resPath = resolve(path);
    if(resPath is error) {
        test:assertTrue(stringutils:contains(resPath.message(), "Path is not a symbolic link"));
    } else {
        test:assertFail("Error resolving path!");
    }
}

@test:Config {}
function testResolveNonExistencePath() {
    string path = "src/file/tests/resources/test_non_existent.txt";
    string|error resPath = resolve(path);
    if(resPath is error) {
        test:assertTrue(stringutils:contains(resPath.message(), "File does not exist"));
    } else {
        test:assertFail("Error resolving path!");
    }
}

//Util functions

function validateAbsolutePath(string path, string expected) {
    boolean|error isAbs = isAbsolute(path);
    if(isAbs is boolean) {
        test:assertEquals(isAbs, stringutils:toBoolean(expected));
    } else {
        test:assertFail("Error checking is-absolute!");
    }
}

function validateFilename(string path, string expected) {
    string|error fname = filename(path);
    if(expected=="error") {
        test:assertTrue(fname is error);
        if (fname is error) {
            test:assertTrue(stringutils:contains(fname.message(), "UNC path"));
        }
    } else {
        test:assertTrue(fname is string);
        if(fname is string) {
            test:assertEquals(fname, expected);
        }
    }
}

function validateParent(string input, string expected) {
    string|error parentName = parent(input);
    if(expected=="error") {
        test:assertTrue(parentName is error);
        if (parentName is error) {
            test:assertTrue(stringutils:contains(parentName.message(), "UNC path"));
        }
    } else {
        test:assertTrue(parentName is string);
        if(parentName is string) {
            test:assertEquals(parentName, expected);
        }
    }
}

function validateNormalizePath(string input, string expected) {
    string|error normPath = normalize(input);
    if(expected=="error") {
        test:assertTrue(normPath is error);
        if (normPath is error) {
            test:assertTrue(stringutils:contains(normPath.message(), "UNC path"));
        }
    } else {
        test:assertTrue(normPath is string);
        if(normPath is string) {
            test:assertEquals(normPath, expected);
        }
    }
}

function validateSplitPath(string input, string expected) {
    string[]|error splitPath = split(input);
    if(expected=="error") {
        test:assertTrue(splitPath is error);
        if (splitPath is error) {
            test:assertTrue(stringutils:contains(splitPath.message(), "UNC path"));
        }
    } else {
        test:assertTrue(splitPath is string[]);
        if(splitPath is string[]) {
            string[] exvalues = stringutils:split(expected, ",");
            int i = 0;
            int arrSize = splitPath.length();
            while (i < arrSize) {
                 test:assertEquals(splitPath[i], exvalues[i]);
                 i = i + 1;
            }
        }
    }
}

function validateBuildPath(string[] parts, string expected) {
    string|error bpath = build(...parts);
    if(expected=="error") {
        test:assertTrue(bpath is error);
        if (bpath is error) {
            test:assertTrue(stringutils:contains(bpath.message(), "UNC path"));
        }
    } else {
        test:assertTrue(bpath is string);
        if(bpath is string) {
            test:assertEquals(bpath, expected);
        }
    }
}

function validateFileExtension(string input, string expected) {
    string|error extName = extension(input);
    if(extName is string) {
        test:assertEquals(extName, expected);
    } else {
        test:assertFail("Error retrieving extension!");
    }
}

function validateRelativePath(string basePath, string targetPath, string expected) {
    string|error relPath = relative(basePath, targetPath);
    if(expected=="error") {
        test:assertTrue(relPath is error);
        if (relPath is error) {
            test:assertTrue(stringutils:contains(relPath.message(), "Can't make: " + targetPath + " relative to " + basePath));
        }
    } else {
        test:assertTrue(relPath is string);
        if(relPath is string) {
            test:assertEquals(relPath, expected);
        }
    }
}

function validatePathMatch(string pattern, string path, string expected) {
    boolean|error matchFound = matches(path, pattern);
    if(expected=="error") {
        test:assertTrue(matchFound is error);
        if (matchFound is error) {
            test:assertTrue(stringutils:contains(matchFound.message(), "Invalid pattern"));
        }
    } else {
        test:assertTrue(matchFound is boolean);
        if(matchFound is boolean) {
            test:assertEquals(matchFound, stringutils:toBoolean(expected));
        }
    }
}

//Data providers

function testIsAbsPathDataProvider() returns (string[][]) {
    return [
        ["/A/B/C", "true", "false"],
        ["/foo/..", "true", "false"],
        [".", "false", "false"],
        ["..", "false", "false"],
        ["../../", "false", "false"],
        ["foo/", "false", "false"],
        ["foo/bar/", "false", "false"],
        ["/AAA/////BBB/", "true", "false"],
        ["", "false", "false"],
        ["//////////////////", "true", "false"],
        ["\\\\\\\\\\\\\\\\\\\\", "false", "false"],
        ["/foo/./bar", "true", "false"],
        ["foo/../bar", "false", "false"],
        ["../foo/bar", "false", "false"],
        ["./foo/bar/../", "false", "false"],
        ["../../foo/../bar/zoo", "false", "false"],
        ["abc/../../././../def", "false", "false"],
        ["abc/def/../../..", "false", "false"],
        ["abc/def/../../../ghi/jkl/../../../mno", "false", "false"],
        ["//server", "true", "false"],
        ["\\\\server", "false", "false"],
        ["\\\\host\\share\\foo", "false", "true"],
        ["C:/foo/..", "false", "true"],
        ["C:\\foo\\..", "false", "true"],
        ["D;\\bar\\baz", "false", "false"],
        ["bar\\baz", "false", "false"],
        ["bar/baz", "false", "false"],
        ["C:\\\\\\\\", "false", "true"],
        ["\\..\\A\\B", "false", "false"]
    ];
}

function getFileNameDataset() returns (string[][]) {
     return [
        ["/A/B/C", "C", "C"],
        ["/foo/..", "..", ".."],
        [".", ".", "."],
        ["..", "..", ".."],
        ["../../", "..", ".."],
        ["foo/", "foo", "foo"],
        ["foo/bar/", "bar", "bar"],
        ["/AAA/////BBB/", "BBB", "BBB"],
        ["", "", ""],
        ["//////////////////", "", "error"],
        ["\\\\\\\\\\\\\\\\\\\\", "\\\\\\\\\\\\\\\\\\\\", "error"],
        ["/foo/./bar", "bar", "bar"],
        ["foo/../bar", "bar", "bar"],
        ["../foo/bar", "bar", "bar"],
        ["./foo/bar/../", "..", ".."],
        ["../../foo/../bar/zoo", "zoo", "zoo"],
        ["abc/../../././../def", "def", "def"],
        ["abc/def/../../..", "..", ".."],
        ["abc/def/../../../ghi/jkl/../../../mno", "mno", "mno"],
        // windows paths
        ["//server", "server", "error"],
        ["\\\\server", "\\\\server", "error"],
        ["C:/foo/..", "..", ".."],
        ["C:\\foo\\..", "C:\\foo\\..", ".."],
        ["D;\\bar\\baz", "D;\\bar\\baz", "baz"],
        ["bar\\baz", "bar\\baz", "baz"],
        ["bar/baz", "baz", "baz"],
        ["C:\\\\\\\\", "C:\\\\\\\\", ""],
        ["\\..\\A\\B", "\\..\\A\\B", "B"],
        ["c:\\test.txt", "c:\\test.txt", "test.txt"]
     ];
 }

function getParentDataset() returns (string[][]) {
    return [
        ["/A/B/C", "/A/B", "\\A\\B"],
        ["/foo/..", "/foo", "\\foo"],
        [".", "", ""],
        ["..", "", ""],
        ["../../", "..", ".."],
        ["foo/", "", ""],
        ["foo/bar/", "foo", "foo"],
        ["/AAA/////BBB/", "/AAA", "\\AAA"],
        ["", "", ""],
        ["//////////////////", "", "error"],
        ["\\\\\\\\\\\\\\\\\\\\", "", "error"],
        ["/foo/./bar", "/foo/.", "\\foo\\."],
        ["foo/../bar", "foo/..", "foo\\.."],
        ["../foo/bar", "../foo", "..\\foo"],
        ["./foo/bar/../", "./foo/bar", ".\\foo\\bar"],
        ["../../foo/../bar/zoo", "../../foo/../bar", "..\\..\\foo\\..\\bar"],
        ["abc/../../././../def", "abc/../../././..", "abc\\..\\..\\.\\.\\.."],
        ["abc/def/../../..", "abc/def/../..", "abc\\def\\..\\.."],
        ["abc/def/../../../ghi/jkl/../../../mno", "abc/def/../../../ghi/jkl/../../..",
                "abc\\def\\..\\..\\..\\ghi\\jkl\\..\\..\\.."],
        ["/", "", ""],
        ["/A", "/", "\\"],
        // windows paths
        ["//server", "/", "error"],
        ["\\\\server", "", "error"],
        ["C:/foo/..", "C:/foo", "C:\\foo"],
        ["C:\\foo\\..", "", "C:\\foo"],
        ["D;\\bar\\baz", "", "D;\\bar"],
        ["bar\\baz", "", "bar"],
        ["bar/baz", "bar", "bar"],
        ["C:\\\\\\\\", "", ""],
        ["\\..\\A\\B", "", "\\..\\A"]
    ];
}

function getNormalizedDataset() returns (string[][]) {
    return [
        ["/A/B/C", "/A/B/C", "\\A\\B\\C"],
        ["/foo/..", "/", "\\"],
        [".", "", ""],
        ["..", "..", ".."],
        ["../../", "../..", "..\\.."],
        ["foo/", "foo", "foo"],
        ["foo/bar/", "foo/bar", "foo\\bar"],
        ["/AAA/////BBB/", "/AAA/BBB", "\\AAA\\BBB"],
        ["", "", ""],
        ["//////////////////", "/", "error"],
        ["\\\\\\\\\\\\\\\\\\\\", "\\\\\\\\\\\\\\\\\\\\", "error"],
        ["/foo/./bar", "/foo/bar", "\\foo\\bar"],
        ["foo/../bar", "bar", "bar"],
        ["../foo/bar", "../foo/bar", "..\\foo\\bar"],
        ["./foo/bar/../", "foo", "foo"],
        ["../../foo/../bar/zoo", "../../bar/zoo", "..\\..\\bar\\zoo"],
        ["abc/../../././../def", "../../def", "..\\..\\def"],
        ["abc/def/../../..", "..", ".."],
        ["abc/def/../../../ghi/jkl/../../../mno", "../../mno",
                "..\\..\\mno"],
        ["/", "/", "\\"],
        ["/A", "/A", "\\A"],
        ["/../A/B", "/A/B", "\\A\\B"],
        // windows paths
        ["//server", "/server", "error"],
        ["\\\\server", "\\\\server", "error"],
        ["C:/foo/..", "C:", "C:\\"],
        ["C:\\foo\\..", "C:\\foo\\..", "C:\\"],
        ["C:\\..\\foo", "C:\\..\\foo", "C:\\foo"],
        ["D;\\bar\\baz", "D;\\bar\\baz", "D;\\bar\\baz"],
        ["bar\\baz", "bar\\baz", "bar\\baz"],
        ["bar/baz", "bar/baz", "bar\\baz"],
        ["C:\\\\\\\\", "C:\\\\\\\\", "C:\\"],
        ["\\..\\A\\B", "\\..\\A\\B", "\\A\\B"]
    ];
}

function getSplitDataset() returns (string[][]) {
    return [
        ["/A/B/C", "A,B,C", "A,B,C"],
        ["/foo/..", "foo,..", "foo,.."],
        [".", ".", "."],
        ["..", "..", ".."],
        ["../../", "..,..", "..,.."],
        ["foo/", "foo", "foo"],
        ["foo/bar/", "foo,bar", "foo,bar"],
        ["/AAA/////BBB/", "AAA,BBB", "AAA,BBB"],
        ["", "", ""],
        ["//////////////////", "/", "error"],
        ["\\\\\\\\\\\\\\\\\\\\", "\\\\\\\\\\\\\\\\\\\\", "error"],
        ["/foo/./bar", "foo,.,bar", "foo,.,bar"],
        ["foo/../bar", "foo,..,bar", "foo,..,bar"],
        ["../foo/bar", "..,foo,bar", "..,foo,bar"],
        ["./foo/bar/../", ".,foo,bar,..", ".,foo,bar,.."],
        ["../../foo/../bar/zoo", "..,..,foo,..,bar,zoo", "..,..,foo,..,bar,zoo"],
        ["abc/../../././../def", "abc,..,..,.,.,..,def", "abc,..,..,.,.,..,def"],
        ["abc/def/../../..", "abc,def,..,..,..", "abc,def,..,..,.."],
        ["abc/def/../../../ghi/jkl/../../../mno", "abc,def,..,..,..,ghi,jkl,..,..,..,mno",
                "abc,def,..,..,..,ghi,jkl,..,..,..,mno"],
        ["/", "/", "\\"],
        ["/A", "A", "A"],
        ["/../A/B", "..,A,B", "..,A,B"],
        // windows paths
        ["//server", "server", "error"],
        ["\\\\server", "\\\\server", "error"],
        ["C:/foo/..", "C:,foo,..", "foo,.."],
        ["C:\\foo\\..", "C:\\foo\\..", "foo,.."],
        ["C:\\..\\foo", "C:\\..\\foo", "..,foo"],
        ["D;\\bar\\baz", "D;\\bar\\baz", "D;,bar,baz"],
        ["bar\\baz", "bar\\baz", "bar,baz"],
        ["bar/baz", "bar,baz", "bar,baz"],
        ["C:\\\\\\\\", "C:\\\\\\\\", "C:\\"],
        ["\\..\\A\\B", "\\..\\A\\B", "..,A,B"]
    ];
}

function getPosixFileParts() returns ([string[], string][][]) {
    return [
        [[[], ""]],
        [[[""], ""]],
        [[["/"], "/"]],
        [[["a"], "a"]],
        [[["A", "B", "C"], "A/B/C"]],
        [[["a", ""], "a"]],
        [[["", "b"], "b"]],
        [[["/", "a"], "/a"]],
        [[["/", "a/b"], "/a/b"]],
        [[["/", ""], "/"]],
        [[["//", "a"], "/a"]],
        [[["/a", "b"], "/a/b"]],
        [[["a/", "b"], "a/b"]],
        [[["a/", ""], "a"]],
        [[["", ""], ""]],
        [[["/", "a", "b"], "/a/b"]],
        [[["C:\\", "test", "data\\eat"], "C:\\/test/data\\eat"]],
        [[["C:", "test", "data\\eat"], "C:/test/data\\eat"]]
    ];
}

function getWindowsFileParts() returns ([string[], string][][]) {
    return [
        [[["directory", "file"], "directory\\file"]],
        [[["C:\\Windows\\", "System32"], "C:\\Windows\\System32"]],
        [[["C:\\Windows\\", ""], "C:\\Windows"]],
        [[["C:\\", "Windows"], "C:\\Windows"]],
        [[["C:", "a"], "C:a"]],
        [[["C:", "a\\b"], "C:a\\b"]],
        [[["C:", "a", "b"], "C:a\\b"]],
        [[["C:", "", "b"], "C:b"]],
        [[["C:", "", "", "b"], "C:b"]],
        [[["C:", ""], "C:"]],
        [[["C:", "", ""], "C:"]],
        [[["C:.", "a"], "C:\\a"]],
        [[["C:a", "b"], "C:a\\b"]],
        [[["C:a", "b", "d"], "C:a\\b\\d"]],
        [[["\\\\host\\share", "foo"], "\\\\host\\share\\foo"]],
        [[["\\\\host\\share\\foo"], "\\\\host\\share\\foo"]],
        [[["//host/share", "foo/bar"], "\\\\host\\share\\foo\\bar"]],
        [[["\\"], "\\"]],
        [[["\\", ""], "\\"]],
        [[["\\", "a"], "\\a"]],
        [[["\\", "a", "b"], "\\a\\b"]],
        [[["\\", "\\\\a\\b", "c"], "\\a\\b\\c"]],
        [[["\\\\a", "b", "c"], "error"]],
        [[["\\\\a\\", "b", "c"], "error"]]
    ];
}

function getExtensionsSet() returns (string[][]) {
    return [
        ["path.bal", "bal", "bal"],
        ["path.pb.bal", "bal", "bal"],
        ["a.pb.bal/b", "", ""],
        ["a.toml/b.bal", "bal", "bal"],
        ["a.pb.bal/", "", ""],
        ["\\..\\A\\B.foo", "foo", "foo"],
        ["C:\\foo\\..\\bar", "\\bar", ""]
    ];
}

function getRelativeSet() returns (string[][]) {
    return [
        ["a/b", "a/b", ".", "."],
        ["a/b/.", "a/b", ".", "."],
        ["a/b", "a/b/.", ".", "."],
        ["./a/b", "a/b", ".", "."],
        ["a/b", "./a/b", ".", "."],
        ["ab/cd", "ab/cde", "../cde", "..\\cde"],
        ["ab/cd", "ab/c", "../c", "..\\c"],
        ["a/b", "a/b/c/d", "c/d", "c\\d"],
        ["a/b", "a/b/../c", "../c", "..\\c"],
        ["a/b/../c", "a/b", "../b", "..\\b"],
        ["a/b/c", "a/c/d", "../../c/d", "..\\..\\c\\d"],
        ["a/b", "c/d", "../../c/d", "..\\..\\c\\d"],
        ["a/b/c/d", "a/b", "../..", "..\\.."],
        ["a/b/c/d", "a/b/", "../..", "..\\.."],
        ["a/b/c/d/", "a/b", "../..", "..\\.."],
        ["a/b/c/d/", "a/b/", "../..", "..\\.."],
        ["../../a/b", "../../a/b/c/d", "c/d", "c\\d"],
        ["/a/b", "/a/b", ".", "."],
        ["/a/b/.", "/a/b", ".", "."],
        ["/a/b", "/a/b/.", ".", "."],
        ["/ab/cd", "/ab/cde", "../cde", "..\\cde"],
        ["/ab/cd", "/ab/c", "../c", "..\\c"],
        ["/a/b", "/a/b/c/d", "c/d", "c\\d"],
        ["/a/b", "/a/b/../c", "../c", "..\\c"],
        ["/a/b/../c", "/a/b", "../b", "..\\b"],
        ["/a/b/c", "/a/c/d", "../../c/d", "..\\..\\c\\d"],
        ["/a/b", "/c/d", "../../c/d", "..\\..\\c\\d"],
        ["/a/b/c/d", "/a/b", "../..", "..\\.."],
        ["/a/b/c/d", "/a/b/", "../..", "..\\.."],
        ["/a/b/c/d/", "/a/b", "../..", "..\\.."],
        ["/a/b/c/d/", "/a/b/", "../..", "..\\.."],
        ["/../../a/b", "/../../a/b/c/d", "c/d", "c\\d"],
        [".", "a/b", "a/b", "a\\b"],
        [".", "..", "..", ".."],
        ["..", ".", "error", "error"],
        ["..", "a", "error", "error"],
        ["../..", "..", "error", "error"],
        ["a", "/a", "error", "error"],
        ["/a", "a", "error", "error"],
        ["C:a\\b\\c", "C:a/b/d", "../C:a/b/d", "..\\d"],
        ["C:\\", "D:\\", "../D:\\", "error"],
        ["C:", "D:", "../D:", "error"],
        ["C:\\Projects", "c:\\projects\\src", "../c:\\projects\\src", "src"],
        ["C:\\Projects", "c:\\projects", "../c:\\projects", "."],
        ["C:\\Projects\\a\\..", "c:\\projects", "../c:\\projects", "."]
    ];
}

function getMatchesSet() returns (string[][]) {
    return [
        ["abc", "abc", "true", "true"],
        ["*", "abc", "true", "true"],
        ["*c", "abc", "true", "true"],
        ["a*", "a", "true", "true"],
        ["a*", "abc", "true", "true"],
        ["a*", "ab/c", "false", "false"],
        ["a*/b", "abc/b", "true", "true"],
        ["a*/b", "a/c/b", "false", "false"],
        ["A*B*C*D*E*/f", "AxBxCxDxE/f", "true", "true"],
        ["a*b*c*d*e*/f", "axbxcxdxexxx/f", "true", "true"],
        ["a*b*c*d*e*/f", "axbxcxdxe/xxx/f", "false", "false"],
        ["a*b*c*d*e*/f", "axbxcxdxexxx/fff", "false", "false"],
        ["a*b?c*x", "abxbbxdbxebxczzx", "true", "true"],
        ["a*b?c*x", "abxbbxdbxebxczzy", "false", "false"],
        ["ab[c]", "abc", "true", "true"],
        ["ab[b-d]", "abc", "true", "true"],
        ["ab[e-g]", "abc", "false", "false"],
        ["[a-b-c]", "a", "error", "error"],
        ["[", "a", "error", "error"],
        ["a[", "a", "error", "error"],
        ["[-]", "-", "true", "true"],
        ["[x-]", "x", "true", "true"],
        ["[]a]", "a", "error", "error"],
        ["[\\-x]", "x", "true", "error"],
        ["a?b", "a/b", "false", "false"],
        ["a*b", "a/b", "false", "false"],
        ["[\\-]", "-", "true", "error"],
        ["[x\\-]", "x", "true", "error"],
        ["[x\\-]", "-", "true", "error"],
        ["[x\\-]", "z", "false", "error"],
        ["[\\-x]", "x", "true", "error"],
        ["[\\-x]", "z", "false", "error"],
        ["[\\-x]", "-", "false", "error"],
        ["[\\-x]", "a", "true", "error"]
    ];
}

//Interops

function getAbsPath(handle path) returns string = @java:Method {
     'class: "org.ballerinalang.stdlib.file.testutils.TestUtil"
} external;

function createLink() = @java:Method {
    'class: "org.ballerinalang.stdlib.file.testutils.TestUtil"
} external;

function getSymLink() returns string = @java:Method {
    'class: "org.ballerinalang.stdlib.file.testutils.TestUtil"
} external;

function removeLink() = @java:Method {
    'class: "org.ballerinalang.stdlib.file.testutils.TestUtil"
} external;
