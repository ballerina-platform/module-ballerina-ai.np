isolated function getExpectedParameterSchema(string message) returns FunctionParameters {
    if message.startsWith("Rate this blog") {
        return expectedParamterSchemaStringForRateBlog;
    }

    if message.startsWith("Please rate this blog") {
        return expectedParamterSchemaStringForRateBlog2;
    }

    if message.startsWith("What is 1 + 1?") {
        return expectedParamterSchemaStringForRateBlog3;
    }

    if message.startsWith("Tell me") {
        return expectedParamterSchemaStringForRateBlog4;
    }

    if message.startsWith("What's the output of the Ballerina code below?") {
        return expectedParamterSchemaStringForBalProgram;
    }

    if message.startsWith("Which country") {
        return expectedParamterSchemaStringForCountry;
    }

    if message.startsWith("Who is a popular sportsperson") {
        return {"$schema":"https://json-schema.org/draft/2020-12/schema", "type": "object", "properties":{"result":{"type":["object", "null"], "properties":{"firstName":{"type":"string"}, "middleName":{"type":["string", "null"]}, "lastName":{"type":"string"}, "yearOfBirth":{"type":"integer"}, "sport":{"type":"string"}}, "required":["firstName", "middleName", "lastName", "yearOfBirth", "sport"]}}};
    }

    return {};
}

isolated function getTheMockLLMResult(string message) returns string {
    if message.startsWith("Rate this blog") {
        return {result: 4}.toJsonString();
    }

    if message.startsWith("Please rate this blog") {
        return review2.toJsonString();
    }

    if message.startsWith("What is 1 + 1?") {
        return {result: "2"}.toJsonString();
    }

    if message.startsWith("Tell me") {
        return {result: [{"name":"Virat Kohli","age":33},{"name":"Kane Williamson","age":30}]}.toJsonString();
    }

    if message.startsWith("What's the output of the Ballerina code below?") {
        return {result: 30}.toJsonString();
    }

    if message.startsWith("Which country") {
        return {result: "Sri Lanka"}.toJsonString();
    }

    if message.startsWith("Who is a popular sportsperson") {
        return {result: {"firstName":"Simone","middleName":null,"lastName":"Biles","yearOfBirth":1997,"sport":"Gymnastics"}}.toJsonString();
    }

    return "INVALID";
}
