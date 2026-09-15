/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.file.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Post-processing actions declared by one service, keyed by remote function name.
 */
public final class PostProcessConfig {

    private final Map<String, PostProcessAction> methodAfterProcessAction = new HashMap<>();
    private final Map<String, PostProcessAction> methodAfterErrorAction = new HashMap<>();

    void put(String methodName, PostProcessAction afterProcess, PostProcessAction afterError) {
        if (afterProcess != null) {
            methodAfterProcessAction.put(methodName, afterProcess);
        }
        if (afterError != null) {
            methodAfterErrorAction.put(methodName, afterError);
        }
    }

    /**
     * Gets the afterProcess action of a remote function.
     *
     * @param methodName The remote function name
     * @return The action, if configured
     */
    public Optional<PostProcessAction> getAfterProcessAction(String methodName) {
        return Optional.ofNullable(methodAfterProcessAction.get(methodName));
    }

    /**
     * Gets the afterError action of a remote function.
     *
     * @param methodName The remote function name
     * @return The action, if configured
     */
    public Optional<PostProcessAction> getAfterErrorAction(String methodName) {
        return Optional.ofNullable(methodAfterErrorAction.get(methodName));
    }

    /**
     * Checks whether a remote function configures at least one action.
     *
     * @param methodName The remote function name
     * @return true if afterProcess or afterError is configured
     */
    public boolean hasPostProcessingActions(String methodName) {
        return methodAfterProcessAction.containsKey(methodName) || methodAfterErrorAction.containsKey(methodName);
    }
}
