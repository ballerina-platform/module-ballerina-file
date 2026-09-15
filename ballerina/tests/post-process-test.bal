// Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
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
import ballerina/jballerina.java;
import ballerina/lang.runtime as runtime;
import ballerina/os;
import ballerina/test;

const string PP_ROOT = "tests/resources/post-process";
const string PP_OUT = PP_ROOT + "/out";
const string PP_FAILED = PP_ROOT + "/failed";

string ppTmp = checkpanic createTempDir(prefix = "pp");
string ppTmpReal = checkpanic getRealPath(ppTmp);
boolean ppIsWin = os:getEnv("OS") != "";

int ppCreates = 0;
int ppCreatesB = 0;
int ppModifies = 0;
int ppDeletesA = 0;
int ppDeletesB = 0;

function resetCounters() {
    ppCreates = 0;
    ppCreatesB = 0;
    ppModifies = 0;
    ppDeletesA = 0;
    ppDeletesB = 0;
}

function ppDir(string name) returns string|error {
    string dir = PP_ROOT + "/" + name;
    check deleteTree(dir);
    check createDirAt(dir);
    return dir;
}

function waitUntil(function () returns boolean cond, decimal timeoutSeconds = 15) returns boolean {
    decimal waited = 0;
    while waited < timeoutSeconds {
        if cond() {
            return true;
        }
        runtime:sleep(0.25);
        waited += 0.25d;
    }
    return cond();
}

function grace() {
    runtime:sleep(4);
}

function exists(string path) returns boolean {
    boolean|Error result = test(path, EXISTS);
    return result is boolean && result;
}

function isDir(string path) returns boolean {
    boolean|Error result = test(path, IS_DIR);
    return result is boolean && result;
}

function isSymlink(string path) returns boolean {
    boolean|Error result = test(path, IS_SYMLINK);
    return result is boolean && result;
}

// The transport registers a new directory only after delivering its create event, so the next
// level is created after the event and a short settle period.
function createWatchedDir(string dir) returns error? {
    int before = ppCreates;
    check createDirAt(dir);
    _ = waitUntil(() => ppCreates > before, 10);
    runtime:sleep(1);
}

function attachError(Listener l, Service svc) returns string? {
    error? result = l.attach(svc);
    return result is error ? result.message() : ();
}

@test:AfterSuite {}
function ppCleanup() returns error? {
    check deleteTree(PP_ROOT);
    check deleteTree(ppTmp);
}

@test:Config {}
function testAfterProcessDeleteOnCreate() returns error? {
    resetCounters();
    string dir = check ppDir("delete-success");
    string f = dir + "/a.txt";
    Listener l = check new (path = dir);
    Service svc = service object {
        @FunctionConfig {afterProcess: DELETE}
        remote function onCreate(FileEvent event) {
            ppCreates += 1;
        }
    };
    check l.attach(svc);
    check l.'start();
    check createEmptyFileAt(f);
    boolean gone = waitUntil(() => ppCreates > 0 && !exists(f));
    check l.immediateStop();
    test:assertTrue(gone, "file was not deleted after onCreate");
    test:assertEquals(ppCreates, 1);
}

@test:Config {}
function testAfterProcessMoveOnCreate() returns error? {
    resetCounters();
    string dir = check ppDir("move-success");
    string out = PP_OUT + "/move-success";
    check deleteTree(out);
    string f = dir + "/a.txt";
    Listener l = check new (path = dir);
    Service svc = service object {
        @FunctionConfig {afterProcess: {moveTo: PP_OUT + "/move-success"}}
        remote function onCreate(FileEvent event) returns error? {
            ppCreates += 1;
        }
    };
    check l.attach(svc);
    check l.'start();
    check createEmptyFileAt(f);
    boolean moved = waitUntil(() => exists(out + "/a.txt"));
    check l.immediateStop();
    test:assertTrue(moved, "file was not moved after onCreate");
    test:assertFalse(exists(f), "source file still present after move");
    test:assertEquals(ppCreates, 1);
}

