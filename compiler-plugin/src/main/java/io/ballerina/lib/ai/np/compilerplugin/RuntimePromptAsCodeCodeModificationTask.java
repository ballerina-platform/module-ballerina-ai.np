/*
 *  Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org).
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.lib.ai.np.compilerplugin;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ArrayTypeSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.TupleTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.BaseNodeModifier;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ImportOrgNameNode;
import io.ballerina.compiler.syntax.tree.ImportPrefixNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NaturalExpressionNode;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeParser;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TreeModifier;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.openapi.service.mapper.type.TypeMapper;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleId;
import io.ballerina.projects.Package;
import io.ballerina.projects.plugins.ModifierTask;
import io.ballerina.projects.plugins.SourceModifierContext;
import io.ballerina.tools.text.TextDocument;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.OpenAPISchema2JsonSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createToken;
import static io.ballerina.lib.ai.np.compilerplugin.Commons.AI_MODULE_NAME;
import static io.ballerina.lib.ai.np.compilerplugin.Commons.BALLERINA_ORG_NAME;
import static io.ballerina.projects.util.ProjectConstants.EMPTY_STRING;

/**
 * Code modification task to replace runtime prompt as code external functions with np:call.
 *
 * @since 0.3.0
 */
public class RuntimePromptAsCodeCodeModificationTask implements ModifierTask<SourceModifierContext> {

    private static final Token COLON = createToken(SyntaxKind.COLON_TOKEN);
    private static final String SCHEMA_ANNOTATION_IDENTIFIER = "JsonSchema";
    private static final String STRING = "string";
    private static final String BYTE = "byte";
    private static final String NUMBER = "number";

    private final ModifierData modifierData;
    private final CodeModifier.AnalysisData analysisData;

    RuntimePromptAsCodeCodeModificationTask(CodeModifier.AnalysisData analysisData) {
        this.modifierData = new ModifierData();
        this.analysisData = analysisData;
    }

    @Override
    public void modify(SourceModifierContext modifierContext) {
        Package currentPackage = modifierContext.currentPackage();

        if (// https://github.com/ballerina-platform/ballerina-lang/issues/44020
            // this.analysisData.analysisTaskErrored ||
                modifierContext.compilation().diagnosticResult().errorCount() > 0) {
            return;
        }

        for (ModuleId moduleId : currentPackage.moduleIds()) {
            Module module = currentPackage.module(moduleId);

            for (DocumentId documentId: module.documentIds()) {
                Document document = module.document(documentId);
                generateSchemasForExpectedTypes(document, modifierData, modifierContext, moduleId,
                        this.analysisData.typeMapper);
            }

            for (DocumentId documentId: module.testDocumentIds()) {
                Document document = module.document(documentId);
                generateSchemasForExpectedTypes(document, modifierData, modifierContext, moduleId,
                        this.analysisData.typeMapper);
            }

            for (DocumentId documentId: module.documentIds()) {
                Document document = module.document(documentId);
                Optional<String> aiImportPrefix = getAiImportPrefix(document);
                modifierContext.modifySourceFile(modifyDocument(document, modifierData, aiImportPrefix), documentId);
            }

            for (DocumentId documentId: module.testDocumentIds()) {
                Document document = module.document(documentId);
                Optional<String> aiImportPrefix = getAiImportPrefix(document);
                modifierContext.modifyTestSourceFile(
                        modifyDocument(document, modifierData, aiImportPrefix), documentId);
            }
        }
    }

    private Optional<String> getAiImportPrefix(Document document) {
        ModulePartNode modulePartNode = document.syntaxTree().rootNode();
        for (ImportDeclarationNode importDeclarationNode : modulePartNode.imports()) {
            Optional<ImportOrgNameNode> importOrgNameNode = importDeclarationNode.orgName();
            if (importOrgNameNode.isEmpty() || !BALLERINA_ORG_NAME.equals(importOrgNameNode.get().orgName().text())) {
                continue;
            }

            SeparatedNodeList<IdentifierToken> moduleName = importDeclarationNode.moduleName();
            if (moduleName.size() > 1 || !AI_MODULE_NAME.equals(moduleName.iterator().next().text())) {
                continue;
            }

            Optional<ImportPrefixNode> prefix = importDeclarationNode.prefix();
            return Optional.of(prefix.isEmpty() ? AI_MODULE_NAME : prefix.get().prefix().text());
        }
        return Optional.empty();
    }

