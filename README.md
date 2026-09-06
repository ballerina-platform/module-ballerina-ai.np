# module-ballerina-ai.np

## Overview

This module provides compile-time code generation for natural programming, including generating JSON schema for types used with natural expressions.

For more information about natural programming, see [Natural Language is Code: A hybrid approach with Natural Programming](https://blog.ballerina.io/posts/2025-04-26-introducing-natural-programming/).

### Sample of compile-time code generation

```ballerina
const int COUNT = 25;

type Employee record {
    string name;
    int id;
    decimal salary;
    Department department;
};

type Department record {
    string name;
    int[] employees;
};

public function main() returns error? {
    Employee[] employees = const natural {
        Give me a list of ${COUNT} employees.
    
        Make sure to reflect real-world data. For example, make sure the data is proportionate to 
        the department sizes and IDs of employees from the same department aren't always contiguous.
    };
    
    printEmployeeDataByDepartment(employees);
}

function printEmployeeDataByDepartment(Employee[] employees) = @natural:code {
    prompt: string `Print the employee data grouped by department, in the following format.
    
    Department: <Department Name>
    =================================================
    Employee ID: <Employee 1 ID>, Name: <Employee 1 Name>, Salary: <Employee 1 Salary>
    ...
    Employee ID: <Employee n ID>, Name: <Employee n Name>, Salary: <Employee n Salary>
    `
} external;
```

### Configuring the Copilot for compile-time code generation 

The `BAL_CODEGEN_URL` and `BAL_CODEGEN_TOKEN` environment variables need to be set.

You can currently use the configuration generated via the `Ballerina: Configure default WSO2 model provider` VS Code command as the values.

## Issues and projects 

Issues and Projects tabs are disabled for this repository as this is part of the Ballerina Standard Library. To report bugs, request new features, start new discussions, view project boards, etc. please visit Ballerina Standard Library [parent repository](https://github.com/ballerina-platform/ballerina-standard-library). 

This repository only contains the source code for the package.

## Build from the source

### Set Up the prerequisites

1. Download and install the Java SE Development Kit (JDK) version 21.
   
   * [OpenJDK](https://adoptium.net/)
   
        > **Note:** Set the JAVA_HOME environment variable to the path name of the directory into which you installed JDK.

2. Export GitHub Personal access token with read package permissions as follows,

        export packageUser=<Username>
        export packagePAT=<Personal access token>

### Build the source

Execute the commands below to build from source.

1. To build the library:

    ```
    ./gradlew clean build
    ```
   
2. To run the integration tests:

    ```
    ./gradlew clean test
    ```

3. To run a group of tests

    ```
    ./gradlew clean test -Pgroups=<test_group_names>
    ```

4. To build the package without the tests:

    ```
    ./gradlew clean build -x test
    ```
   
5. To debug the tests:

    ```
    ./gradlew clean test -Pdebug=<port>
    ```
   
6. To debug with Ballerina language:

    ```
    ./gradlew clean build -PbalJavaDebug=<port>
    ```

7. Publish the generated artifacts to the local Ballerina central repository:

    ```
    ./gradlew clean build -PpublishToLocalCentral=true
    ```

8. Publish the generated artifacts to the Ballerina central repository:

    ```
    ./gradlew clean build -PpublishToCentral=true
    ```

## Contribute to Ballerina

As an open-source project, Ballerina welcomes contributions from the community. 

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

