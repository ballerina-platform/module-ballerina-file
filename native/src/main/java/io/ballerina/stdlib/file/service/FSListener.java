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

package io.ballerina.stdlib.file.service;

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.file.utils.FileConstants;
import io.ballerina.stdlib.file.utils.ModuleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.localfilesystem.server.connector.contract.LocalFileSystemEvent;
import org.wso2.transport.localfilesystem.server.connector.contract.LocalFileSystemListener;

import java.util.HashMap;
import java.util.Map;

import static io.ballerina.stdlib.file.service.DirectoryListenerConstants.RESOURCE_NAME_ON_MESSAGE;

/**
 * File System connector listener for Ballerina.
 */
public class FSListener implements LocalFileSystemListener {

    private static final Logger log = LoggerFactory.getLogger(FSListener.class);
    private final Runtime runtime;
    private final BObject service;
    private final Map<String, MethodType> attachedFunctionRegistry;
    private static final StrandMetadata ON_MESSAGE_METADATA = new StrandMetadata(ModuleUtils.getModule().getOrg(),
            ModuleUtils.getModule().getName(), ModuleUtils.getModule().getVersion(), RESOURCE_NAME_ON_MESSAGE);

    public FSListener(Runtime runtime, BObject service, Map<String, MethodType> resourceRegistry) {
        this.runtime = runtime;
        this.service = service;
        this.attachedFunctionRegistry = resourceRegistry;
    }

    @Override
    public void onMessage(LocalFileSystemEvent fileEvent) {
        Map<String, Object> properties = getJvmSignatureParameters(fileEvent);
        MethodType resource = getMethodType(fileEvent.getEvent());
        if (resource != null) {
            String resourceName = resource.getName();
            if (service.getType().isIsolated()
                        && service.getType().isIsolated(resourceName)) {
                runtime.invokeMethodAsyncConcurrently(service, resourceName, null,
                        ON_MESSAGE_METADATA, new DirectoryCallback(), properties, null);
            } else {
                runtime.invokeMethodAsyncSequentially(service, resourceName, null,
                        ON_MESSAGE_METADATA, new DirectoryCallback(), properties, null);
            }
        } else {
            log.warn(String.format("FileEvent received for unregistered resource: [%s] %s", fileEvent.getEvent(),
                    fileEvent.getFileName()));
        }
    }

    private Map<String, Object> getJvmSignatureParameters(LocalFileSystemEvent fileEvent) {
        Map<String, Object> eventStruct = new HashMap<>();
        eventStruct.put(FileConstants.FILE_EVENT_NAME, fileEvent.getFileName());
        eventStruct.put(FileConstants.FILE_EVENT_OPERATION, fileEvent.getEvent());
        return eventStruct;
    }

    private MethodType getMethodType(String event) {
        return attachedFunctionRegistry.get(event);
    }
}