    private static void generateSchemasForExpectedTypes(Document document, ModifierData modifierData,
                                                        SourceModifierContext modifierContext,
                                                        ModuleId moduleId,
                                                        TypeMapper typeMapper) {
        ModulePartNode modulePartNode = document.syntaxTree().rootNode();
        NaturalExpressionSchemaGenerator naturalExpressionSchemaGenerator =
                new NaturalExpressionSchemaGenerator(
                        modifierData, modifierContext, moduleId, document, typeMapper);
        modulePartNode.apply(naturalExpressionSchemaGenerator);
    }

    private static TextDocument modifyDocument(Document document, ModifierData modifierData,
                                               Optional<String> aiImportPrefix) {
        TypeDefinitionModifier typeDefinitionModifier =
                new TypeDefinitionModifier(modifierData.typeSchemas, modifierData, aiImportPrefix, document);

        ModulePartNode modulePartNode = document.syntaxTree().rootNode();
        ModulePartNode finalRoot = (ModulePartNode) modulePartNode.apply(typeDefinitionModifier);
        finalRoot = finalRoot.modify(
                updateImports(document, finalRoot, modifierData), finalRoot.members(), finalRoot.eofToken());

        return document.syntaxTree().modifyWith(finalRoot).textDocument();
    }

    private static class NaturalExpressionSchemaGenerator extends BaseNodeModifier {

        private final ModifierData modifierData;
        private final SemanticModel semanticModel;
        private final Document document;
        private final TypeMapper typeMapper;

        NaturalExpressionSchemaGenerator(ModifierData modifierData, SourceModifierContext modifierContext,
                                         ModuleId moduleId, Document document, TypeMapper typeMapper) {
            this.modifierData = modifierData;
            this.semanticModel = modifierContext.compilation().getSemanticModel(moduleId);
            this.document = document;
            this.typeMapper = typeMapper;
        }

        @Override
        public NaturalExpressionNode transform(NaturalExpressionNode naturalExpressionNode) {
            Optional<TypeSymbol> typeSymbol =
                    semanticModel.expectedType(document, naturalExpressionNode.lineRange().startLine());
            typeSymbol.ifPresent(symbol -> populateTypeSchema(symbol, this.typeMapper,
                    this.modifierData.typeSchemas, this.semanticModel.types().ANYDATA));
            return naturalExpressionNode;
        }
    }

    private static class TypeDefinitionModifier extends TreeModifier {

        private final Map<String, String> typeSchemas;
        private final ModifierData modifierData;
        private final Optional<String> aiImportPrefix;
        private final Document document;

        TypeDefinitionModifier(Map<String, String> typeSchemas, ModifierData modifierData,
                               Optional<String> aiImportPrefix, Document document) {
            this.typeSchemas = typeSchemas;
            this.modifierData = modifierData;
            this.aiImportPrefix = aiImportPrefix;
            this.document = document;
        }

        @Override
        public TypeDefinitionNode transform(TypeDefinitionNode typeDefinitionNode) {
            String typeName = typeDefinitionNode.typeName().text();

            if (!this.typeSchemas.containsKey(typeName)) {
                return typeDefinitionNode;
            }

            if (this.aiImportPrefix.isEmpty()) {
                modifierData.documentsRequiringAiImport.add(this.document);
            }

            MetadataNode updatedMetadataNode =
                                updateMetadata(typeDefinitionNode, typeSchemas.get(typeName),
                                               this.aiImportPrefix.orElse(AI_MODULE_NAME));
            return typeDefinitionNode.modify().withMetadata(updatedMetadataNode).apply();
        }

