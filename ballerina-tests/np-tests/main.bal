import ballerina/np;

function queryAboutCountry(string query,
    np:Prompt prompt = `Which country is ${query}?`) returns string|error = @np:NaturalFunction external;

function getParsedValues(string[] arr,
    np:Prompt prompt = `For each string value in the given array if the value can be parsed
        as an integer give an integer, if not give the same string value. Please preserve the order.
        Array value: ${arr}`) returns (string|int)[]|error = @np:NaturalFunction external;

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

function getPopularSportsPerson(
        string nameSegment,
        int decadeStart,
        np:Prompt prompt = `Who is a popular sportsperson that was born in the decade starting
            from ${decadeStart} with ${nameSegment} in their name?`)
    returns SportsPerson|error? = @np:NaturalFunction external;
