/*
 * Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.lib.ai.np.compilerplugintests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.lib.commons.AbstractCodeGenerationTest;
import io.ballerina.projects.Project;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static io.ballerina.projects.util.ProjectConstants.BALLERINA_HOME;

/**
 * This class tests compile-time code generation with const natural expressions and @natural:code annotations.
 *
 * @since 0.4.0
 */
public class BallerinaIntelligenceCodeGenerationTest extends AbstractCodeGenerationTest {

    private static final Path DISTRIBUTION_PATH = Paths.get("")
            .toAbsolutePath().resolve("../../target/ballerina-runtime").normalize();
    private static final Path RESOURCE_DIRECTORY = Paths.get("src", "test", "resources").toAbsolutePath();
    private static final String CODE_PATH = "/code";
    private static final String REPAIR_PATH = "/code/repair";

    @BeforeSuite
    void init() throws IOException {
        server.start(8080);
        System.setProperty(BALLERINA_HOME, DISTRIBUTION_PATH.toAbsolutePath().toString());
    }

    @AfterSuite
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void testConstNaturalExpressionsInProject() throws IOException, InterruptedException {
        server.enqueue(new MockResponse()
                .setBody(getCodeMockResponse("const-natural-expressions", "const_natural_expr_response.txt"))
                .setResponseCode(200));

        final Path projectPath = RESOURCE_DIRECTORY
                .resolve("const-natural-expressions")
                .resolve("const-natural-expressions-project");
        final Project naturalExprProject = loadPackageProject(projectPath);
        naturalExprProject.currentPackage().runCodeGenAndModifyPlugins();

        assertRequest(CODE_PATH, "const-natural-expressions", "const_natural_expr_proj_request.json");

        Assert.assertEquals(
                buildAndRunExecutable(naturalExprProject, getJarPath(projectPath.toString(), naturalExprProject)),
                "[1234,1456,1678,1890,1357,1579,1246,1468,1975,1753]");
    }

    @Test
    public void testConstNaturalExpressionsInProjectWithNonConstExpressions() throws IOException, InterruptedException {
        String serviceResourceDirectoryName = "const-natural-expressions" + File.separator +
                "const-natural-expressions-with-validation-failure";
        server.enqueue(new MockResponse()
                .setBody(getCodeMockResponse(
                        serviceResourceDirectoryName, "const_natural_expr_with_validation_failure_code_response.txt"))
                .setResponseCode(200));
        server.enqueue(new MockResponse()
                .setBody(getCodeMockResponse(
                        serviceResourceDirectoryName,
                        "const_natural_expr_with_validation_failure_repair_response.json"))
                .setResponseCode(200));

        final Path projectPath = RESOURCE_DIRECTORY
                .resolve("const-natural-expressions")
                .resolve("const_natural_expr_with_validation");
        final Project naturalExprProject = loadPackageProject(projectPath);
        naturalExprProject.currentPackage().runCodeGenAndModifyPlugins();

        assertRequest(CODE_PATH, serviceResourceDirectoryName,
                "const_natural_expr_with_validation_failure_code_request.json");
        assertRequest(REPAIR_PATH, serviceResourceDirectoryName,
                "const_natural_expr_with_validation_failure_repair_request.json");

        Assert.assertEquals(
                buildAndRunExecutable(naturalExprProject, getJarPath(projectPath.toString(), naturalExprProject)),
                "[2,-2,99998,99996,\"a\",\"a\",2,1,6,3]");
    }

    @Test
    public void testConstNaturalExpressionsInSingleBalFile() throws IOException, InterruptedException {
        server.enqueue(new MockResponse()
                .setBody(getCodeMockResponse("const-natural-expressions", "const_natural_expr_response.txt"))
                .setResponseCode(200));

        final Path dirPath = RESOURCE_DIRECTORY.resolve("const-natural-expressions");
        final Project naturalExprProject = loadSingleBalFileProject(dirPath.resolve("const_natural_expressions.bal"));
        naturalExprProject.currentPackage().runCodeGenAndModifyPlugins();

        assertRequest(CODE_PATH, "const-natural-expressions", "const_natural_expr_single_bal_file_request.json");

        Assert.assertEquals(
                buildAndRunExecutable(naturalExprProject, getJarPathForSingleBalFile(dirPath)),
                "[1234,1456,1678,1890,1357,1579,1246,1468,1975,1753]");
    }