        private MetadataNode updateMetadata(TypeDefinitionNode typeDefinitionNode, String schema, String aiPrefix) {
            MetadataNode metadataNode = getMetadataNode(typeDefinitionNode);
            NodeList<AnnotationNode> updatedAnnotations =
                                            updateAnnotations(metadataNode.annotations(), schema, aiPrefix);
            return metadataNode.modify().withAnnotations(updatedAnnotations).apply();
        }
    }

    public static MetadataNode getMetadataNode(TypeDefinitionNode typeDefinitionNode) {
        return typeDefinitionNode.metadata().orElseGet(() -> {
            NodeList<AnnotationNode> annotations = NodeFactory.createNodeList();
            return NodeFactory.createMetadataNode(null, annotations);
        });
    }

    private static NodeList<AnnotationNode> updateAnnotations(NodeList<AnnotationNode> currentAnnotations,
                                                              String jsonSchema, String aiPrefix) {
        NodeList<AnnotationNode> updatedAnnotations = NodeFactory.createNodeList();

        if (currentAnnotations.isEmpty()) {
            updatedAnnotations = updatedAnnotations.add(getSchemaAnnotation(jsonSchema, aiPrefix));
        }

        return updatedAnnotations;
    }

    public static AnnotationNode getSchemaAnnotation(String jsonSchema, String aiPrefix) {
        String configIdentifierString = aiPrefix + COLON.text() + SCHEMA_ANNOTATION_IDENTIFIER;
        IdentifierToken identifierToken = NodeFactory.createIdentifierToken(configIdentifierString);

        return NodeFactory.createAnnotationNode(
                NodeFactory.createToken(SyntaxKind.AT_TOKEN),
                NodeFactory.createSimpleNameReferenceNode(identifierToken),
                getAnnotationExpression(jsonSchema)
        );
    }

    public static MappingConstructorExpressionNode getAnnotationExpression(String jsonSchema) {
        return (MappingConstructorExpressionNode) NodeParser.parseExpression(jsonSchema);
    }

    private static boolean containsBallerinaAIImport(NodeList<ImportDeclarationNode> imports) {
        for (ImportDeclarationNode importDeclarationNode : imports) {
            Optional<ImportOrgNameNode> importOrgNameNode = importDeclarationNode.orgName();
            if (importOrgNameNode.isPresent() && importOrgNameNode.get().orgName().text().equals(BALLERINA_ORG_NAME)
                    && importDeclarationNode.moduleName().get(0).text().equals(AI_MODULE_NAME)) {
                return true;
            }
        }
        return false;
    }

    private static NodeList<ImportDeclarationNode> updateImports(Document document, ModulePartNode modulePartNode,
                                                                 ModifierData modifierData) {
        NodeList<ImportDeclarationNode> imports = modulePartNode.imports();

        if (containsBallerinaAIImport(imports)) {
            return imports;
        }

        if (modifierData.documentsRequiringAiImport.contains(document)) {
            return imports.add(createImportDeclarationForAiModule());
        }
        return imports;
    }

    private static ImportDeclarationNode createImportDeclarationForAiModule() {
        // TODO: handle the `ai` prefix already being used
        return NodeParser.parseImportDeclaration(String.format("import %s/%s;", BALLERINA_ORG_NAME, AI_MODULE_NAME));
    }

    private static void populateTypeSchema(TypeSymbol memberType, TypeMapper typeMapper,
                                           Map<String, String> typeSchemas, TypeSymbol anydataType) {
        switch (memberType) {
            case TypeReferenceTypeSymbol typeReference -> {
                if (!typeReference.subtypeOf(anydataType)) {
                    return;
                }
                typeSchemas.put(typeReference.definition().getName().get(),
                        getJsonSchema(typeMapper.getSchema(typeReference)));
            }
            case ArrayTypeSymbol arrayType ->
                            populateTypeSchema(arrayType.memberTypeDescriptor(), typeMapper, typeSchemas, anydataType);
            case TupleTypeSymbol tupleType ->
                    tupleType.members().forEach(member ->
                            populateTypeSchema(member.typeDescriptor(), typeMapper, typeSchemas, anydataType));
            case RecordTypeSymbol recordType ->
                    recordType.fieldDescriptors().values().forEach(field ->
                            populateTypeSchema(field.typeDescriptor(), typeMapper, typeSchemas, anydataType));
            case UnionTypeSymbol unionTypeSymbol -> unionTypeSymbol.memberTypeDescriptors().forEach(member ->
                            populateTypeSchema(member, typeMapper, typeSchemas, anydataType));
            default -> { }
        }
    }


