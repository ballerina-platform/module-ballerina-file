/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.file.transport.contractimpl;

import io.ballerina.stdlib.file.service.FSListener;
import io.ballerina.stdlib.file.transport.contract.FileSystemServerConnector;
import org.wso2.transport.localfilesystem.server.DirectoryListener;
import org.wso2.transport.localfilesystem.server.exception.LocalFileSystemServerConnectorException;

import java.util.Map;

/**
 * Implementation of the LocalFileSystemServerConnector interface.
 *
 * @since 1.3.1
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