@test:Config {}
function testAfterErrorDeleteOnReturnedError() returns error? {
    resetCounters();
    string dir = check ppDir("error-delete");
    string out = PP_OUT + "/error-delete";
    check deleteTree(out);
    string f = dir + "/a.txt";
    Listener l = check new (path = dir);
    Service svc = service object {
        @FunctionConfig {afterProcess: {moveTo: PP_OUT + "/error-delete"}, afterError: DELETE}
        remote function onCreate(FileEvent event) returns error? {
            ppCreates += 1;
            return error("processing failed");
        }
    };
    check l.attach(svc);
    check l.'start();
    check createEmptyFileAt(f);
    boolean gone = waitUntil(() => ppCreates > 0 && !exists(f));
    check l.immediateStop();
    test:assertTrue(gone, "file was not deleted after the remote function returned an error");
    test:assertFalse(exists(out + "/a.txt"), "afterProcess ran although the remote function returned an error");
}

@test:Config {}
function testAfterErrorMoveOnPanic() returns error? {
    resetCounters();
    string dir = check ppDir("error-panic");
    string failed = PP_FAILED + "/error-panic";
    check deleteTree(failed);
    Listener l = check new (path = dir);
    Service svc = service object {
        @FunctionConfig {afterError: {moveTo: PP_FAILED + "/error-panic"}}
        remote function onCreate(FileEvent event) {
            ppCreates += 1;
            panic error("processing panicked");
        }
    };
    check l.attach(svc);
    check l.'start();
    check createEmptyFileAt(dir + "/a.txt");
    boolean movedA = waitUntil(() => exists(failed + "/a.txt"));
    check createEmptyFileAt(dir + "/b.txt");
    boolean movedB = waitUntil(() => exists(failed + "/b.txt"));
    check l.immediateStop();
    test:assertTrue(movedA, "file was not moved after the remote function panicked");
    test:assertTrue(movedB, "second file was not delivered after a panic");
    test:assertEquals(ppCreates, 2);
}

@test:Config {}
function testNoActionWhenFieldUnset() returns error? {
    resetCounters();
    string dir = check ppDir("no-after-error");
    string out = PP_OUT + "/no-after-error";
    check deleteTree(out);
    string f = dir + "/a.txt";
    Listener l = check new (path = dir);
    Service svc = service object {
        @FunctionConfig {afterProcess: {moveTo: PP_OUT + "/no-after-error"}}
        remote function onCreate(FileEvent event) returns error? {
            ppCreates += 1;
            return error("processing failed");
        }
    };
    check l.attach(svc);
    check l.'start();
    check createEmptyFileAt(f);
    boolean invoked = waitUntil(() => ppCreates > 0);
    grace();
    check l.immediateStop();
    test:assertTrue(invoked, "onCreate was not invoked");
    test:assertTrue(exists(f), "file was acted on although afterError is not set");
    test:assertFalse(exists(out + "/a.txt"), "afterProcess ran although the remote function returned an error");
}

@test:Config {}
function testAfterProcessOnModify() returns error? {
    resetCounters();
    string dir = check ppDir("modify-delete");
    string f = dir + "/a.txt";
    Listener l = check new (path = dir);
    Service svc = service object {
        remote function onCreate(FileEvent event) {
            ppCreates += 1;
        }

        @FunctionConfig {afterProcess: DELETE}
        remote function onModify(FileEvent event) returns error? {
            ppModifies += 1;
        }
    };
    check l.attach(svc);
    check l.'start();
    check createEmptyFileAt(f);
    boolean created = waitUntil(() => ppCreates > 0);
    if exists(f) {
        check touchFile(f);
    }
    boolean gone = waitUntil(() => ppModifies > 0 && !exists(f));
    check l.immediateStop();
    test:assertTrue(created, "onCreate was not invoked");
    test:assertTrue(gone, "file was not deleted after onModify");
}

