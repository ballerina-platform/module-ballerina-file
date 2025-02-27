/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org)
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

package io.ballerina.stdlib.file.compiler.staticcodeanalyzer;

import io.ballerina.compiler.syntax.tree.BinaryExpressionNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SimpleNameReferenceNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.scan.Reporter;
import io.ballerina.tools.diagnostics.Location;

import static io.ballerina.stdlib.file.compiler.Constants.FILE_REMOVE;
import static io.ballerina.stdlib.file.compiler.Constants.FILE_READ;
import static io.ballerina.stdlib.file.compiler.Constants.FILE_WRITE;
import static io.ballerina.stdlib.file.compiler.staticcodeanalyzer.FileRule.AVOID_PATH_INJECTION;

/**
 * Analyzer to detect potential file path injection vulnerabilities.
 */
public class FilePathInjectionAnalyzer implements AnalysisTask<SyntaxNodeAnalysisContext> {

    private final Reporter reporter;

    public FilePathInjectionAnalyzer(Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        if (!(context.node() instanceof FunctionCallExpressionNode functionCall)) {
            return;
        }

        Document document = getDocument(context);

        String functionName = functionCall.functionName().toString();

        // Detect vulnerable file function calls
        if ((FILE_REMOVE.equals(functionName) || FILE_READ.equals(functionName) ||
                FILE_WRITE.equals(functionName)) && !isSafePath(functionCall)) {
            Location location = functionCall.location();
            this.reporter.reportIssue(document, location, AVOID_PATH_INJECTION.getId());
        }
    }

    public static Document getDocument(SyntaxNodeAnalysisContext context) {
        return context.currentPackage().module(context.moduleId()).document(context.documentId());
    }

    private boolean isSafePath(FunctionCallExpressionNode functionCall) {
        NodeList<FunctionArgumentNode> arguments = functionCall.arguments();
        if (arguments.isEmpty()) {
            return true;
        }

        FunctionArgumentNode firstArg = arguments.get(0);
        ExpressionNode argument;

        if (firstArg instanceof PositionalArgumentNode) {
            argument = ((PositionalArgumentNode) firstArg).expression();
        } else {
            return true;
        }

        // Direct concatenation detection
        if (argument instanceof BinaryExpressionNode binaryExpression &&
                binaryExpression.operator().kind() == SyntaxKind.PLUS_TOKEN) {
            return false;
        }

        if (argument instanceof SimpleNameReferenceNode variableRef) {
            return isVariableSafe(variableRef);
        }
        return true;
    }

    private boolean isVariableSafe(SimpleNameReferenceNode variableRef) {
        String variableName = variableRef.name().text();
        Node currentNode = variableRef.parent();

        while (currentNode != null) {
            if (currentNode instanceof FunctionBodyBlockNode functionBody) {
                for (StatementNode statement : functionBody.statements()) {
                    if (statement instanceof VariableDeclarationNode varDecl &&
                            isMatchingVariable(varDecl, variableName)) {
                        if (hasConcatenationAssignment(varDecl) || isAssignedFromFunctionParameter(varDecl)) {
                            return isFunctionParameter(variableRef);
                        }
                        return true;
                    }
                }
            }
            currentNode = currentNode.parent();
        }
        return true;
    }

    private boolean isMatchingVariable(VariableDeclarationNode varDecl, String variableName) {
        return varDecl.typedBindingPattern().bindingPattern() instanceof CaptureBindingPatternNode bindingPattern
                && bindingPattern.variableName().text().equals(variableName);
    }

    private boolean hasConcatenationAssignment(VariableDeclarationNode varDecl) {
        return varDecl.initializer().orElse(null) instanceof BinaryExpressionNode binaryExpr
                && binaryExpr.operator().kind() == SyntaxKind.PLUS_TOKEN;
    }

    private boolean isAssignedFromFunctionParameter(VariableDeclarationNode varDecl) {
        return varDecl.initializer().orElse(null) instanceof SimpleNameReferenceNode;
    }

    private boolean isFunctionParameter(SimpleNameReferenceNode variableRef) {
        String paramName = variableRef.name().text();
        Node currentNode = variableRef.parent();

        while (currentNode != null) {
            if (currentNode instanceof FunctionDefinitionNode functionDef) {
                if (hasDirectParameterReference(functionDef, paramName) ||
                        hasIndirectParameterReference(variableRef, functionDef)) {
                    return false;
                }
            }
            currentNode = currentNode.parent();
        }
        return true;
    }

    private boolean hasDirectParameterReference(FunctionDefinitionNode functionDef, String paramName) {
        for (ParameterNode param : functionDef.functionSignature().parameters()) {
            if (param instanceof RequiredParameterNode reqParam && reqParam.paramName().isPresent()
                    && reqParam.paramName().get().text().equals(paramName)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasIndirectParameterReference(SimpleNameReferenceNode variableRef,
                                                  FunctionDefinitionNode functionDef) {
        for (ParameterNode param : functionDef.functionSignature().parameters()) {
            if (param instanceof RequiredParameterNode reqParam && isIndirectFunctionParameter(variableRef, reqParam)) {
                return true;
            }
        }
        return false;
    }

    private boolean isIndirectFunctionParameter(SimpleNameReferenceNode variableRef, RequiredParameterNode reqParam) {
        Node currentNode = variableRef.parent();

        while (currentNode != null) {
            if (currentNode instanceof FunctionBodyBlockNode functionBody) {
                for (StatementNode statement : functionBody.statements()) {
                    if (statement instanceof VariableDeclarationNode varDecl
                            && isBindingPatternMatch(varDecl, variableRef)) {
                        return checkVariableInitializer(varDecl, reqParam);
                    }
                }
            }
            currentNode = currentNode.parent();
        }
        return false;
    }

    private boolean isBindingPatternMatch(VariableDeclarationNode varDecl, SimpleNameReferenceNode variableRef) {
        return varDecl.typedBindingPattern().bindingPattern() instanceof CaptureBindingPatternNode bindingPattern
                && bindingPattern.variableName().text().equals(variableRef.name().text());
    }

    private boolean checkVariableInitializer(VariableDeclarationNode varDecl, RequiredParameterNode reqParam) {
        ExpressionNode initializer = varDecl.initializer().orElse(null);
        if (initializer == null) {
            return false;
        }

        if (initializer instanceof SimpleNameReferenceNode initializerRef) {
            return initializerRef.name().text().equals(reqParam.paramName().get().text());
        }

        if (initializer instanceof BinaryExpressionNode binaryExpr) {
            return binaryExpr.operator().kind() == SyntaxKind.PLUS_TOKEN
                    && isIndirectFunctionParameterFromBinary(binaryExpr, reqParam);
        }

        return false;
    }

    private boolean isIndirectFunctionParameterFromBinary(BinaryExpressionNode binaryExpr,
                                                          RequiredParameterNode reqParam) {
        if (binaryExpr.lhsExpr() instanceof SimpleNameReferenceNode leftRef &&
                leftRef.name().text().equals(reqParam.paramName().get().text())) {
            return true;
        }
        return binaryExpr.rhsExpr() instanceof SimpleNameReferenceNode rightRef &&
                rightRef.name().text().equals(reqParam.paramName().get().text());
    }
}
