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

package io.ballerina.stdlib.file.compiler;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;

import java.util.Optional;

/**
 * Helpers for the {@code @file:FunctionConfig} annotation.
 */
public final class FunctionConfigUtil {

    static final String FUNCTION_CONFIG = "FunctionConfig";
    static final String AFTER_PROCESS = "afterProcess";
    static final String AFTER_ERROR = "afterError";
    static final String BALLERINA_ORG_NAME = "ballerina";
    static final String PACKAGE_NAME = "file";

    private FunctionConfigUtil() {
    }

    /**
     * Finds the {@code @file:FunctionConfig} annotation attached to a function, if any.
     *
     * @param functionDefinitionNode The function
     * @param semanticModel          The semantic model of the function's module
     * @return The annotation node
     */
    public static Optional<AnnotationNode> findFunctionConfig(FunctionDefinitionNode functionDefinitionNode,
                                                              SemanticModel semanticModel) {
        Optional<MetadataNode> metadata = functionDefinitionNode.metadata();
        if (metadata.isEmpty()) {
            return Optional.empty();
        }
        for (AnnotationNode annotationNode : metadata.get().annotations()) {
            Optional<Symbol> symbol = semanticModel.symbol(annotationNode.annotReference());
            if (symbol.isEmpty()) {
                symbol = semanticModel.symbol(annotationNode);
            }
            if (symbol.isPresent() && symbol.get() instanceof AnnotationSymbol annotationSymbol
                    && FUNCTION_CONFIG.equals(annotationSymbol.getName().orElse(""))
                    && annotationSymbol.getModule().map(FunctionConfigUtil::isFileModule).orElse(false)) {
                return Optional.of(annotationNode);
            }
        }
        return Optional.empty();
    }

    /**
     * Checks whether the annotation value configures at least one post-processing action.
     *
     * @param annotationNode The annotation
     * @return true if afterProcess or afterError is set, or the value cannot be inspected statically
     */
    public static boolean hasConfiguredAction(AnnotationNode annotationNode) {
        Optional<MappingConstructorExpressionNode> value = annotationNode.annotValue();
        if (value.isEmpty()) {
            return false;
        }
        for (MappingFieldNode field : value.get().fields()) {
            if (field.kind() != SyntaxKind.SPECIFIC_FIELD) {
                return true;
            }
            String fieldName = ((SpecificFieldNode) field).fieldName().toString().trim().replace("\"", "");
            if (AFTER_PROCESS.equals(fieldName) || AFTER_ERROR.equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    static boolean isFileModule(ModuleSymbol moduleSymbol) {
        return BALLERINA_ORG_NAME.equals(moduleSymbol.id().orgName())
                && PACKAGE_NAME.equals(moduleSymbol.id().moduleName());
    }

    static Diagnostic createDiagnostic(ErrorCodes errorCode, Location location, Object... args) {
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(errorCode.getErrorCode(), errorCode.getError(),
                DiagnosticSeverity.ERROR);
        return DiagnosticFactory.createDiagnostic(diagnosticInfo, location, args);
    }
}
