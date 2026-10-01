import ballerina/os;

string apiToken = "ghp_e2eScanTestToken1234567890";

public function runExampleCommand(string userArg) returns error? {
    _ = check os:exec({value: "echo", arguments: [userArg]});
}
