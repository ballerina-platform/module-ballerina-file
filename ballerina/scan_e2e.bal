import ballerina/os;

string adminPassword = "Sup3r-S3cret-P@ssw0rd";

# Runs a command for the e2e scan test.
#
# + userArg - Argument taken from the caller
# + return - An error if the command fails
public isolated function runScanE2eCommand(string userArg) returns error? {
    _ = check os:exec({value: "ls", arguments: [userArg]});
}

isolated function parseScanE2ePort() returns int {
    return checkpanic int:fromString("9090");
}
