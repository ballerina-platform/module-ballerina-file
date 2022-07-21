/*
 * Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.file.service.endpoint;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.file.service.DirectoryListenerConstants;
import io.ballerina.stdlib.file.service.FSListener;
import io.ballerina.stdlib.file.transport.contract.FileSystemConnectorFactory;
import io.ballerina.stdlib.file.transport.contract.FileSystemServerConnector;
import io.ballerina.stdlib.file.transport.contractimpl.FileSystemConnectorFactoryImpl;
import io.ballerina.stdlib.file.utils.FileConstants;
import io.ballerina.stdlib.file.utils.FileUtils;
import org.wso2.transport.localfilesystem.server.exception.LocalFileSystemServerConnectorException;
import org.wso2.transport.localfilesystem.server.util.Constants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Initialize endpoints.
 */
public class InitEndpoint {

    public static Object initEndpoint(Environment env, BObject listener) {
        BMap serviceEndpointConfig = listener.getMapValue(DirectoryListenerConstants.SERVICE_ENDPOINT_CONFIG);
        String path = serviceEndpointConfig.getStringValue(DirectoryListenerConstants.ANNOTATION_PATH).getValue();
        if (path.isEmpty()) {
            return FileUtils.getBallerinaError(FileConstants.FILE_SYSTEM_ERROR, "'path' field is empty");
        }
        final Path dirPath = Paths.get(path);
        if (Files.notExists(dirPath)) {
            return FileUtils.getBallerinaError(FileConstants.FILE_SYSTEM_ERROR, "Folder does not exist: " + path);
        }
        if (!Files.isDirectory(dirPath)) {
            return FileUtils.getBallerinaError(FileConstants.FILE_SYSTEM_ERROR, "Unable to find a directory: " + path);
        }
        FileSystemConnectorFactory connectorFactory = new FileSystemConnectorFactoryImpl();

        final Map<String, String> paramMap = getParamMap(serviceEndpointConfig);
        FileSystemServerConnector serverConnector = null;
        try {
            serverConnector = connectorFactory.createServerConnector(listener.getType().getName(), paramMap,
                    new FSListener(env.getRuntime()));
            listener.addNativeData(DirectoryListenerConstants.FS_SERVER_CONNECTOR, serverConnector);
        } catch (LocalFileSystemServerConnectorException e) {
            return FileUtils.getBallerinaError(FileConstants.FILE_SYSTEM_ERROR,
                    "Unable to initialize server connector: " + e.getMessage());
        }
        return null;
    }

    private static Map<String, String> getParamMap(BMap serviceEndpointConfig) {
        final String path = serviceEndpointConfig.getStringValue(DirectoryListenerConstants.ANNOTATION_PATH).getValue();
        final boolean recursive = serviceEndpointConfig
                .getBooleanValue(DirectoryListenerConstants.ANNOTATION_DIRECTORY_RECURSIVE);
        Map<String, String> paramMap = new HashMap<>(3);
        paramMap.put(Constants.FILE_URI, path);
        paramMap.put(Constants.DIRECTORY_WATCH_EVENTS,
                Constants.EVENT_CREATE + "," + Constants.EVENT_MODIFY + "," + Constants.EVENT_DELETE);
        paramMap.put(Constants.DIRECTORY_WATCH_RECURSIVE, String.valueOf(recursive));
        return paramMap;
    }

    private InitEndpoint() {}
}
