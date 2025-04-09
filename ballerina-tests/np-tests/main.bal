function queryAboutCountry(string query) returns string|error => natural {
    Which country is ${query}?
};

function getParsedValues(string[] arr) returns (string|int)[]|error => natural {
    For each string value in the given array if the value can be parsed
    as an integer give an integer, if not give the same string value. Please preserve the order.
    Array value: ${arr}
};

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

function getPopularSportsPerson(string nameSegment, int decadeStart)
      returns SportsPerson|error? => natural {
    Who is a popular sportsperson that was born in the decade starting
    from ${decadeStart} with ${nameSegment} in their name?
};
