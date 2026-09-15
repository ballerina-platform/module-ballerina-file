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
import io.ballerina.runtime.api.concurrent.StrandMetadata;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.file.utils.FileConstants;
import io.ballerina.stdlib.file.utils.FileUtils;
import io.ballerina.stdlib.file.utils.ModuleUtils;
import org.wso2.transport.localfilesystem.server.connector.contract.LocalFileSystemEvent;
import org.wso2.transport.localfilesystem.server.connector.contract.LocalFileSystemListener;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.ballerina.stdlib.file.service.DirectoryListenerConstants.ANNOTATION_AFTER_ERROR;
import static io.ballerina.stdlib.file.service.DirectoryListenerConstants.ANNOTATION_AFTER_PROCESS;
import static io.ballerina.stdlib.file.service.DirectoryListenerConstants.FILE_SYSTEM_EVENT;

/**
 * File System connector listener for Ballerina.
 */
public class FSListener implements LocalFileSystemListener {

    private final Runtime runtime;
    private final Path watchRoot;
    private final boolean recursive;
    private final Map<BObject, Map<String, MethodType>> serviceRegistry = new ConcurrentHashMap<>();
    private final Map<String, OwnedActions> actions = new ConcurrentHashMap<>();

    public FSListener(Runtime runtime, Path watchRoot, boolean recursive) {
        this.runtime = runtime;
        this.watchRoot = watchRoot;
        this.recursive = recursive;
    }

    @Override
    public void onMessage(LocalFileSystemEvent fileEvent) {
        Thread.startVirtualThread(() -> {
            Object balFileEvent = createBallerinaFileEvent(fileEvent);
            String event = fileEvent.getEvent();
            OwnedActions owned = actions.get(event);
            Boolean ownerSucceeded = null;
            for (Map.Entry<BObject, Map<String, MethodType>> serviceEntry : serviceRegistry.entrySet()) {
                MethodType serviceFunction = serviceEntry.getValue().get(event);
                if (serviceFunction == null) {
                    continue;
                }
                BObject service = serviceEntry.getKey();
                boolean succeeded = invokeRemoteFunction(service, serviceFunction.getName(), balFileEvent);
                if (owned != null && owned.owner == service) {
                    ownerSucceeded = succeeded;
                }
            }
            if (owned == null || ownerSucceeded == null) {
                return;
            }
            PostProcessAction action = ownerSucceeded ? owned.afterProcess : owned.afterError;
            if (action != null) {
                PostProcessor.executePostProcessAction(action, fileEvent.getFileName(), watchRoot, recursive,
                        ownerSucceeded ? ANNOTATION_AFTER_PROCESS : ANNOTATION_AFTER_ERROR, owned.methodName);
            }
        });
    }

    private boolean invokeRemoteFunction(BObject service, String functionName, Object balFileEvent) {
        try {
            ObjectType type = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service));
            boolean isConcurrentSafe = type.isIsolated() && type.isIsolated(functionName);
            Object result = runtime.callMethod(service, functionName, new StrandMetadata(isConcurrentSafe, null),
                    balFileEvent);
            if (result instanceof BError bError) {
                bError.printStackTrace();
                return false;
            }
            return true;
        } catch (BError bError) {
            bError.printStackTrace();
            return false;
        } catch (RuntimeException e) {
            FileUtils.getBallerinaError(FileConstants.FILE_SYSTEM_ERROR, "Error invoking remote function "
                    + functionName + ": " + e.getMessage()).printStackTrace();
            return false;
        }
    }

    private Object createBallerinaFileEvent(LocalFileSystemEvent fileEvent) {
        BMap<BString, Object> eventStruct = ValueCreator.createRecordValue(ModuleUtils.getModule(), FILE_SYSTEM_EVENT);
        eventStruct.put(StringUtils.fromString(FileConstants.FILE_EVENT_NAME),
                StringUtils.fromString(fileEvent.getFileName()));
        eventStruct.put(StringUtils.fromString(FileConstants.FILE_EVENT_OPERATION),
                StringUtils.fromString(fileEvent.getEvent()));
        return eventStruct;
    }

    /**
     * Registers a service and claims the post-processing actions it declares.
     *
     * @param service           The service object
     * @param attachedFunctions The remote functions by event name
     * @param config            The post-processing actions declared by the service
     * @return The name of a remote function whose action is already owned by another service, if any
     */
    public synchronized Optional<String> addService(BObject service, Map<String, MethodType> attachedFunctions,
                                                    PostProcessConfig config) {
        for (Map.Entry<String, MethodType> entry : attachedFunctions.entrySet()) {
            String methodName = entry.getValue().getName();
            OwnedActions existing = actions.get(entry.getKey());
            if (config.hasPostProcessingActions(methodName) && existing != null && existing.owner != service) {
                return Optional.of(methodName);
            }
        }
        actions.values().removeIf(owned -> owned.owner == service);
        for (Map.Entry<String, MethodType> entry : attachedFunctions.entrySet()) {
            String methodName = entry.getValue().getName();
            if (config.hasPostProcessingActions(methodName)) {
                actions.put(entry.getKey(), new OwnedActions(service, methodName,
                        config.getAfterProcessAction(methodName).orElse(null),
                        config.getAfterErrorAction(methodName).orElse(null)));
            }
        }
        this.serviceRegistry.put(service, attachedFunctions);
        return Optional.empty();
    }

    public synchronized void removeService(BObject service) {
        this.serviceRegistry.remove(service);
        actions.values().removeIf(owned -> owned.owner == service);
    }

    private static final class OwnedActions {
        private final BObject owner;
        private final String methodName;
        private final PostProcessAction afterProcess;
        private final PostProcessAction afterError;

        private OwnedActions(BObject owner, String methodName, PostProcessAction afterProcess,
                             PostProcessAction afterError) {
            this.owner = owner;
            this.methodName = methodName;
            this.afterProcess = afterProcess;
            this.afterError = afterError;
        }
    }
}
