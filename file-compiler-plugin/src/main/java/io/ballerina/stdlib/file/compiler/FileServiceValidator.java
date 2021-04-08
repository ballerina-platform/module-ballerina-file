/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.file.compiler;

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;
import org.ballerinalang.util.diagnostic.DiagnosticErrorCode;

/**
 * File service validator for compiler API.
 */
public class FileServiceValidator implements AnalysisTask<SyntaxNodeAnalysisContext> {
    private static final String CODE = "File";
    private static final String FILE_EVENT = "file:FileEvent";
    private static final String RESOURCE_NAME_ON_CREATE = "onCreate";
    private static final String RESOURCE_NAME_ON_DELETE = "onDelete";
    private static final String RESOURCE_NAME_ON_MODIFY = "onModify";
    private static final String INVALID_INPUT_PARAM = "Invalid parameter type `{0}` provided for remote function. " +
            "Only file:FileEvent is allowed as the parameter type";
    private static final String INVALID_REMOTE_FUNCTION = "Missing remote key word in the remote function `{0}`";
    private static final String INVALID_FUNCTION_NAME = "Invalid function name `{0}`";
    private static final String INVALID_RETURN_TYPE = "Return types are not allowed in the remote function `{0}`";
    private static final String INVALID_PARAM_SIZE = "The remote function should contain a parameter";
    private static final String EMPTY_SERVICE = "At least a single remote function required in the service";

    @Override
    public void perform(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) syntaxNodeAnalysisContext.node();
        long size = serviceDeclarationNode.members().stream().filter(child -> child.kind() ==
                SyntaxKind.OBJECT_METHOD_DEFINITION || child.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION)
                .count();
        if (size > 0) {
            serviceDeclarationNode.members().stream().filter(child -> child.kind() ==
                    SyntaxKind.OBJECT_METHOD_DEFINITION || child.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION).
                    forEach(node -> {
                FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
                // Check functions are remote or not
                validateServiceFunctions(functionDefinitionNode, syntaxNodeAnalysisContext);
                // Check params and return types
                validateFunctionSignature(functionDefinitionNode, syntaxNodeAnalysisContext);

            });
        } else {
            reportErrorDiagnostic(serviceDeclarationNode.absoluteResourcePath().get(0).location(),
                    syntaxNodeAnalysisContext, CODE, EMPTY_SERVICE);
        }

    }

    public void validateServiceFunctions(FunctionDefinitionNode functionDefinitionNode,
                                         SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        boolean hasRemoteKeyword = functionDefinitionNode.qualifierList().stream()
                .filter(q -> q.kind() == SyntaxKind.REMOTE_KEYWORD).toArray().length == 1;
        if (!hasRemoteKeyword) {
            reportErrorDiagnostic(functionDefinitionNode.location(), syntaxNodeAnalysisContext,
                    DiagnosticErrorCode.KEY_NOT_FOUND.diagnosticId(), INVALID_REMOTE_FUNCTION,
                    functionDefinitionNode.functionName().text());
        }
    }

    public void validateFunctionSignature(FunctionDefinitionNode functionDefinitionNode,
                                          SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {

        FunctionSignatureNode functionSignatureNode = functionDefinitionNode.functionSignature();
        SeparatedNodeList<ParameterNode> parameterNodes = functionSignatureNode.parameters();
        String functionName = functionDefinitionNode.functionName().text();
        if (!(functionName.equals(RESOURCE_NAME_ON_CREATE) || functionName.equals(RESOURCE_NAME_ON_DELETE) ||
                functionName.equals(RESOURCE_NAME_ON_MODIFY))) {
            reportErrorDiagnostic(functionDefinitionNode.location(), syntaxNodeAnalysisContext,
                    DiagnosticErrorCode.INVALID_FUNCTION_INVOCATION_WITH_NAME.diagnosticId(), INVALID_FUNCTION_NAME,
                    functionName);
        }
        if (parameterNodes.size() == 1) {
            RequiredParameterNode requiredParameterNode = (RequiredParameterNode)
                    functionSignatureNode.parameters().get(0);
            String value = requiredParameterNode.toString();
            if (!value.contains(FILE_EVENT)) {
                reportErrorDiagnostic(functionDefinitionNode.location(), syntaxNodeAnalysisContext,
                        DiagnosticErrorCode.INVALID_VARIABLE_ASSIGNMENT.diagnosticId(), INVALID_INPUT_PARAM,
                        value.split(" ")[0]);
            } else if (functionSignatureNode.returnTypeDesc().isPresent()) {
                reportErrorDiagnostic(functionDefinitionNode.location(), syntaxNodeAnalysisContext,
                        DiagnosticErrorCode.CHECKED_EXPR_NO_MATCHING_ERROR_RETURN_IN_ENCL_INVOKABLE.diagnosticId(),
                        INVALID_RETURN_TYPE, functionName);
            }
        } else {
            reportErrorDiagnostic(functionDefinitionNode.location(), syntaxNodeAnalysisContext,
                    DiagnosticErrorCode.CHECKED_EXPR_NO_MATCHING_ERROR_RETURN_IN_ENCL_INVOKABLE.diagnosticId(),
                    INVALID_PARAM_SIZE);
        }

    }

    public void reportErrorDiagnostic(Location location, SyntaxNodeAnalysisContext syntaxNodeAnalysisContext,
                                      String code, String message, Object... args) {
        DiagnosticInfo diagnosticErrInfo = new DiagnosticInfo(code , message, DiagnosticSeverity.ERROR);
        Diagnostic diagnostic = DiagnosticFactory.createDiagnostic(diagnosticErrInfo, location, args);
        syntaxNodeAnalysisContext.reportDiagnostic(diagnostic);
    }
}
