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

import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.projects.Document;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.scan.Reporter;
import io.ballerina.tools.diagnostics.Location;
import org.ballerinalang.model.tree.expressions.StringTemplateLiteralNode;

import static io.ballerina.stdlib.file.compiler.Constants.FILE_FUNCTIONS;
import static io.ballerina.stdlib.file.compiler.Constants.OS;
import static io.ballerina.stdlib.file.compiler.Constants.GET_ENV;
import static io.ballerina.stdlib.file.compiler.Constants.FILE;
import static io.ballerina.stdlib.file.compiler.Constants.PUBLIC_DIRECTORIES;
import static io.ballerina.stdlib.file.compiler.staticcodeanalyzer.FileRule.AVOID_INSECURE_DIRECTORY_ACCESS;

public class InsecureDirectoryAccessAnalyzer implements AnalysisTask<SyntaxNodeAnalysisContext> {
    private final Reporter reporter;

    public InsecureDirectoryAccessAnalyzer(Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        if (!(context.node() instanceof FunctionCallExpressionNode functionCall)) {
            return;
        }

        if (!isFileMethodCall(functionCall)) {
            return;
        }

        Document document = getDocument(context);
        Location location = functionCall.location();
        this.reporter.reportIssue(document, location, AVOID_INSECURE_DIRECTORY_ACCESS.getId());
    }

    public static Document getDocument(SyntaxNodeAnalysisContext context) {
        return context.currentPackage().module(context.moduleId()).document(context.documentId());
    }

    private boolean isFileMethodCall(FunctionCallExpressionNode functionCall) {
        Node currentNode = functionCall;
        while (currentNode != null) {
            if (currentNode instanceof FunctionCallExpressionNode currentMethodCall) {
                if (currentMethodCall.functionName() instanceof QualifiedNameReferenceNode qNode) {
                    String modulePrefix = qNode.modulePrefix().text();
                    String functionName = qNode.identifier().text();
                    if (modulePrefix.equals(OS) && functionName.equals(GET_ENV)) {
                        String envVarName = currentMethodCall.arguments().get(0).toString();
                        if (PUBLIC_DIRECTORIES.contains(envVarName)) {
                            return true;
                        }
                    }
                }

                if (currentMethodCall.functionName() instanceof QualifiedNameReferenceNode fileCallNode) {
                    String fileModulePrefix = fileCallNode.modulePrefix().text();
                    String fileFunctionName = fileCallNode.identifier().text();
                    if (fileModulePrefix.equals(FILE) && FILE_FUNCTIONS.contains(fileFunctionName)) {
                        SeparatedNodeList<FunctionArgumentNode> arguments = currentMethodCall.arguments();
                        if (arguments != null && !arguments.isEmpty()) {
                            FunctionArgumentNode pathArg = arguments.get(0);
                            if (pathArg instanceof PositionalArgumentNode posArg) {
                                ExpressionNode pathExpr = posArg.expression();
                                if (pathExpr instanceof BasicLiteralNode pathLiteral) {
                                    String filePath = pathLiteral.toString().trim();
                                    if (isInsecureDirectory(filePath)) {
                                        return true;
                                    }
                                } else if (pathExpr instanceof StringTemplateLiteralNode templatePath) {
                                    String filePath = templatePath.toString().trim();
                                    if (isInsecureDirectory(filePath)) {
                                        return true;
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

    private boolean isInsecureDirectory(String filePath) {
        return PUBLIC_DIRECTORIES.contains(filePath);
    }
}
