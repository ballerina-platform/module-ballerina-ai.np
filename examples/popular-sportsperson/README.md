# Get popular sportsperson

Use a natural function to find a popular sportsperson who has the specified name segment in their name and was born in the decade starting from the specified year, and retrieve information about them.

## Steps

1. Define a record type representing the information required about the sportsperson.

    ```ballerina
    # Represents a person who plays a sport.
    type SportsPerson record {|
        # First name of the person
        string firstName;
        # Last name of the person
        string lastName;
        # Year the person was born
        int yearOfBirth;
        # Sport that the person plays
        string sport;
    |};
    ```

2. Define a natural function to retrieve the information.

    ```ballerina
    import ballerina/ai;
   
    final ai:ModelProvider model = check ai:getDefaultModelProvider();

    function getPopularSportsPerson(string nameSegment, int decadeStart) 
            returns SportsPerson|error? => natural (model) {
        Who is a popular sportsperson that was born in the decade starting 
        from ${decadeStart} with ${nameSegment} in their name?
    };
    ```

    - Specify the prompt in natural language in a natural expression. Note how interpolations can refer to in-scope symbols.
    - Use the type defined above (`SportsPerson`) in the return type along with `error` (to allow for failures when calling the LLM or attempting to bind the response to the target type) and optionally nil (`?`, representing `null` to allow for no match).

3. Call the function with arguments for `nameSegment` and `decadeStart` in the `main` function and access the required fields from the returned `SportsPerson` value.

    ```ballerina
    public function main() returns error? {
        string nameSegment = "Simone";
        int decadeStart = 1990;

        SportsPerson|error person = getPopularSportsPerson(nameSegment, decadeStart);
        if person is SportsPerson {
            io:println("Full name: ", person.firstName, " ", person.lastName);
            io:println("Born in: ", person.yearOfBirth);
            io:println("Sport: ", person.sport);
        } else {
            io:println("Error finding matching sportsperson", person);
        }
    }
    ```

4. Provide configuration for the default model provider via the Config.toml file. You can use the default model made available via WSO2 Copilot. Log in to WSO2 Copilot, open up the VS Code command palette (`Ctrl + Shift + P` or `command + shift + P`), and run `Configure Default Model for Natural Functions`. This will add configuration for the default model into the Config.toml file. Please note that this will require VS Code being open in the relevant directory.

    You can use your own keys and configuration for providers such as OpenAI or Azure OpenAI by using a value of the model provider from the relevant `ballerinax/ai.<provider>` package, instead of `ai:getDefaultModelProvider()`.

5. Run the sample using the Ballerina run command passing the `--experimental` option.

    ```cmd
    $ bal run --experimental popular_sportsperson.bal
    Compiling source
        popular_sportsperson.bal

    Running executable

    Full name: Simone Biles
    Born in: 1997
    Sport: Gymnastics
    ```
