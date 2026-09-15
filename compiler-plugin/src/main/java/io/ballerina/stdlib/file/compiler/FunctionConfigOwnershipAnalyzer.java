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
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.CompilationAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reports a second service that configures a post-processing action for the same remote function on the same
 * listener variable.
 */
public class FunctionConfigOwnershipAnalyzer implements AnalysisTask<CompilationAnalysisContext> {

    private static final Set<String> POST_PROCESSING_FUNCTIONS = Set.of("onCreate", "onModify");

    @Override
    public void perform(CompilationAnalysisContext context) {
        Set<String> claimed = new HashSet<>();
        for (ModuleId moduleId : context.currentPackage().moduleIds()) {
            Module module = context.currentPackage().module(moduleId);
            SemanticModel semanticModel = context.compilation().getSemanticModel(moduleId);
            if (hasErrors(semanticModel)) {
                return;
            }
            List<DocumentId> documentIds = new ArrayList<>(module.documentIds());
            documentIds.addAll(module.testDocumentIds());
            for (DocumentId documentId : documentIds) {
                Document document = module.document(documentId);
                ModulePartNode modulePartNode = document.syntaxTree().rootNode();
                for (Node member : modulePartNode.members()) {
                    if (member.kind() == SyntaxKind.SERVICE_DECLARATION) {
                        analyzeService((ServiceDeclarationNode) member, semanticModel, moduleId, claimed, context);
                    }
                }
            }
        }
    }

    private void analyzeService(ServiceDeclarationNode serviceNode, SemanticModel semanticModel, ModuleId moduleId,
                                Set<String> claimed, CompilationAnalysisContext context) {
        if (!FileServiceValidator.isFileService(semanticModel, serviceNode)) {
            return;
        }
        List<String> listeners = listenerNames(serviceNode, semanticModel);
        if (listeners.isEmpty()) {
            return;
        }
        for (Node member : serviceNode.members()) {
            configuredAction(member, semanticModel).ifPresent(annotation ->
                    claim((FunctionDefinitionNode) member, annotation, listeners, moduleId, claimed, context));
        }
    }

    private List<String> listenerNames(ServiceDeclarationNode serviceNode, SemanticModel semanticModel) {
        List<String> listeners = new ArrayList<>();
        for (ExpressionNode expression : serviceNode.expressions()) {
            listenerName(expression, semanticModel).ifPresent(listeners::add);
        }
        return listeners;
    }

    /** The FunctionConfig annotation of an onCreate or onModify member that configures at least one action. */
    private Optional<AnnotationNode> configuredAction(Node member, SemanticModel semanticModel) {
        if (member.kind() != SyntaxKind.OBJECT_METHOD_DEFINITION) {
            return Optional.empty();
        }
        FunctionDefinitionNode function = (FunctionDefinitionNode) member;
        if (!POST_PROCESSING_FUNCTIONS.contains(function.functionName().text())) {
            return Optional.empty();
        }
        return FunctionConfigUtil.findFunctionConfig(function, semanticModel)
                .filter(FunctionConfigUtil::hasConfiguredAction);
    }

    private void claim(FunctionDefinitionNode function, AnnotationNode annotation, List<String> listeners,
                       ModuleId moduleId, Set<String> claimed, CompilationAnalysisContext context) {
        String functionName = function.functionName().text();
        for (String listener : listeners) {
            if (!claimed.add(moduleId.toString() + ":" + listener + ":" + functionName)) {
                context.reportDiagnostic(FunctionConfigUtil.createDiagnostic(ErrorCodes.FILE_108,
                        annotation.location(), functionName, listener));
            }
        }
    }

    private Optional<String> listenerName(ExpressionNode expression, SemanticModel semanticModel) {
        if (expression.kind() != SyntaxKind.SIMPLE_NAME_REFERENCE) {
            return Optional.empty();
        }
        Optional<Symbol> symbol = semanticModel.symbol(expression);
        if (symbol.isPresent() && symbol.get() instanceof VariableSymbol variableSymbol
                && variableSymbol.qualifiers().contains(Qualifier.LISTENER)) {
            return variableSymbol.getName();
        }
        return Optional.empty();
    }

    private boolean hasErrors(SemanticModel semanticModel) {
        for (Diagnostic diagnostic : semanticModel.diagnostics()) {
            if (diagnostic.diagnosticInfo().severity() == DiagnosticSeverity.ERROR) {
                return true;
            }
        }
        return false;
    }
}
