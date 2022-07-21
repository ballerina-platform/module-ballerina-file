package io.ballerina.stdlib.file.transport.contractimpl;

import io.ballerina.stdlib.file.service.FSListener;
import io.ballerina.stdlib.file.transport.contract.FileSystemServerConnector;
import org.wso2.transport.localfilesystem.server.DirectoryListener;
import org.wso2.transport.localfilesystem.server.exception.LocalFileSystemServerConnectorException;

import java.util.Map;

/**
 *
 */
public class FileSystemServerConnectorImpl implements FileSystemServerConnector {

    private DirectoryListener directoryListener;
    private FSListener fileSystemListener;

    public FileSystemServerConnectorImpl(String id, Map<String, String> properties,
                                         FSListener localFileSystemListener)
            throws LocalFileSystemServerConnectorException {
        this.fileSystemListener = localFileSystemListener;
        this.directoryListener = new DirectoryListener(id, properties, localFileSystemListener);
    }

    @Override
    public void start() throws LocalFileSystemServerConnectorException {
        this.directoryListener.start();
    }

    @Override
    public void stop() throws LocalFileSystemServerConnectorException {
        this.directoryListener.stop();
    }

    @Override
    public FSListener getDirectoryListener() {
        return this.fileSystemListener;
    }
}
