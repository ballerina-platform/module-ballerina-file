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
import io.ballerina.stdlib.file.observability.FileMetricsUtil;
import io.ballerina.stdlib.file.observability.FileObserverContext;
import io.ballerina.stdlib.file.observability.FileTracingUtil;
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
    private final String watchedPath;
    private final Map<BObject, Map<String, MethodType>> serviceRegistry = new ConcurrentHashMap<>();
    private final Map<String, OwnedActions> actions = new ConcurrentHashMap<>();

    public FSListener(Runtime runtime, Path watchRoot, boolean recursive) {
        this.runtime = runtime;
        this.watchRoot = watchRoot;
        this.recursive = recursive;
        this.watchedPath = watchRoot.toString();
    }

    @Override
    public void onMessage(LocalFileSystemEvent fileEvent) {
        Thread.startVirtualThread(() -> dispatch(fileEvent));
    }

    private void dispatch(LocalFileSystemEvent fileEvent) {
        String filePath = fileEvent.getFileName();
        String eventType = mapEventType(fileEvent.getEvent());
        String event = fileEvent.getEvent();

        boolean hasHandler = serviceRegistry.values().stream()
                .anyMatch(methods -> methods.containsKey(event));

        if (hasHandler) {
            FileMetricsUtil.reportFileStage(watchedPath, FileMetricsUtil.FILE_STAGE_FOUND,
                    null, null, null);
        } else {
            FileMetricsUtil.reportFileStage(watchedPath, FileMetricsUtil.FILE_STAGE_FOUND,
                    FileMetricsUtil.OUTCOME_SKIPPED, FileMetricsUtil.FAILURE_NO_HANDLER_MATCHED, null);
            return;
        }

        FileObserverContext lifecycleCtx = FileTracingUtil.createFileLifecycleContext(
                watchedPath, filePath);

        Object balFileEvent = createBallerinaFileEvent(fileEvent);
        OwnedActions owned = actions.get(event);
        Boolean ownerSucceeded = invokeServices(event, eventType, balFileEvent, owned, lifecycleCtx,
                filePath);

        FileTracingUtil.finishFileLifecycleSpan(lifecycleCtx);

        if (owned != null && ownerSucceeded != null) {
            runOwnedAction(owned, ownerSucceeded, filePath);
        }
    }

    private Boolean invokeServices(String event, String eventType, Object balFileEvent,
                                   OwnedActions owned, FileObserverContext lifecycleCtx,
                                   String filePath) {
        Boolean ownerSucceeded = null;
        for (Map.Entry<BObject, Map<String, MethodType>> serviceEntry : serviceRegistry.entrySet()) {
            MethodType serviceFunction = serviceEntry.getValue().get(event);
            if (serviceFunction == null) {
                continue;
            }
            BObject service = serviceEntry.getKey();
            String functionName = serviceFunction.getName();
            boolean succeeded = invokeRemoteFunction(service, functionName, eventType, balFileEvent,
                    lifecycleCtx, filePath);
            if (owned != null && owned.owner == service) {
                ownerSucceeded = succeeded;
            }
        }
        return ownerSucceeded;
    }

    private void runOwnedAction(OwnedActions owned, boolean ownerSucceeded, String filePath) {
        PostProcessAction action = ownerSucceeded ? owned.afterProcess : owned.afterError;
        if (action != null) {
            PostProcessor.executePostProcessAction(action, filePath, watchRoot, recursive,
                    ownerSucceeded ? ANNOTATION_AFTER_PROCESS : ANNOTATION_AFTER_ERROR, owned.methodName);
        }
    }

    private boolean invokeRemoteFunction(BObject service, String functionName, String eventType,
                                         Object balFileEvent, FileObserverContext lifecycleCtx,
                                         String filePath) {
        FileMetricsUtil.reportFileStage(watchedPath, FileMetricsUtil.FILE_STAGE_DISPATCHED,
                null, null, functionName);
        Map<String, Object> properties = FileTracingUtil.createStrandProperties(
                watchedPath, eventType, functionName);
        FileTracingUtil.setParentContext(properties, lifecycleCtx);
        FileTracingUtil.addFileMetadataToStrandProperties(properties, filePath);

        ObjectType type = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service));
        boolean isConcurrentSafe = type.isIsolated() && type.isIsolated(functionName);
        long startTime = System.nanoTime();
        try {
            Object result = runtime.callMethod(service, functionName,
                    new StrandMetadata(isConcurrentSafe, properties), balFileEvent);
            double durationSecs = (System.nanoTime() - startTime) / 1_000_000_000.0;

            String outcome = (result instanceof BError)
                    ? FileMetricsUtil.OUTCOME_FAILURE : FileMetricsUtil.OUTCOME_SUCCESS;
            String errorType = (result instanceof BError)
                    ? ((BError) result).getType().getName() : null;

            FileMetricsUtil.reportFileStage(watchedPath, FileMetricsUtil.FILE_STAGE_HANDLED,
                    outcome, errorType, functionName);
            FileMetricsUtil.reportResourceExecutionDuration(watchedPath, functionName,
                    outcome, durationSecs);

            if (result instanceof BError bError) {
                bError.printStackTrace();
                return false;
            }
            return true;
        } catch (BError bError) {
            double durationSecs = (System.nanoTime() - startTime) / 1_000_000_000.0;
            FileMetricsUtil.reportHandledFailure(watchedPath, bError.getType().getName(),
                    functionName, durationSecs);
            bError.printStackTrace();
            return false;
        } catch (RuntimeException e) {
            double durationSecs = (System.nanoTime() - startTime) / 1_000_000_000.0;
            FileMetricsUtil.reportHandledFailure(watchedPath, e.getClass().getSimpleName(),
                    functionName, durationSecs);
            FileUtils.getBallerinaError(FileConstants.FILE_SYSTEM_ERROR, "Error invoking remote function "
                    + functionName + ": " + e.getMessage()).printStackTrace();
            return false;
        }
    }

    private String mapEventType(String event) {
        return switch (event) {
            case DirectoryListenerConstants.EVENT_CREATE -> FileMetricsUtil.EVENT_TYPE_CREATE;
            case DirectoryListenerConstants.EVENT_DELETE -> FileMetricsUtil.EVENT_TYPE_DELETE;
            case DirectoryListenerConstants.EVENT_MODIFY -> FileMetricsUtil.EVENT_TYPE_MODIFY;
            default -> FileMetricsUtil.NONE;
        };
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
