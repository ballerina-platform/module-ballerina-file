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

import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
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

import java.util.List;
import java.util.Optional;

/**
 * File service validator for compiler API.
 */
public class FileServiceValidator implements AnalysisTask<SyntaxNodeAnalysisContext> {
    private static final String FILE_EVENT = "file:FileEvent";
    private static final String RESOURCE_NAME_ON_CREATE = "onCreate";
    private static final String RESOURCE_NAME_ON_DELETE = "onDelete";
    private static final String RESOURCE_NAME_ON_MODIFY = "onModify";
    public static final String BALLERINA_ORG_NAME = "ballerina";
    public static final String PACKAGE_NAME = "file";

    @Override
    public void perform(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        List<Diagnostic> diagnostics = syntaxNodeAnalysisContext.semanticModel().diagnostics();
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR) {
                return;
            }
        }
        if (isFileService(syntaxNodeAnalysisContext)) {
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
                        syntaxNodeAnalysisContext, ErrorCodes.FILE_106);
            }
        }
    }

    public void validateServiceFunctions(FunctionDefinitionNode functionDefinitionNode,
                                         SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        boolean hasRemoteKeyword = functionDefinitionNode.qualifierList().stream()
                .filter(q -> q.kind() == SyntaxKind.REMOTE_KEYWORD).toArray().length == 1;
        if (!hasRemoteKeyword) {
            reportErrorDiagnostic(functionDefinitionNode.location(), syntaxNodeAnalysisContext,
                    ErrorCodes.FILE_102, functionDefinitionNode.functionName().text());
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
                    ErrorCodes.FILE_103, functionName);
        }
        if (parameterNodes.size() == 1) {
            RequiredParameterNode requiredParameterNode = (RequiredParameterNode)
                    functionSignatureNode.parameters().get(0);
            String value = requiredParameterNode.toString();
            if (!value.contains(FILE_EVENT)) {
                reportErrorDiagnostic(functionDefinitionNode.location(), syntaxNodeAnalysisContext,
                        ErrorCodes.FILE_101, value.split(" ")[0]);
            } else if (functionSignatureNode.returnTypeDesc().isPresent()) {
                reportErrorDiagnostic(functionDefinitionNode.location(), syntaxNodeAnalysisContext,
                        ErrorCodes.FILE_104, functionName);
            }
        } else {
            reportErrorDiagnostic(functionDefinitionNode.location(), syntaxNodeAnalysisContext,
                    ErrorCodes.FILE_105);
        }

    }

    public boolean isFileService(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext) {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) syntaxNodeAnalysisContext.node();
        Optional<Symbol> serviceDeclarationSymbol = syntaxNodeAnalysisContext.semanticModel()
                .symbol(serviceDeclarationNode);
        if (serviceDeclarationSymbol.isPresent()) {
            List<TypeSymbol> listenerTypes = ((ServiceDeclarationSymbol) serviceDeclarationSymbol.get())
                    .listenerTypes();
            for (TypeSymbol listenerType : listenerTypes) {
                if (listenerType.typeKind() == TypeDescKind.UNION) {
                    List<TypeSymbol> memberDescriptors = ((UnionTypeSymbol) listenerType).memberTypeDescriptors();
                    for (TypeSymbol typeSymbol : memberDescriptors) {
                        if (typeSymbol.getModule().isPresent() && typeSymbol.getModule().get().id().orgName()
                                .equals(BALLERINA_ORG_NAME) && typeSymbol.getModule()
                                .flatMap(Symbol::getName).orElse("").equals(PACKAGE_NAME)) {

                            return true;
                        }
                    }
                } else if (listenerType.typeKind() == TypeDescKind.TYPE_REFERENCE
                        && listenerType.getModule().isPresent()
                        && listenerType.getModule().get().id().orgName().equals(BALLERINA_ORG_NAME)
                        && ((TypeReferenceTypeSymbol) listenerType).typeDescriptor().getModule()
                        .flatMap(Symbol::getName).orElse("").equals(PACKAGE_NAME)) {

                    return true;
                }
            }
        }
        return false;
    }

    public void reportErrorDiagnostic(Location location, SyntaxNodeAnalysisContext syntaxNodeAnalysisContext,
                                      ErrorCodes errorCode, Object... args) {
        DiagnosticInfo diagnosticErrInfo = new DiagnosticInfo(errorCode.getErrorCode() , errorCode.getError(),
                DiagnosticSeverity.ERROR);
        Diagnostic diagnostic = DiagnosticFactory.createDiagnostic(diagnosticErrInfo, location, args);
        syntaxNodeAnalysisContext.reportDiagnostic(diagnostic);
    }
}