    @SuppressWarnings("rawtypes")
    private static String getJsonSchema(Schema schema) {
        modifySchema(schema);
        OpenAPISchema2JsonSchema openAPISchema2JsonSchema = new OpenAPISchema2JsonSchema();
        openAPISchema2JsonSchema.process(schema);
        String newLineRegex = "\\R";
        String jsonCompressionRegex = "\\s*([{}\\[\\]:,])\\s*";
        return Json.pretty(schema.getJsonSchema())
                .replaceAll(newLineRegex, EMPTY_STRING)
                .replaceAll(jsonCompressionRegex, "$1");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void modifySchema(Schema schema) {
        if (schema == null) {
            return;
        }
        modifySchema(schema.getItems());
        modifySchema(schema.getNot());

        Map<String, Schema> properties = schema.getProperties();
        if (properties != null) {
            properties.values().forEach(RuntimePromptAsCodeCodeModificationTask::modifySchema);
        }

        List<Schema> allOf = schema.getAllOf();
        if (allOf != null) {
            schema.setType(null);
            allOf.forEach(RuntimePromptAsCodeCodeModificationTask::modifySchema);
        }

        List<Schema> anyOf = schema.getAnyOf();
        if (anyOf != null) {
            schema.setType(null);
            anyOf.forEach(RuntimePromptAsCodeCodeModificationTask::modifySchema);
        }

        List<Schema> oneOf = schema.getOneOf();
        if (oneOf != null) {
            schema.setType(null);
            oneOf.forEach(RuntimePromptAsCodeCodeModificationTask::modifySchema);
        }

        // Override default ballerina byte to json schema mapping
        if (BYTE.equals(schema.getFormat()) && STRING.equals(schema.getType())) {
            schema.setFormat(null);
            schema.setType(NUMBER);
        }
        removeUnwantedFields(schema);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void removeUnwantedFields(Schema schema) {
        schema.setSpecVersion(null);
        schema.setSpecVersion(null);
        schema.setContains(null);
        schema.set$id(null);
        schema.set$schema(null);
        schema.set$anchor(null);
        schema.setExclusiveMaximumValue(null);
        schema.setExclusiveMinimumValue(null);
        schema.setDiscriminator(null);
        schema.setTitle(null);
        schema.setMaximum(null);
        schema.setExclusiveMaximum(null);
        schema.setMinimum(null);
        schema.setExclusiveMinimum(null);
        schema.setMaxLength(null);
        schema.setMinLength(null);
        schema.setMaxItems(null);
        schema.setMinItems(null);
        schema.setMaxProperties(null);
        schema.setMinProperties(null);
        schema.setAdditionalProperties(null);
        schema.setAdditionalProperties(null);
        schema.set$ref(null);
        schema.set$ref(null);
        schema.setReadOnly(null);
        schema.setWriteOnly(null);
        schema.setExample(null);
        schema.setExample(null);
        schema.setExternalDocs(null);
        schema.setDeprecated(null);
        schema.setPrefixItems(null);
        schema.setContentEncoding(null);
        schema.setContentMediaType(null);
        schema.setContentSchema(null);
        schema.setPropertyNames(null);
        schema.setUnevaluatedProperties(null);
        schema.setMaxContains(null);
        schema.setMinContains(null);
        schema.setAdditionalItems(null);
        schema.setUnevaluatedItems(null);
        schema.setIf(null);
        schema.setElse(null);
        schema.setThen(null);
        schema.setDependentSchemas(null);
        schema.set$comment(null);
        schema.setExamples(null);
        schema.setExtensions(null);
        schema.setConst(null);
    }

    static final class ModifierData {
        Set<Document> documentsRequiringAiImport = new HashSet<>(0);
        Map<String, String> typeSchemas = new HashMap<>();
    }
}
