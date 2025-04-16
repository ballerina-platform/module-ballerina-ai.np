isolated function getExpectedPrompt(string message) returns string {
    if message.startsWith("Rate this blog") {
        return expectedPromptStringForRateBlog;
    }

    if message.startsWith("Please rate this blog") {
        return expectedPromptStringForRateBlog2;
    }

    if message.startsWith("What is 1 + 1?") {
        return expectedPromptStringForRateBlog3;
    }

    if message.startsWith("Tell me") {
        return expectedPromptStringForRateBlog4;
    }

    if message.startsWith("What's the output of the Ballerina code below?") {
        return expectedPromptStringForBalProgram;
    }

    if message.startsWith("Which country") {
        return expectedPromptStringForCountry;
    }

    if message.startsWith("Who is a popular sportsperson") {
        return string `Who is a popular sportsperson that was born in the decade starting
            from 1990 with Simone in their name?.  
        The output should be a JSON value that satisfies the following JSON schema, 
        returned within a markdown snippet enclosed within ${"```json"} and ${"```"}
        
        Schema:
        {"$schema":"https://json-schema.org/draft/2020-12/schema", "type":["object", "null"], "properties":{"firstName":{"type":"string"}, "middleName":{"type":["string", "null"]}, "lastName":{"type":"string"}, "yearOfBirth":{"type":"integer"}, "sport":{"type":"string"}}, "required":["firstName", "middleName", "lastName", "yearOfBirth", "sport"]}`;
    }

    return "INVALID";
}

isolated function getTheMockLLMResult(string message) returns string {
    if message.startsWith("Rate this blog") {
        return "4";
    }

    if message.startsWith("Please rate this blog") {
        return review2.toJsonString();
    }

    if message.startsWith("What is 1 + 1?") {
        return "2";
    }

    if message.startsWith("Tell me") {
        return "[{\"name\":\"Virat Kohli\",\"age\":33},{\"name\":\"Kane Williamson\",\"age\":30}";
    }

    if message.startsWith("What's the output of the Ballerina code below?") {
        return string `The output of the provided Ballerina code calculates the sum of ${"`"}x${"`"} and ${"`"}y${"`"}, which is ${"`"}10 + 20${"`"}. Therefore, the result will be ${"`"}30${"`"}. \n\nHere is the output formatted as a JSON value that satisfies your specified schema:${"\n\n```"}json${"\n"}30${"\n```"}`;
    }

    if message.startsWith("Which country") {
        return "```\n\"Sri Lanka\"\n```";
    }

    if message.startsWith("Who is a popular sportsperson") {
        return "```\n{\"firstName\":\"Simone\",\"middleName\":null,\"lastName\":\"Biles\",\"yearOfBirth\":1997,\"sport\":\"Gymnastics\"}\n```";
    }

    return "INVALID";
}
