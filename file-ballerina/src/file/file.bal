# Returns the current working directory.
public isolated function getWorkingDir() returns string {}

# Reports whether the file or directory exists in the given the path.
public isolated function isExist(string path) returns boolean {}

# Creates a new directory with the specified file name.
# If the `parentDirs` flag is true, it creates a directory in the specified path with any necessary parents.
public function createDir(string dir, boolean parentDirs = false) returns string|Error {}

# Removes the specified file or directory.
# If the recursive flag is true, it removes the path and any children it contains.
public function remove(string path, boolean recursive = false) returns Error? {}

# Renames(Moves) the old path with the new path.
public function rename(string oldPath, string newPath) returns Error? {}

# Returns the default directory to use for temporary files.
public isolated function getTempDir() returns string {}

# Creates a file in the specified file path.
# Truncates if the file already exists in the given path.
public function createFile(string path) returns string|Error {}

# Returns the metadata information of the file specified in the file path.
public isolated function getStat(string path) returns FileStat|Error {}

# Reads the directory and returns a list of files and directories 
# inside the specified directory.
public function readDir(string path, int maxDepth = -1) returns FileStat[]|Error {}

# Reads the directory and returns a stream of FileStat.
public function readDirAsStream(string path, int maxdepth=-1) returns stream<FileStat, Error>|Error{}

# Copy the file/directory in the old path to the new path.
# If a file already exists in the new path, this replaces that file.
public function copy(string sourcePath, string destinationPath,
                     boolean replaceExisting = false) returns Error? {}
