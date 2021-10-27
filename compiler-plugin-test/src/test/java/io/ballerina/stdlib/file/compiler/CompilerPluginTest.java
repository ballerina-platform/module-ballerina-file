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

import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.environment.Environment;
import io.ballerina.projects.environment.EnvironmentBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * File compiler plugin tests.
 */
public class CompilerPluginTest {

    private static final Path RESOURCE_DIRECTORY = Paths.get("src", "test", "resources", "test-src")
            .toAbsolutePath();
    private static final Path DISTRIBUTION_PATH = Paths.get("../", "target", "ballerina-runtime")
            .toAbsolutePath();

    @Test
    public void testCompilerPlugin() {
        Package currentPackage = loadPackage("package_01");
        PackageCompilation compilation = currentPackage.getCompilation();

        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test
    public void testCompilerPluginWithInvalidParams() {
        Package currentPackage = loadPackage("package_02");
        PackageCompilation compilation = currentPackage.getCompilation();
        String errMsg = "the remote function should only contain file:FileEvent parameter";
        String errMsg1 = "invalid function name `onEdit`, file listener only supports `onCreate`, `onModify` and " +
                "`onDelete` remote functions";
        String errMsg2 = "the remote function should only contain file:FileEvent parameter";
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 3);
        Object[] errors = diagnosticResult.errors().toArray();
        Assert.assertTrue(errors[0].toString().contains(errMsg));
        Assert.assertTrue(errors[1].toString().contains(errMsg1));
        Assert.assertTrue(errors[2].toString().contains(errMsg2));
    }

    @Test
    public void testCompilerPluginWithInvalidParamsType() {
        Package currentPackage = loadPackage("package_03");
        PackageCompilation compilation = currentPackage.getCompilation();
        String errMsg = "invalid parameter type `string` provided for remote function. Only file:FileEvent " +
                "is allowed as the parameter type";
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Assert.assertTrue(diagnosticResult.errors().stream().anyMatch(
                diagnostic -> diagnostic.toString().contains(errMsg)));
    }

    @Test
    public void testCompilerPluginWithEmptyFunction() {
        Package currentPackage = loadPackage("package_04");
        PackageCompilation compilation = currentPackage.getCompilation();
        String errMsg = "at least a single remote function required in the service";
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Assert.assertTrue(diagnosticResult.errors().stream().anyMatch(
                diagnostic -> diagnostic.toString().contains(errMsg)));
    }

    @Test
    public void testCompilerPluginWithRemoteFunc() {
        Package currentPackage = loadPackage("package_05");
        PackageCompilation compilation = currentPackage.getCompilation();
        String errMsg1 = "missing remote keyword in the remote function `onCreate`";
        String errMsg2 = "missing remote keyword in the remote function `onModify`";
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 2);
        Object[] errors = diagnosticResult.errors().toArray();
        Assert.assertTrue(errors[0].toString().contains(errMsg1));
        Assert.assertTrue(errors[1].toString().contains(errMsg2));
    }

    @Test
    public void testCompilerPluginWithRemoteFunc1() {
        Package currentPackage = loadPackage("package_09");
        PackageCompilation compilation = currentPackage.getCompilation();
        String errMsg = "invalid token 'remote'";
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Object[] errors = diagnosticResult.errors().toArray();
        Assert.assertTrue(errors[0].toString().contains(errMsg));
    }

    @Test
    public void testCompilerPluginWithListener() {
        Package currentPackage = loadPackage("package_06");
        PackageCompilation compilation = currentPackage.getCompilation();
        String errMsg = "listener variable incompatible types: 'localFolder' is not a Listener object";
        String errMsg1 = "unknown type 'Listener'";
        String errMsg2 = "cannot infer type of the object from '(other|error)'";
        String errMsg4 = "invalid listener attachment";
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 5);
        Object[] errors = diagnosticResult.errors().toArray();
        Assert.assertTrue(errors[0].toString().contains(errMsg));
        Assert.assertTrue(errors[1].toString().contains(errMsg1));
        Assert.assertTrue(errors[2].toString().contains(errMsg2));
        Assert.assertFalse(errors[3].toString().isEmpty());
        Assert.assertTrue(errors[4].toString().contains(errMsg4));
    }

    @Test
    public void testCompilerPluginWithReturn() {
        Package currentPackage = loadPackage("package_07");
        String errMsg = "return types are not allowed in the remote function `onCreate`";
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Assert.assertTrue(diagnosticResult.errors().stream().anyMatch(
                diagnostic -> diagnostic.toString().contains(errMsg)));
    }

    @Test
    public void testCompilerPluginWithDummyAndMultipleService() {
        Package currentPackage = loadPackage("package_08");
        String errMsg = "return types are not allowed in the remote function `onCreate`";
        String errMsg1 = "return types are not allowed in the remote function `onCreate`";
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 2);
        Object[] errors = diagnosticResult.errors().toArray();
        Assert.assertTrue(errors[0].toString().contains(errMsg));
        Assert.assertTrue(errors[1].toString().contains(errMsg1));
    }

    private Package loadPackage(String path) {
        Path projectDirPath = RESOURCE_DIRECTORY.resolve(path);
        BuildProject project = BuildProject.load(getEnvironmentBuilder(), projectDirPath);
        return project.currentPackage();
    }

    private static ProjectEnvironmentBuilder getEnvironmentBuilder() {
        Environment environment = EnvironmentBuilder.getBuilder().setBallerinaHome(DISTRIBUTION_PATH).build();
        return ProjectEnvironmentBuilder.getBuilder(environment);
    }
}
