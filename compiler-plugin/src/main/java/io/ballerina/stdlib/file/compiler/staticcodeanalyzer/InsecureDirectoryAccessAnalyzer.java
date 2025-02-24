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

import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.projects.Document;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.scan.Reporter;
import io.ballerina.tools.diagnostics.Location;

import static io.ballerina.stdlib.file.compiler.staticcodeanalyzer.FileRule.AVOID_INSECURE_DIRECTORY_ACCESS;

public class InsecureDirectoryAccessAnalyzer implements AnalysisTask<SyntaxNodeAnalysisContext> {
    private final Reporter reporter;

    public InsecureDirectoryAccessAnalyzer(Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        // Check if the current node is a function call
        if (!(context.node() instanceof FunctionCallExpressionNode functionCall)) {
            return;
        }

        // Check if the function call is related to file operations
        if (!isFileOperation(functionCall)) {
            return;
        }

        Document document = getDocument(context);

        // Check if the function call contains insecure directory paths
        if (containsInsecureDirectory(functionCall.arguments(), context)) {
            // Report a diagnostic for insecure directory access
            Location location = functionCall.location();
            this.reporter.reportIssue(document, location, AVOID_INSECURE_DIRECTORY_ACCESS.getId());
        }
    }

    public static Document getDocument(SyntaxNodeAnalysisContext context) {
        return context.currentPackage().module(context.moduleId()).document(context.documentId());
    }

    private boolean isFileOperation(FunctionCallExpressionNode functionCall) {
        if (!(functionCall.functionName() instanceof QualifiedNameReferenceNode qNode)) {
            return false;
        }
        String modulePrefix = qNode.modulePrefix().text();
        String functionName = qNode.identifier().text();
        return modulePrefix.equals("file") &&
                (functionName.equals("create") || functionName.equals("getAbsolutePath") ||
                        functionName.equals("createTemp") || functionName.equals("createTempDir"));
    }

    private boolean containsInsecureDirectory(SeparatedNodeList<FunctionArgumentNode> arguments,
                                              SyntaxNodeAnalysisContext context) {
        for (FunctionArgumentNode arg : arguments) {
            ExpressionNode expr;
            if (arg instanceof PositionalArgumentNode posArg) {
                expr = posArg.expression();
            } else {
                continue;
            }

            // Extract the file path from the expression
            String filePath = expr.toSourceCode().trim();
            if (isInsecureDirectory(filePath)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInsecureDirectory(String filePath) {
        return filePath.contains("/tmp") || filePath.contains("TMP") || filePath.contains("TEMP");
    }
}