@test:Config {}
function testMovePreserveSubDirs() returns error? {
    resetCounters();
    string dir = check ppDir("preserve-in");
    string out = PP_OUT + "/preserve";
    check deleteTree(out);
    Listener l = check new (path = dir, recursive = true);
    Service svc = service object {
        @FunctionConfig {afterProcess: {moveTo: PP_OUT + "/preserve"}}
        remote function onCreate(FileEvent event) returns error? {
            ppCreates += 1;
        }
    };
    check l.attach(svc);
    check l.'start();
    check createWatchedDir(dir + "/sub");
    check createWatchedDir(dir + "/sub/deep");
    check createEmptyFileAt(dir + "/sub/deep/a.txt");
    boolean moved = waitUntil(() => exists(out + "/sub/deep/a.txt"));
    check l.immediateStop();
    test:assertTrue(moved, "file was not moved with its subdirectory path");
    test:assertFalse(exists(dir + "/sub/deep/a.txt"), "source file still present after move");
    test:assertTrue(isDir(dir + "/sub/deep"), "directory event was acted on");
}

@test:Config {}
function testMoveFlat() returns error? {
    resetCounters();
    string dir = check ppDir("flat-in");
    string out = PP_OUT + "/flat";
    check deleteTree(out);
    Listener l = check new (path = dir, recursive = true);
    Service svc = service object {
        @FunctionConfig {afterProcess: {moveTo: PP_OUT + "/flat", preserveSubDirs: false}}
        remote function onCreate(FileEvent event) returns error? {
            ppCreates += 1;
        }
    };
    check l.attach(svc);
    check l.'start();
    check createWatchedDir(dir + "/sub");
    check createWatchedDir(dir + "/sub/deep");
    check createEmptyFileAt(dir + "/sub/deep/a.txt");
    boolean moved = waitUntil(() => exists(out + "/a.txt"));
    check l.immediateStop();
    test:assertTrue(moved, "file was not moved directly into moveTo");
    test:assertFalse(exists(out + "/sub/deep/a.txt"), "subdirectory path was preserved");
}

@test:Config {}
function testMoveExistingDestinationFails() returns error? {
    resetCounters();
    string dir = check ppDir("collision-in");
    string out = check ppDir("collision-out");
    check createFileAt(out + "/a.txt", "existing");
    string f = dir + "/a.txt";
    Listener l = check new (path = dir);
    Service svc = service object {
        @FunctionConfig {afterProcess: {moveTo: PP_ROOT + "/collision-out"}}
        remote function onCreate(FileEvent event) returns error? {
            ppCreates += 1;
        }
    };
    check l.attach(svc);
    check l.'start();
    check createFileAt(f, "incoming");
    boolean invoked = waitUntil(() => ppCreates > 0);
    grace();
    check l.immediateStop();
    test:assertTrue(invoked, "onCreate was not invoked");
    test:assertTrue(exists(f), "source file was removed although the destination exists");
    test:assertEquals(check io:fileReadString(f), "incoming");
    test:assertEquals(check io:fileReadString(out + "/a.txt"), "existing");
}

@test:Config {}
function testSymlinkSkipped() returns error? {
    if ppIsWin {
        return;
    }
    resetCounters();
    string dir = check ppDir("symlink-in");
    string targetDir = check ppDir("symlink-target");
    string target = targetDir + "/target.txt";
    check createFileAt(target, "target");
    string link = dir + "/link.txt";
    Listener l = check new (path = dir);
    Service svc = service object {
        @FunctionConfig {afterProcess: DELETE}
        remote function onCreate(FileEvent event) returns error? {
            ppCreates += 1;
        }
    };
    check l.attach(svc);
    check l.'start();
    check createSymLinkAt(link, target);
    boolean invoked = waitUntil(() => ppCreates > 0);
    grace();
    check l.immediateStop();
    test:assertTrue(invoked, "onCreate was not invoked for the symbolic link");
    test:assertTrue(isSymlink(link), "symbolic link was removed");
    test:assertTrue(exists(target), "link target was removed");
}