    @Test
    public void testCodeFunction() throws IOException, InterruptedException {
        String serviceResourceDirectoryName = "code-function-projects" + File.separator +
                "code-function";
        server.enqueue(new MockResponse()
                .setBody(getCodeMockResponse(serviceResourceDirectoryName, "code_function_code_response.txt"))
                .setResponseCode(200));
        server.enqueue(new MockResponse()
                .setBody(getCodeMockResponse(serviceResourceDirectoryName, "code_function_repair_response.json"))
                .setResponseCode(200)
                .setHeader("Content-type", "application/json"));

        final Path projectPath = RESOURCE_DIRECTORY.resolve(serviceResourceDirectoryName);
        final Project naturalExprProject = loadPackageProject(projectPath);
        naturalExprProject.currentPackage().runCodeGenAndModifyPlugins();

        assertRequest(CODE_PATH, serviceResourceDirectoryName, "code_function_code_request.json");
        assertRepairRequest(serviceResourceDirectoryName, "code_function_repair_request.json");

        // Validate that a second repair doesn't happen.
        RecordedRequest recordedRequest = server.takeRequest(3L, TimeUnit.SECONDS);
        Assert.assertNull(recordedRequest);

        validateGeneratedCodeAndDeleteGeneratedDir(serviceResourceDirectoryName,
                "sortEmployees_np_generated.bal");

        Assert.assertEquals(
                buildAndRunExecutable(naturalExprProject, getJarPath(projectPath.toString(), naturalExprProject)),
                "[{\"name\":\"David\",\"salary\":70000},{\"name\":\"Bob\",\"salary\":60000}," +
                        "{\"name\":\"Alice\",\"salary\":50000},{\"name\":\"Charlie\",\"salary\":50000}]");
    }

    @Test
    public void testCodeFunctionWithValidation() throws IOException, InterruptedException {
        String serviceResourceDirectoryName = "code-function-projects" + File.separator +
                "code-function-with-validation-failure";
        server.enqueue(new MockResponse()
                .setBody(getCodeMockResponse(serviceResourceDirectoryName,
                        "code_function_with_validation_code_response.txt"))
                .setResponseCode(200));
        server.enqueue(new MockResponse()
                .setBody(getCodeMockResponse(serviceResourceDirectoryName,
                        "code_function_with_validation_repair_response.json"))
                .setResponseCode(200)
                .setHeader("Content-type", "application/json"));

        final Path projectPath = RESOURCE_DIRECTORY.resolve(serviceResourceDirectoryName);
        final Project naturalExprProject = loadPackageProject(projectPath);
        naturalExprProject.currentPackage().runCodeGenAndModifyPlugins();

        assertRequest(CODE_PATH, serviceResourceDirectoryName,
                "code_function_with_validation_code_request.json");
        assertRepairRequest(serviceResourceDirectoryName,
                "code_function_with_validation_repair_request.json");

        validateGeneratedCodeAndDeleteGeneratedDir(serviceResourceDirectoryName,
                "calculateTotalPrice_np_generated.bal");

        Assert.assertEquals(
                buildAndRunExecutable(naturalExprProject, getJarPath(projectPath.toString(), naturalExprProject)),
                "Total price: 110.54556");
    }

    private void assertRepairRequest(String dirName, String repairRequestJsonFileName)
            throws InterruptedException, IOException {
        JsonObject expectedPayload = getExpectedPayload(dirName, repairRequestJsonFileName);
        JsonObject diagnosticsRequest = expectedPayload.getAsJsonObject("diagnosticRequest");
        JsonArray diagnostics = diagnosticsRequest.getAsJsonArray("diagnostics");

        for (int i = 0; i < diagnostics.size(); i++) {
            String message = diagnostics.get(i).getAsJsonObject().getAsJsonPrimitive("message").
                    getAsString().replace("generated/functions",
                            String.format("generated%sfunctions", File.separator));
            JsonObject messageObj = new JsonObject();
            messageObj.addProperty("message", message);
            diagnostics.set(i, messageObj);
        }
        assertRequest(REPAIR_PATH, expectedPayload, "Bearer not-a-real-token");
    }
}
