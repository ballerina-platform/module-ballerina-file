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

import io.ballerina.runtime.observability.ObserveUtils;
import io.ballerina.runtime.observability.metrics.DefaultMetricRegistry;
import io.ballerina.runtime.observability.metrics.MetricId;
import io.ballerina.runtime.observability.metrics.MetricRegistry;
import io.ballerina.runtime.observability.metrics.StatisticConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.time.Duration;

/**
 * Utility class for recording file module metrics.
 *
 * <p>All public methods swallow exceptions internally so that observability failures
 * never break file operations.
 *
 * <p>Metrics published:
 * <ul>
 *   <li>{@code file_events_total} (counter) — total file lifecycle events</li>
 *   <li>{@code file_resource_execution_duration_seconds} (gauge) — handler execution duration</li>
 * </ul>
 */
public class FileMetricsUtil {

    private static final Logger log = LoggerFactory.getLogger(FileMetricsUtil.class);
    private static final String METRIC_NAME_SEPARATOR = "_";
    private static final String FILE_CONNECTOR_NAME = "file";
    private static final String[] METRIC_FILE_EVENTS = {
            "events_total", "Total file lifecycle events"};
    private static final String[] METRIC_RESOURCE_EXECUTION_DURATION = {
            "resource_execution_duration_seconds", "Time taken to execute the resource/handler method"};

    private static final StatisticConfig DURATION_STATISTIC_CONFIG = StatisticConfig.builder()
            .percentiles(0.5, 0.75, 0.9, 0.95, 0.99)
            .expiry(Duration.ofMinutes(5))
            .buckets(10)
            .build();

    public static final String NONE = "none";
    public static final String MODULE_FILE = "file";
    public static final String PROTOCOL_LOCAL = "local";
    public static final String CONTEXT_LISTENER = "listener";
    public static final String EVENT_TYPE_CREATE = "create";
    public static final String EVENT_TYPE_DELETE = "delete";
    public static final String EVENT_TYPE_MODIFY = "modify";
    public static final String ACTION_TYPE_EVENT = "file_event";

    public static final String FILE_STAGE_FOUND = "found";
    public static final String FILE_STAGE_DISPATCHED = "dispatched";
    public static final String FILE_STAGE_HANDLED = "handled";

    public static final String OUTCOME_SUCCESS = "success";
    public static final String OUTCOME_FAILURE = "failure";
    public static final String OUTCOME_SKIPPED = "skipped";
    public static final String FAILURE_NO_HANDLER_MATCHED = "no_handler_matched";

    private static final MetricRegistry metricRegistry = DefaultMetricRegistry.getInstance();

    /** Returns the hostname of the current instance, or {@code null} if unavailable. */
    public static String getInstanceUrl() {
        return InstanceUrlHolder.INSTANCE_URL;
    }

    private static final class InstanceUrlHolder {
        static final String INSTANCE_URL;

        static {
            String url;
            try {
                url = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                url = null;
            }
            INSTANCE_URL = url;
        }
    }

    private FileMetricsUtil() {
    }

    /**
     * Reports a file lifecycle stage event. Publishes an explicit {@code file_events_total} counter
     * increment with the standard tag set.
     *
     * @param watchedPath monitored directory path
     * @param fileStage   lifecycle stage (found, dispatched, handled)
     * @param outcome     outcome tag value, or {@code null}
     * @param errorType   error type (Ballerina error name or predefined reason), or {@code null}
     * @param handlerName handler method name, or {@code null}
     */
    public static void reportFileStage(String watchedPath, String fileStage,
                                       String outcome, String errorType, String handlerName) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        try {
            FileObserverContext observerContext = new FileObserverContext(CONTEXT_LISTENER, watchedPath);
            observerContext.addTag(FileObserverContext.TAG_ACTION_TYPE, ACTION_TYPE_EVENT);
            observerContext.addTag(FileObserverContext.TAG_FILE_STAGE, fileStage);
            observerContext.addTag(FileObserverContext.TAG_OUTCOME, outcome != null ? outcome : NONE);
            observerContext.addTag(FileObserverContext.TAG_ERROR_TYPE, errorType != null ? errorType : NONE);
            observerContext.addTag(FileObserverContext.TAG_HANDLER_NAME, handlerName != null ? handlerName : NONE);
            String host = getInstanceUrl();
            observerContext.addTag(FileObserverContext.TAG_INSTANCE_URL, host != null ? host : NONE);
            metricRegistry.counter(new MetricId(
                    FILE_CONNECTOR_NAME + METRIC_NAME_SEPARATOR + METRIC_FILE_EVENTS[0],
                    METRIC_FILE_EVENTS[1], observerContext.getAllTags())).increment();
        } catch (Throwable t) {
            log.debug("Failed to report file stage metric", t);
        }
    }

    /**
     * Reports the time taken to execute the user's resource/handler method.
     *
     * @param watchedPath  monitored directory path
     * @param handlerName  handler method name (e.g. "onCreate")
     * @param outcome      {@link #OUTCOME_SUCCESS} or {@link #OUTCOME_FAILURE}
     * @param durationSecs duration in seconds
     */
    public static void reportResourceExecutionDuration(String watchedPath, String handlerName,
                                                       String outcome, double durationSecs) {
        if (!ObserveUtils.isMetricsEnabled()) {
            return;
        }
        try {
            FileObserverContext observerContext = new FileObserverContext(CONTEXT_LISTENER, watchedPath);
            if (handlerName != null) {
                observerContext.addTag(FileObserverContext.TAG_HANDLER_NAME, handlerName);
            }
            observerContext.addTag(FileObserverContext.TAG_OUTCOME, outcome);
            metricRegistry.gauge(new MetricId(
                    FILE_CONNECTOR_NAME + METRIC_NAME_SEPARATOR + METRIC_RESOURCE_EXECUTION_DURATION[0],
                    METRIC_RESOURCE_EXECUTION_DURATION[1], observerContext.getAllTags()),
                    DURATION_STATISTIC_CONFIG).setValue(durationSecs);
        } catch (Throwable t) {
            log.debug("Failed to report resource execution duration metric", t);
        }
    }

    /**
     * Reports a handled-stage failure and its execution duration in a single call.
     *
     * @param watchedPath  monitored directory path
     * @param errorType    error type name
     * @param functionName handler method name
     * @param durationSecs duration in seconds
     */
    public static void reportHandledFailure(String watchedPath, String errorType,
                                            String functionName, double durationSecs) {
        reportFileStage(watchedPath, FILE_STAGE_HANDLED, OUTCOME_FAILURE, errorType, functionName);
        reportResourceExecutionDuration(watchedPath, functionName, OUTCOME_FAILURE, durationSecs);
    }
}