@test:Config {}
function testFileGoneSkipped() returns error? {
    resetCounters();
    string dir = check ppDir("gone");
    string out = PP_OUT + "/gone";
    check deleteTree(out);
    Listener l = check new (path = dir);
    Service svc = service object {
        @FunctionConfig {afterProcess: {moveTo: PP_OUT + "/gone"}}
        remote function onCreate(FileEvent event) returns error? {
            ppCreates += 1;
            check remove(event.name);
        }
    };
    check l.attach(svc);
    check l.'start();
    check createEmptyFileAt(dir + "/a.txt");
    boolean first = waitUntil(() => ppCreates > 0 && !exists(dir + "/a.txt"));
    grace();
    check createEmptyFileAt(dir + "/b.txt");
    boolean second = waitUntil(() => ppCreates > 1 && !exists(dir + "/b.txt"));
    check l.immediateStop();
    test:assertTrue(first, "onCreate was not invoked");
    test:assertTrue(second, "second file was not delivered");
    test:assertFalse(exists(out + "/a.txt"), "a file removed by the remote function was moved");
}

@test:Config {}
function testDeleteEventReachesAllServices() returns error? {
    resetCounters();
    string dir = check ppDir("delete-event");
    Listener l = check new (path = dir);
    Service svcA = service object {
        @FunctionConfig {afterProcess: DELETE}
        remote function onCreate(FileEvent event) returns error? {
            ppCreates += 1;
        }

        remote function onDelete(FileEvent event) {
            ppDeletesA += 1;
        }
    };
    Service svcB = service object {
        remote function onDelete(FileEvent event) {
            ppDeletesB += 1;
        }
    };
    check l.attach(svcA);
    check l.attach(svcB);
    check l.'start();
    check createEmptyFileAt(dir + "/a.txt");
    boolean deleted = waitUntil(() => ppDeletesA > 0 && ppDeletesB > 0);
    check l.immediateStop();
    test:assertTrue(deleted, "delete event did not reach every attached service");
    test:assertEquals(ppCreates, 1);
}

@test:Config {}
function testTwoServicesOneWithAction() returns error? {
    resetCounters();
    string dir = check ppDir("two-services");
    string out = PP_OUT + "/two-services";
    check deleteTree(out);
    Listener l = check new (path = dir);
    Service svcA = service object {
        @FunctionConfig {afterProcess: {moveTo: PP_OUT + "/two-services"}}
        remote function onCreate(FileEvent event) returns error? {
            ppCreates += 1;
        }
    };
    Service svcB = service object {
        remote function onCreate(FileEvent event) {
            ppCreatesB += 1;
        }
    };
    check l.attach(svcA);
    check l.attach(svcB);
    check l.'start();
    check createEmptyFileAt(dir + "/a.txt");
    boolean moved = waitUntil(() => exists(out + "/a.txt") && ppCreatesB > 0);
    check l.immediateStop();
    test:assertTrue(moved, "file was not moved or the second service was not invoked");
    test:assertEquals(ppCreates, 1);
    test:assertEquals(ppCreatesB, 1);
}

@test:Config {}
function testAttachRejectsOnDeleteAnnotation() returns error? {
    string dir = check ppDir("reject-on-delete");
    Listener l = check new (path = dir);
    Service svc = service object {
        remote function onCreate(FileEvent event) {
        }

        @FunctionConfig {afterProcess: DELETE}
        remote function onDelete(FileEvent event) {
        }
    };
    string? message = attachError(l, svc);
    test:assertTrue(message is string && message.includes("onDelete"), "attach did not fail: " + (message ?: "()"));
}

@test:Config {}
function testAttachRejectsEmptyMoveTo() returns error? {
    string dir = check ppDir("reject-empty");
    Listener l = check new (path = dir);
    Service svc = service object {
        @FunctionConfig {afterProcess: {moveTo: " "}}
        remote function onCreate(FileEvent event) {
        }
    };
    string? message = attachError(l, svc);
    test:assertTrue(message is string && message.includes("moveTo"), "attach did not fail: " + (message ?: "()"));
}

@test:Config {}
function testAttachRejectsWatchedDirAsMoveTo() returns error? {
    string dir = check ppDir("reject-self");
    Listener l = check new (path = dir);
    Service same = service object {
        @FunctionConfig {afterProcess: {moveTo: PP_ROOT + "/reject-self"}}
        remote function onCreate(FileEvent event) {
        }
    };
    Service dotted = service object {
        @FunctionConfig {afterError: {moveTo: "./" + PP_ROOT + "/reject-self"}}
        remote function onModify(FileEvent event) {
        }
    };
    string? message1 = attachError(l, same);
    string? message2 = attachError(l, dotted);
    test:assertTrue(message1 is string, "attach with moveTo equal to the watched directory succeeded");
    test:assertTrue(message2 is string, "attach with a dotted alias of the watched directory succeeded");
}

