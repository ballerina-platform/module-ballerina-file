package io.ballerina.stdlib.file.transport.contract;

import io.ballerina.stdlib.file.service.FSListener;
import org.wso2.transport.localfilesystem.server.exception.LocalFileSystemServerConnectorException;

import java.util.Map;

/**
 *
 */
public interface FileSystemConnectorFactory {

    FileSystemServerConnector createServerConnector(String serviceId, Map<String, String> connectorConfig,
                                                    FSListener localFileSystemListener)
            throws LocalFileSystemServerConnectorException;
}
