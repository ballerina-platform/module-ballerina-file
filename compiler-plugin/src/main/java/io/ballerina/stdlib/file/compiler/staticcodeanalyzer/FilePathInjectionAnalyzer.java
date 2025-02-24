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
        if ("file:remove".equals(functionName) || "file:read".equals(functionName) ||
                "file:write".equals(functionName)) {
            if (!isSafePath(functionCall, context)) {
                Location location = functionCall.location();
                this.reporter.reportIssue(document, location, AVOID_PATH_INJECTION.getId());
            }
        }
    }

    public static Document getDocument(SyntaxNodeAnalysisContext context) {
        return context.currentPackage().module(context.moduleId()).document(context.documentId());
    }

    private boolean isSafePath(FunctionCallExpressionNode functionCall, SyntaxNodeAnalysisContext context) {
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
        if (argument instanceof BinaryExpressionNode binaryExpression) {
            if (binaryExpression.operator().kind() == SyntaxKind.PLUS_TOKEN) {
                return false; // Unsafe concatenation detected
            }
        }

        // Variable reference detection
        if (argument instanceof SimpleNameReferenceNode variableRef) {
            return isVariableSafe(variableRef, context);
        }
        return true;
    }

    private boolean isVariableSafe(SimpleNameReferenceNode variableRef, SyntaxNodeAnalysisContext context) {
        String variableName = variableRef.name().text();
        Node currentNode = variableRef.parent();
        while (currentNode != null) {
            if (currentNode instanceof FunctionBodyBlockNode functionBody) {
                for (StatementNode statement : functionBody.statements()) {
                    if (statement instanceof VariableDeclarationNode varDecl) {
                        if (varDecl.typedBindingPattern().bindingPattern() instanceof
                                CaptureBindingPatternNode bindingPattern) {
                            if (bindingPattern.variableName().text().equals(variableName)) {
                                // Check if assigned using concatenation
                                if (varDecl.initializer().orElse(null) instanceof BinaryExpressionNode) {
                                    BinaryExpressionNode binaryExpr = (BinaryExpressionNode)
                                            varDecl.initializer().get();
                                    if (binaryExpr.operator().kind() == SyntaxKind.PLUS_TOKEN) {
                                        return isFunctionParameter(variableRef);
                                    }
                                }
                                // Check if assigned directly from a function parameter
                                if (varDecl.initializer().orElse(null) instanceof SimpleNameReferenceNode) {
                                    SimpleNameReferenceNode initializer = (SimpleNameReferenceNode)
                                            varDecl.initializer().get();
                                    return isFunctionParameter(initializer);
                                }
                                return true;
                            }
                        }
                    }
                }
            }
            currentNode = currentNode.parent();
        }
        return true;
    }

    private boolean isFunctionParameter(SimpleNameReferenceNode variableRef) {
        String paramName = variableRef.name().text();
        Node currentNode = variableRef.parent();
        while (currentNode != null) {
            if (currentNode instanceof FunctionDefinitionNode functionDef) {
                // Check if function is public
                boolean isPublic = functionDef.qualifierList().stream()
                        .anyMatch(q -> q.kind() == SyntaxKind.PUBLIC_KEYWORD);

                // Iterate over function parameters to check direct reference
                for (ParameterNode param : functionDef.functionSignature().parameters()) {
                    if (param instanceof RequiredParameterNode reqParam) {
                        // Check direct parameter reference
                        if (reqParam.paramName().isPresent() &&
                                reqParam.paramName().get().text().equals(paramName)) {
                            return !isPublic;
                        }
                        // Check indirect reference chain (assignments)
                        if (isIndirectFunctionParameter(variableRef, reqParam)) {
                            return !isPublic;
                        }
                    }
                }
            }
            currentNode = currentNode.parent();
        }
        return true;
    }

    /**
     * Checks if the variable is indirectly referencing a function parameter
     * by tracing back the assignment chain.
     */
    private boolean isIndirectFunctionParameter(SimpleNameReferenceNode variableRef, RequiredParameterNode reqParam) {
        Node currentNode = variableRef.parent();
        while (currentNode != null) {
            if (currentNode instanceof FunctionBodyBlockNode functionBody) {
                for (StatementNode statement : functionBody.statements()) {
                    if (statement instanceof VariableDeclarationNode varDecl) {
                        if (varDecl.typedBindingPattern().bindingPattern() instanceof
                                CaptureBindingPatternNode bindingPattern) {
                            if (bindingPattern.variableName().text().equals(variableRef.name().text())) {
                                // Now check if this variable is assigned to another variable
                                if (varDecl.initializer().isPresent()) {
                                    ExpressionNode initializer = varDecl.initializer().get();
                                    // If it's a reference to the function parameter, return true
                                    if (initializer instanceof SimpleNameReferenceNode initializerRef) {
                                        if (initializerRef.name().text().equals(reqParam.paramName().get().text())) {
                                            return true;
                                        }
                                    }
                                    // If it's a binary expression (like concatenation), recurse
                                    if (initializer instanceof BinaryExpressionNode binaryExpr) {
                                        if (binaryExpr.operator().kind() == SyntaxKind.PLUS_TOKEN) {
                                            // Recursively check both sides of the binary expression
                                            if (isIndirectFunctionParameterFromBinary(binaryExpr, reqParam)) {
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            currentNode = currentNode.parent();
        }
        return false;
    }

    /**
     * Recursively checks both sides of the binary expression (e.g., concatenation) for a reference to the
     * function parameter.
     */
    private boolean isIndirectFunctionParameterFromBinary(BinaryExpressionNode binaryExpr,
                                                          RequiredParameterNode reqParam) {
        // Check both the left and right sides of the binary expression
        if (binaryExpr.lhsExpr() instanceof SimpleNameReferenceNode leftRef) {
            if (leftRef.name().text().equals(reqParam.paramName().get().text())) {
                return true;
            }
        }
        if (binaryExpr.rhsExpr() instanceof SimpleNameReferenceNode rightRef) {
            if (rightRef.name().text().equals(reqParam.paramName().get().text())) {
                return true;
            }
        }
        return false;
    }
}