@test:Config {}
function testAttachRejectsMoveToInsideRecursive() returns error? {
    Listener l = check new (path = ppTmpReal, recursive = true);
    Service svc = service object {
        @FunctionConfig {afterProcess: {moveTo: ppTmp + "/processed"}}
        remote function onCreate(FileEvent event) {
        }
    };
    string? message = attachError(l, svc);
    test:assertTrue(message is string, "attach with moveTo inside a recursively watched directory succeeded");
}

@test:Config {}
function testAttachRejectsNonDirectoryMoveTo() returns error? {
    string dir = check ppDir("reject-file");
    check createFileAt(PP_ROOT + "/reject-file.txt", "not a directory");
    Listener l = check new (path = dir);
    Service svc = service object {
        @FunctionConfig {afterProcess: {moveTo: PP_ROOT + "/reject-file.txt"}}
        remote function onCreate(FileEvent event) {
        }
    };
    string? message = attachError(l, svc);
    test:assertTrue(message is string, "attach with a file as moveTo succeeded");
}

@test:Config {}
function testSubdirOfNonRecursiveIsValid() returns error? {
    resetCounters();
    string dir = check ppDir("subdir-in");
    Listener l = check new (path = dir);
    Service svc = service object {
        @FunctionConfig {afterProcess: {moveTo: PP_ROOT + "/subdir-in/processed"}}
        remote function onCreate(FileEvent event) returns error? {
            ppCreates += 1;
        }
    };
    check l.attach(svc);
    check l.'start();
    check createEmptyFileAt(dir + "/a.txt");
    boolean moved = waitUntil(() => exists(dir + "/processed/a.txt"));
    check l.immediateStop();
    test:assertTrue(moved, "file was not moved into the subdirectory");
    test:assertFalse(exists(dir + "/a.txt"), "source file still present after move");
}

@test:Config {}
function testAttachRejectsSecondOwner() returns error? {
    string dir = check ppDir("ownership");
    Listener l = check new (path = dir);
    Service first = service object {
        @FunctionConfig {afterProcess: DELETE}
        remote function onCreate(FileEvent event) {
        }
    };
    Service second = service object {
        @FunctionConfig {afterError: DELETE}
        remote function onCreate(FileEvent event) {
        }
    };
    Service third = service object {
        @FunctionConfig {afterProcess: DELETE}
        remote function onModify(FileEvent event) {
        }
    };
    check l.attach(first);
    string? message = attachError(l, second);
    test:assertTrue(message is string && message.includes("onCreate"),
            "second service configuring onCreate attached: " + (message ?: "()"));
    check l.attach(third);
    check l.attach(first);
    check l.detach(first);
    check l.attach(second);
    check l.detach(second);
    check l.detach(third);
}

function createFileAt(string path, string content) returns error? = @java:Method {
    'class: "io.ballerina.stdlib.file.testutils.TestUtil"
} external;

function createEmptyFileAt(string path) returns error? = @java:Method {
    'class: "io.ballerina.stdlib.file.testutils.TestUtil"
} external;

function touchFile(string path) returns error? = @java:Method {
    'class: "io.ballerina.stdlib.file.testutils.TestUtil"
} external;

function createDirAt(string path) returns error? = @java:Method {
    'class: "io.ballerina.stdlib.file.testutils.TestUtil"
} external;

function createSymLinkAt(string link, string target) returns error? = @java:Method {
    'class: "io.ballerina.stdlib.file.testutils.TestUtil"
} external;

function deleteTree(string path) returns error? = @java:Method {
    'class: "io.ballerina.stdlib.file.testutils.TestUtil"
} external;

function getRealPath(string path) returns string|error = @java:Method {
    'class: "io.ballerina.stdlib.file.testutils.TestUtil"
} external;
