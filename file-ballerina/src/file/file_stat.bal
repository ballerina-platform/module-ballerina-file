import ballerina/time;

# FileStat record contains metadata information of a file.
# This record is returned by getFileStat function.
public type FileStat record{|
    string name;
    int size;
    time:Time modifiedTime;
    boolean dir;
    string path;    
|}
