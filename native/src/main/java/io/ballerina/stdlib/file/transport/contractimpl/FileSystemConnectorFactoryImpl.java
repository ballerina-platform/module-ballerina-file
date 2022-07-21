package io.ballerina.stdlib.file.transport.contractimpl;

import io.ballerina.stdlib.file.service.FSListener;
import io.ballerina.stdlib.file.transport.contract.FileSystemConnectorFactory;
import io.ballerina.stdlib.file.transport.contract.FileSystemServerConnector;
import org.wso2.transport.localfilesystem.server.exception.LocalFileSystemServerConnectorException;

import java.util.Map;

/**
 *
 */
public class FileSystemConnectorFactoryImpl implements FileSystemConnectorFactory {

    @Override
    public FileSystemServerConnector createServerConnector(String serviceId, Map<String, String> connectorConfig,
                                                           FSListener localFileSystemListener)
            throws LocalFileSystemServerConnectorException {
        return new FileSystemServerConnectorImpl(serviceId, connectorConfig, localFileSystemListener);
    }
}
