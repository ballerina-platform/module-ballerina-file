package io.ballerina.stdlib.file.transport.contract;

import io.ballerina.stdlib.file.service.FSListener;
import org.wso2.transport.localfilesystem.server.exception.LocalFileSystemServerConnectorException;

/**
 *
 */
public interface FileSystemServerConnector {

    /**
     * Start the file watching operation.
     *
     * @throws LocalFileSystemServerConnectorException If unable to start the polling.
     */
    void start() throws LocalFileSystemServerConnectorException;

    /**
     * Stop the watching operation.
     *
     * @throws LocalFileSystemServerConnectorException If unable to stop the polling.
     */
    void stop() throws LocalFileSystemServerConnectorException;

    FSListener getDirectoryListener();
}
