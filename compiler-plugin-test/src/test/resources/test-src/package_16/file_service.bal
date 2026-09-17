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

import ballerina/file;

public type Config record {|
    string afterProcess?;
|};

public annotation Config FunctionConfig on service remote function;

listener file:Listener localFolder = new ({
    path: "src/test/resources",
    recursive: false
});

service "one" on localFolder {

    @FunctionConfig {afterProcess: "archive"}
    remote function onCreate(file:FileEvent m) {
    }

    @FunctionConfig {afterProcess: "archive"}
    remote function onDelete(file:FileEvent m) {
    }
}

service "two" on localFolder {

    @FunctionConfig {afterProcess: "archive"}
    remote function onCreate(file:FileEvent m) {
    }
}
