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

# Delete action for file processing. When specified, the file is deleted after processing.
public const DELETE = "DELETE";

# Configuration for moving a file after processing.
public type Move record {|
    # Destination directory the file is moved into
    string moveTo;
    # If `true`, keeps the subdirectory path relative to the listener's `path` under `moveTo`
    boolean preserveSubDirs = true;
|};

# Type alias for the `Move` record, used in the action unions.
public type MOVE Move;

# Configuration for the remote functions of a file service.
public type FunctionConfiguration record {|
    # Action to perform after the remote function returns successfully. Can be `DELETE` or `MOVE`.
    # If not specified, no action is taken and the file remains in place
    MOVE|DELETE afterProcess?;
    # Action to perform after the remote function returns an error or panics. Can be `DELETE` or `MOVE`.
    # If not specified, no action is taken and the file remains in place
    MOVE|DELETE afterError?;
|};

# Annotation to configure the remote functions of a file service.
public annotation FunctionConfiguration FunctionConfig on service remote function;
