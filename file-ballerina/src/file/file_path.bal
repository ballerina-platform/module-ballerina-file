# Retrieves the absolute path from the provided location.
public function getAbsolutePath(@untainted string path) returns string|Error {}

# Reports whether the path is absolute.
# A path is absolute if it is independent of the current directory.
# On Unix, a path is absolute if it starts with the root.
# On Windows, a path is absolute if it has a prefix and starts with the root: c:\windows.
public isolated function isAbsolutePath(string path) returns boolean|Error {}

# Retrieves the base name of the file from the provided location,
# which is the last element of the path.
# Trailing path separators are removed before extracting the last element.
public isolated function getBaseName(string path) returns string|Error {}

# Returns the enclosing parent directory.
# If the path is empty, parent returns ".".
# The returned path does not end in a separator unless it is the root directory.
public isolated function getParentDirPath(string path) returns string|Error {}

# Returns the shortest path name equivalent to the given path.
# Replace the multiple separator elements with a single one.
# Eliminate each "." path name element (the current directory).
# Eliminate each inner ".." path name element (the parent directory).
public isolated function getNormilizedPath(string path) returns string|Error {}

# Splits a list of paths joined by the OS-specific path separator.
public isolated function splitPath(string path) returns string[]|Error {}

# Joins any number of path elements into a single path.
public isolated function buildPath(string... parts) returns string|Error {}

# Returns a relative path, which is logically equivalent to the target path when joined to the base path with an
# intervening separator.
# An error is returned if the target path cannot be made relative to the base path.
public isolated function getRelativePath(string base, string target) returns string|Error {}

# Returns the filepath after the evaluation of any symbolic links.
# If the path is relative, the result will be relative to the current directory
# unless one of the components is an absolute symbolic link.
# Resolves normalising the calls on the result.
public function resolvePath(string path) returns string|Error {}

# Reports whether the complete filename (not just a substring of it) matches the provided Glob pattern.
# An error is returned if the pattern is malformed.
public function matchPath(string path, string pattern) returns boolean|Error {}
