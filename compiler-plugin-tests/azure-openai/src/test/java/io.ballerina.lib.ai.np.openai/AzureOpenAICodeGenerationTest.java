package io.ballerina.lib.ai.np.openai;

import io.ballerina.lib.commons.AbstractCodeGenerationTest;
import io.ballerina.projects.Project;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static io.ballerina.projects.util.ProjectConstants.BALLERINA_HOME;

public class AzureOpenAICodeGenerationTest extends AbstractCodeGenerationTest {
    private static final String API_PATH = "/openai/deployments/gpt-4o-mini/chat/completions?api-version=2024-06-30";
    @BeforeSuite
    void setUp() throws IOException {
        server.start(8080);
        System.setProperty(BALLERINA_HOME, DISTRIBUTION_PATH.toAbsolutePath().toString());
    }

    @AfterSuite
    void tearDown() throws IOException {
        server.shutdown();
        System.clearProperty(BALLERINA_HOME);
    }

    @Test
    public void testConstNaturalExpressionsInProject() throws IOException, InterruptedException {
        enqueueResponse("const-natural-expressions", "azure_openai_const_natural_expr_response.json");

        final Path projectPath = RESOURCE_DIRECTORY
                .resolve("const-natural-expressions")
                .resolve("const-natural-expressions-project");
        final Project naturalExprProject = loadPackageProject(projectPath);
        naturalExprProject.currentPackage().runCodeGenAndModifyPlugins();

        assertRequest(API_PATH, "const-natural-expressions", "azure_openai_const_natural_expr_proj_request.json",
                "not-a-real-azure-openai-token", "api-key");
        Assert.assertEquals(
                buildAndRunExecutable(naturalExprProject, getJarPath(projectPath.toString(), naturalExprProject)),
                "[1234,1456,1678,1890,1357,1579,1246,1468,1975,1753]");
    }

    @Test
    public void testConstNaturalExpressionsInProjectWithRepair() throws IOException, InterruptedException {
        String resDir = "const-natural-expressions/const-natural-expressions-with-validation-failure";
        enqueueResponse(resDir, "azure_openai_const_natural_expr_with_validation_failure_code_response.json");
        enqueueResponse(resDir, "azure_openai_const_natural_expr_with_validation_failure_repair_response.json");

        final Path projectPath = RESOURCE_DIRECTORY
                .resolve("const-natural-expressions")
                .resolve("const_natural_expr_with_validation");
        final Project naturalExprProject = loadPackageProject(projectPath);
        naturalExprProject.currentPackage().runCodeGenAndModifyPlugins();

        assertRequest(API_PATH, resDir, "azure_openai_const_natural_expr_with_validation_failure_code_request.json",
                "not-a-real-azure-openai-token", "api-key");
        assertRequest(API_PATH, resDir, "azure_openai_const_natural_expr_with_validation_failure_repair_request.json",
                "not-a-real-azure-openai-token", "api-key");

        Assert.assertEquals(
                buildAndRunExecutable(naturalExprProject, getJarPath(projectPath.toString(), naturalExprProject)),
                "[2,-2,99998,99996,\"a\",\"a\",2,1,6,3]");
    }

    @Test
    public void testConstNaturalExpressionsInSingleBalFile() throws IOException, InterruptedException {
        enqueueResponse("const-natural-expressions", "azure_openai_const_natural_expr_response.json");

        final Path dirPath = RESOURCE_DIRECTORY.resolve("const-natural-expressions");
        final Project naturalExprProject = loadSingleBalFileProject(dirPath.resolve("const_natural_expressions.bal"));
        naturalExprProject.currentPackage().runCodeGenAndModifyPlugins();

        assertRequest(API_PATH, "const-natural-expressions", "azure_openai_const_natural_expr_single_bal_file_request.json",
                "not-a-real-azure-openai-token", "api-key");

        Assert.assertEquals(
                buildAndRunExecutable(naturalExprProject, getJarPathForSingleBalFile(dirPath)),
                "[1234,1456,1678,1890,1357,1579,1246,1468,1975,1753]");
    }

    @Test
    public void testCodeFunction() throws IOException, InterruptedException {
        String resDir = "code-function-projects/code-function";
        enqueueResponse(resDir, "azure_openai_code_function_code_response.json");
        enqueueResponse(resDir, "azure_openai_code_function_repair_response.json");

        final Path projectPath = RESOURCE_DIRECTORY.resolve(resDir);
        final Project naturalExprProject = loadPackageProject(projectPath);
        naturalExprProject.currentPackage().runCodeGenAndModifyPlugins();

        assertRequest(API_PATH, resDir, "azure_openai_code_function_code_request.json",
                "not-a-real-azure-openai-token", "api-key");
        assertRequest(API_PATH, resDir, "azure_openai_code_function_repair_request.json",
                "not-a-real-azure-openai-token", "api-key");

        Assert.assertNull(server.takeRequest(3L, TimeUnit.SECONDS)); // No third request

        validateGeneratedCodeAndDeleteGeneratedDir(resDir, "sortEmployees_np_generated.bal");

        Assert.assertEquals(
                buildAndRunExecutable(naturalExprProject, getJarPath(projectPath.toString(), naturalExprProject)),
                "[{\"name\":\"David\",\"salary\":70000},{\"name\":\"Bob\",\"salary\":60000}," +
                        "{\"name\":\"Alice\",\"salary\":50000},{\"name\":\"Charlie\",\"salary\":50000}]");
    }

    @Test
    public void testCodeFunctionWithValidation() throws IOException, InterruptedException {
        String resDir = "code-function-projects/code-function-with-validation-failure";
        enqueueResponse(resDir, "azure_openai_code_function_with_validation_code_response.json");
        enqueueResponse(resDir, "azure_openai_code_function_with_validation_repair_response.json");

        final Path projectPath = RESOURCE_DIRECTORY.resolve(resDir);
        final Project naturalExprProject = loadPackageProject(projectPath);
        naturalExprProject.currentPackage().runCodeGenAndModifyPlugins();

        assertRequest(API_PATH, resDir, "azure_openai_code_function_with_validation_code_request.json",
                "not-a-real-azure-openai-token", "api-key");
        assertRequest(API_PATH, resDir, "azure_openai_code_function_with_validation_repair_request.json",
                "not-a-real-azure-openai-token", "api-key");

        validateGeneratedCodeAndDeleteGeneratedDir(resDir, "calculateTotalPrice_np_generated.bal");

        Assert.assertEquals(
                buildAndRunExecutable(naturalExprProject, getJarPath(projectPath.toString(), naturalExprProject)),
                "Total price: 110.54556");
    }
}
