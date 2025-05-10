# module-ballerina-np

This is the library module for natural programming - specifically the Code for AI component of natural programming, which simplifies working with large language models (LLMs) and allows LLM interactions to be strongly typed. This is enabled via natural expressions in Ballerina. 

For more information about natural expressions and natural programming, see [Natural Language is Code: A hybrid approach with Natural Programming](https://blog.ballerina.io/posts/2025-04-26-introducing-natural-programming/).

To get started, see [Work with Large Language Models (LLMs) using natural expressions](https://ballerina.io/learn/work-with-llms-using-natural-expressions/).

For a set of examples, see [Examples](./examples/).

## Overview

A natural expression in Ballerina is transformed into a call to the `callLlm` function in this module, via a compiler plugin.

For example, 

```ballerina
public isolated function reviewBlog(Blog blog) returns Review|error => natural {
    You are an expert content reviewer for a blog site that 
        categorizes posts under the following categories: ${categories}

        Your tasks are:
        1. Suggest a suitable category for the blog from exactly the specified categories. 
           If there is no match, use null.

        2. Rate the blog post on a scale of 1 to 10 based on the following criteria:
        - **Relevance**: How well the content aligns with the chosen category.
        - **Depth**: The level of detail and insight in the content.
        - **Clarity**: How easy it is to read and understand.
        - **Originality**: Whether the content introduces fresh perspectives or ideas.
        - **Language Quality**: Grammar, spelling, and overall writing quality.

        Here is the blog post content:

        Title: ${blog.title}
        Content: ${blog.content}
};
```

becomes

```ballerina
import ballerina/np;

public isolated function reviewBlog(Blog blog) returns Review|error => np:callLlm(
    `You are an expert content reviewer for a blog site that 
        categorizes posts under the following categories: ${categories}

        Your tasks are:
        1. Suggest a suitable category for the blog from exactly the specified categories. 
           If there is no match, use null.

        2. Rate the blog post on a scale of 1 to 10 based on the following criteria:
        - **Relevance**: How well the content aligns with the chosen category.
        - **Depth**: The level of detail and insight in the content.
        - **Clarity**: How easy it is to read and understand.
        - **Originality**: Whether the content introduces fresh perspectives or ideas.
        - **Language Quality**: Grammar, spelling, and overall writing quality.

        Here is the blog post content:

        Title: ${blog.title}
        Content: ${blog.content}`);
```

The `np:callLlm` function is dependently-typed, to allow natural expressions to be dependently-typed.

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

As an open source project, Ballerina welcomes contributions from the community. 

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

