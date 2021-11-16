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

import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.file.utils.FileConstants;
import io.ballerina.stdlib.file.utils.ModuleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.localfilesystem.server.connector.contract.LocalFileSystemEvent;
import org.wso2.transport.localfilesystem.server.connector.contract.LocalFileSystemListener;

import java.util.Map;

import static io.ballerina.stdlib.file.service.DirectoryListenerConstants.FILE_SYSTEM_EVENT;
import static io.ballerina.stdlib.file.service.DirectoryListenerConstants.RESOURCE_NAME_ON_MESSAGE;

/**
 * File System connector listener for Ballerina.
 */
public class FSListener implements LocalFileSystemListener {

    private static final Logger log = LoggerFactory.getLogger(FSListener.class);
    private Runtime runtime;
    private BObject service;
    private Map<String, MethodType> attachedFunctionRegistry;
    private static final StrandMetadata ON_MESSAGE_METADATA = new StrandMetadata(ModuleUtils.getModule().getOrg(),
            ModuleUtils.getModule().getName(), ModuleUtils.getModule().getVersion(), RESOURCE_NAME_ON_MESSAGE);

    public FSListener(Runtime runtime, BObject service, Map<String, MethodType> resourceRegistry) {
        this.runtime = runtime;
        this.service = service;
        this.attachedFunctionRegistry = resourceRegistry;
    }

    @Override
    public void onMessage(LocalFileSystemEvent fileEvent) {
        Object[] parameters = getJvmSignatureParameters(fileEvent);
        MethodType resource = getMethodType(fileEvent.getEvent());
        if (resource != null) {
            String resourceName = resource.getName();
            if (service.getType().isIsolated()
                    && service.getType().isIsolated(resourceName)) {
                runtime.invokeMethodAsyncConcurrently(service, resourceName, null,
                        ON_MESSAGE_METADATA, new DirectoryCallback(), null, PredefinedTypes.TYPE_NULL,
                        parameters);
            } else {
                runtime.invokeMethodAsyncSequentially(service, resourceName, null,
                        ON_MESSAGE_METADATA, new DirectoryCallback(), null, PredefinedTypes.TYPE_NULL,
                        parameters);
            }
        } else {
            log.warn(String.format("FileEvent received for unregistered resource: [%s] %s", fileEvent.getEvent(),
                    fileEvent.getFileName()));
        }
    }

    private Object[] getJvmSignatureParameters(LocalFileSystemEvent fileEvent) {
        BMap<BString, Object> eventStruct = ValueCreator.createRecordValue(ModuleUtils.getModule(), FILE_SYSTEM_EVENT);
        eventStruct.put(StringUtils.fromString(FileConstants.FILE_EVENT_NAME),
                StringUtils.fromString(fileEvent.getFileName()));
        eventStruct.put(StringUtils.fromString(FileConstants.FILE_EVENT_OPERATION),
                StringUtils.fromString(fileEvent.getEvent()));
        return new Object[] { eventStruct, true };
    }

    private MethodType getMethodType(String event) {
        return attachedFunctionRegistry.get(event);
    }
}
