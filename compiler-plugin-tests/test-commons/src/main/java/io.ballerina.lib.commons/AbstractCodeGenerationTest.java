package io.ballerina.lib.commons;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.projects.BuildOptions;
import io.ballerina.projects.JBallerinaBackend;
import io.ballerina.projects.JvmTarget;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.SingleFileProject;
import io.ballerina.projects.environment.Environment;
import io.ballerina.projects.environment.EnvironmentBuilder;
import io.ballerina.projects.util.ProjectUtils;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.testng.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static io.ballerina.projects.util.ProjectConstants.BALLERINA_HOME;

/**
 * Abstract base class for compile-time code generation tests.
 */
public abstract class AbstractCodeGenerationTest {
    protected static final Path DISTRIBUTION_PATH = Paths.get("")
            .toAbsolutePath().resolve("../../target/ballerina-runtime").normalize();
    protected static final Path RESOURCE_DIRECTORY = Paths.get("src", "test", "resources").toAbsolutePath();
    protected static final Path SERVER_RESOURCES = RESOURCE_DIRECTORY.resolve("server-resources");
    private static final String TARGET = "target";
    protected final MockWebServer server = new MockWebServer();

    protected void enqueueResponse(String directory, String file, int responseCode) throws IOException {
        server.enqueue(new MockResponse()
                .setBody(getCodeMockResponse(directory, file))
                .setResponseCode(responseCode));
    }

    protected void enqueueResponse(String directory, String file) throws IOException {
        enqueueResponse(directory, file, 200);
    }

    protected Project loadPackageProject(Path path) {
        return loadProject(path, false);
    }

    protected Project loadSingleBalFileProject(Path path) {
        return loadProject(path, true);
    }

    protected String buildAndRunExecutable(Project project, Path jarPath) throws IOException {
        JBallerinaBackend jBallerinaBackend =
                JBallerinaBackend.from(project.currentPackage().getCompilation(), JvmTarget.JAVA_21);
        jBallerinaBackend.emit(JBallerinaBackend.OutputType.EXEC, jarPath);

        ProcessBuilder builder = new ProcessBuilder("java", "-jar", jarPath.toString());
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.readLine();
        }
    }

    protected Path getJarPathForSingleBalFile(Path projectPath) {
        Path jarPath = projectPath.resolve("single_file_project.jar");
        jarPath.toFile().deleteOnExit();
        return jarPath;
    }

    protected Path getJarPath(String projectPath, Project naturalExprProject) {
        return RESOURCE_DIRECTORY
                .resolve(projectPath)
                .resolve(TARGET)
                .resolve(ProjectUtils.getExecutableName(naturalExprProject.currentPackage()));
    }

    private Project loadProject(Path path, boolean isSingleBalFileMode) {
        Environment environment = EnvironmentBuilder.getBuilder().setBallerinaHome(DISTRIBUTION_PATH).build();
        ProjectEnvironmentBuilder projectEnvironmentBuilder = ProjectEnvironmentBuilder.getBuilder(environment);
        BuildOptions buildOptions = BuildOptions.builder().setExperimental(true).build();
        return isSingleBalFileMode ?
                SingleFileProject.load(projectEnvironmentBuilder, path, buildOptions) :
                BuildProject.load(projectEnvironmentBuilder, path, buildOptions);
    }

    protected String getCodeMockResponse(String directory, String file) throws IOException {
        return getFileContent(SERVER_RESOURCES.resolve(directory).resolve(file));
    }

    private String getFileContent(Path path) throws IOException {
        return String.join("\n", Files.readAllLines(path));
    }

    protected JsonObject getExpectedPayload(String directory, String file) throws IOException {
        try (FileReader reader = new FileReader(
                SERVER_RESOURCES.resolve(directory).resolve(file).toString(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    protected void assertRequest(String path, String directory, String file) throws InterruptedException, IOException {
        assertRequest(path, getExpectedPayload(directory, file), "Bearer not-a-real-token");
    }

    protected void assertRequest(String path, String directory, String file, String expectedAuthHeader)
                throws InterruptedException, IOException {
        assertRequest(path, getExpectedPayload(directory, file), expectedAuthHeader);
    }

    protected void assertRequest(String path, JsonObject expectedPayload, String expectedAuthHeader)
            throws InterruptedException {
        RecordedRequest recordedRequest = server.takeRequest(5, TimeUnit.SECONDS); // Increased timeout
        Assert.assertNotNull(recordedRequest, "Request was not sent to the mock server.");
        Assert.assertEquals(recordedRequest.getPath(), path);
        if (expectedAuthHeader != null) {
            Assert.assertEquals(recordedRequest.getHeader("Authorization"), expectedAuthHeader);
        }
        JsonObject actualPayload = JsonParser.parseString(
                recordedRequest.getBody().readUtf8().replace("\\r\\n", "\\n")).getAsJsonObject();
        Assert.assertEquals(actualPayload, expectedPayload);
    }

    protected void validateGeneratedCodeAndDeleteGeneratedDir(String dirName, String generatedFileName)
            throws IOException {
        Path generatedDirPath = RESOURCE_DIRECTORY.resolve(dirName).resolve("generated");
        Assert.assertTrue(Files.isDirectory(generatedDirPath));

        Path generatedFuncFilePath = generatedDirPath.resolve(generatedFileName);
        Assert.assertTrue(Files.isRegularFile(generatedFuncFilePath));
        String actualCode = getFileContent(generatedFuncFilePath);
        String expectedCode = getFileContent(RESOURCE_DIRECTORY
                .resolve(dirName).resolve("expected").resolve("expected_function_source.bal"));
        Assert.assertEquals(actualCode.trim().replace("\r\n", "\n"),
                expectedCode.trim().replace("\r\n", "\n"));

        PrintStream out = System.out;
        boolean deleteRes1 = generatedFuncFilePath.toFile().delete();
        boolean deleteRes2 = generatedDirPath.toFile().delete();
        if (!deleteRes1 || !deleteRes2) {
            out.println("Failed to delete file and/or directory");
        }
    }
}
