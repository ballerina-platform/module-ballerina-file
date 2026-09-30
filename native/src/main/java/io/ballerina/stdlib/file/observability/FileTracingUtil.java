/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com)
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.file.observability;

import io.ballerina.runtime.observability.ObservabilityConstants;
import io.ballerina.runtime.observability.ObserveUtils;
import io.ballerina.runtime.observability.ObserverContext;
import io.ballerina.runtime.observability.tracer.BSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for injecting file module observability context into Ballerina strands and spans.
 */
public class FileTracingUtil {

    private static final Logger log = LoggerFactory.getLogger(FileTracingUtil.class);

    private FileTracingUtil() {
    }

    /**
     * Creates a per-file parent span that covers the entire file event lifecycle
     * (found -> dispatched -> handled). The returned context has a {@link BSpan} set on it.
     * When this context is set as the parent of strand properties (via {@link #setParentContext}),
     * the runtime automatically creates child spans for each {@code callMethod} invocation.
     *
     * @param watchedPath monitored directory path
     * @param filePath    path of the file being processed (added as a trace-only tag)
     * @return context with parent span, or {@code null} if tracing is disabled
     */
    public static FileObserverContext createFileLifecycleContext(String watchedPath, String filePath) {
        if (!ObserveUtils.isTracingEnabled()) {
            return null;
        }
        try {
            FileObserverContext ctx = new FileObserverContext(
                    FileMetricsUtil.CONTEXT_LISTENER, watchedPath);
            BSpan span = BSpan.start("file", "file-lifecycle", false);
            span.addTag(FileObserverContext.TAG_MODULE, FileMetricsUtil.MODULE_FILE);
            span.addTag(FileObserverContext.TAG_PROTOCOL, FileMetricsUtil.PROTOCOL_LOCAL);
            span.addTag(FileObserverContext.TAG_CONTEXT, FileMetricsUtil.CONTEXT_LISTENER);
            span.addTag(FileObserverContext.TAG_REMOTE_URL, "localhost");
            span.addTag(FileObserverContext.TAG_ACTION_TYPE, FileMetricsUtil.ACTION_TYPE_EVENT);
            String instanceUrl = FileMetricsUtil.getInstanceUrl();
            if (instanceUrl != null) {
                span.addTag(FileObserverContext.TAG_INSTANCE_URL, instanceUrl);
            }
            if (watchedPath != null) {
                span.addTag(FileObserverContext.TAG_WATCHED_PATH, watchedPath);
            }
            if (filePath != null) {
                span.addTag(FileObserverContext.TAG_FILE_PATH, filePath);
            }
            ctx.setSpan(span);
            return ctx;
        } catch (Throwable t) {
            log.debug("Failed to create file lifecycle context", t);
            return null;
        }
    }

    /**
     * Sets a parent context on the observer context inside strand properties, so that the
     * auto-instrumented span created by {@code callMethod} becomes a child of the parent's span.
     *
     * @param strandProperties the strand properties map (may be null)
     * @param parentCtx        the parent context with a span set on it (may be null)
     */
    public static void setParentContext(Map<String, Object> strandProperties,
                                        FileObserverContext parentCtx) {
        if (strandProperties == null || parentCtx == null) {
            return;
        }
        try {
            Object ctxObj = strandProperties.get(ObservabilityConstants.KEY_OBSERVER_CONTEXT);
            if (ctxObj instanceof ObserverContext ctx) {
                ctx.setParent(parentCtx);
            }
        } catch (Throwable t) {
            log.debug("Failed to set parent context on strand properties", t);
        }
    }

    /**
     * Finishes the per-file parent span. Must be called exactly once per file event, after all
     * handler invocations have completed.
     *
     * @param parentCtx the parent context returned by {@link #createFileLifecycleContext}, or null
     */
    public static void finishFileLifecycleSpan(FileObserverContext parentCtx) {
        if (parentCtx == null) {
            return;
        }
        try {
            BSpan span = parentCtx.getSpan();
            if (span != null) {
                span.finishSpan();
            }
        } catch (Throwable t) {
            log.debug("Failed to finish file lifecycle span", t);
        }
    }

    /**
     * Creates strand properties containing a {@link FileObserverContext} for a listener event
     * dispatch. Pass the returned map to {@code new StrandMetadata(isConcurrentSafe, props)} when
     * invoking a service method via {@code callMethod}.
     *
     * @param watchedPath monitored directory path
     * @param eventType   event type tag value (e.g. {@link FileMetricsUtil#EVENT_TYPE_CREATE})
     * @param handlerName handler method name (e.g. "onCreate")
     * @return properties map, or {@code null} if observability is disabled
     */
    public static Map<String, Object> createStrandProperties(String watchedPath, String eventType,
                                                              String handlerName) {
        if (!ObserveUtils.isObservabilityEnabled()) {
            return null;
        }
        try {
            FileObserverContext observerContext = new FileObserverContext(
                    FileMetricsUtil.CONTEXT_LISTENER, watchedPath);
            observerContext.addTag(FileObserverContext.TAG_ACTION_TYPE, FileMetricsUtil.ACTION_TYPE_EVENT);
            String instanceUrl = FileMetricsUtil.getInstanceUrl();
            if (instanceUrl != null) {
                observerContext.addTag(FileObserverContext.TAG_INSTANCE_URL, instanceUrl);
            }
            observerContext.addTag(FileObserverContext.TAG_EVENT_TYPE, eventType);
            if (handlerName != null) {
                observerContext.addTag(FileObserverContext.TAG_HANDLER_NAME, handlerName);
            }
            Map<String, Object> properties = new HashMap<>();
            properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
            return properties;
        } catch (Throwable t) {
            log.debug("Failed to create strand properties", t);
            return null;
        }
    }

    /**
     * Adds file metadata (size, modified time) directly to the lifecycle parent span.
     * These are added via {@link BSpan#addTag} on the already-created parent span rather than
     * through {@link io.ballerina.runtime.observability.ObserverContext#addTag} or
     * {@link io.ballerina.runtime.observability.ObserverContext#addProperty}, because:
     * <ul>
     *   <li>{@code ObserverContext.addTag} would propagate to framework-level metrics,
     *       causing cardinality explosion for per-file values.</li>
     *   <li>{@code ObserverContext.addProperty} is not copied onto the span by the runtime's
     *       {@code TracingUtils.stopObservation}.</li>
     * </ul>
     * Writing directly to the {@link BSpan} is trace-only and avoids both problems.
     *
     * @param strandProperties the strand properties map (may be null)
     * @param filePath         file path to read metadata from
     */
    public static void addFileMetadataToStrandProperties(Map<String, Object> strandProperties,
                                                          String filePath) {
        if (strandProperties == null || filePath == null || !ObserveUtils.isTracingEnabled()) {
            return;
        }
        try {
            FileObserverContext ctx = (FileObserverContext) strandProperties.get(
                    ObservabilityConstants.KEY_OBSERVER_CONTEXT);
            if (ctx == null || ctx.getParent() == null || ctx.getParent().getSpan() == null) {
                return;
            }
            BSpan parentSpan = ctx.getParent().getSpan();
            File file = new File(filePath);
            if (file.exists()) {
                parentSpan.addTag(FileObserverContext.TAG_FILE_SIZE, String.valueOf(file.length()));
                parentSpan.addTag(FileObserverContext.TAG_FILE_MODIFIED_TIME,
                        String.valueOf(file.lastModified()));
            }
        } catch (Throwable t) {
            log.debug("Failed to add file metadata to strand properties", t);
        }
    }
}
