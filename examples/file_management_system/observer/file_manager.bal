// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/file;
import ballerina/email;
import ballerina/log;

configurable string directoryPath = ?;
configurable string host = ?;
configurable string username = ?;
configurable string password = ?;
configurable int port = ?;
configurable string toAddress = ?;

// Create the email client.
final email:SmtpClient smtpClient = check new (host, username , password, port = port);

listener file:Listener inFolder = new ({
    path: directoryPath
});

isolated service "fileObserver" on inFolder {

    isolated remote function onCreate(file:FileEvent m) {
        sendEmail("New File Created", "Created file path: " + m.name);
    }

    isolated remote function onModify(file:FileEvent m) {
        sendEmail("File Modified", "Modified file path: " + m.name);
    }

    isolated remote function onDelete(file:FileEvent m) {
        sendEmail("File DELETED", "Deleted file path: " + m.name);
    }
}

isolated function sendEmail(string subject, string body) {
    // Creates a message.
    email:Message email = {
        to: [toAddress],
        subject: subject,
        body: body
    };
    email:Error? sendMessage = smtpClient->sendMessage(email);
    if sendMessage is email:Error {
        log:printError("Failed to send the mail", 'error = sendMessage);
    } else {
        log:printInfo("The email has been sent", recipient = toAddress);
    }
}
