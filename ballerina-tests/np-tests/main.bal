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

# Represents a place of interest.
type PlaceOfInterest record {|
    # Name of the place.
    string name;
    # City in which the place is located.
    string city;
    # Country in which the place is located.
    string country;
    # One-liner description of the place.
    string description;
|};

function getPlacesOfInterset(string country, 
                             string interest,
                             int count = 3) returns PlaceOfInterest[]|error => natural {
    Tell me about places in the specified country that could be a good destination 
    to someone who has the specified interest.

    Include only the number of places specified by the count parameter.
};

public class Obj {
    int def = 0;
}

function getPlacesOfIntersetWithNonAnydataParams(string country, 
                             string interest,
                             Obj operator,
                             int count = 3) returns PlaceOfInterest[]|error => natural {
    Tell me about places in the specified country that could be a good destination 
    to someone who has the specified interest.

    Include only the number of places specified by the count parameter.
};

function getPopularSportsPersonWithUnusedParams(string nameSegment, 
                                string thisParamIsNotUsed,
                                int alsoNotUsed = 4, 
                                int decadeStart = 1990)
      returns SportsPerson|error? => natural {
    Who is a popular sportsperson that was born in the decade starting
    from ${decadeStart} with ${nameSegment} in their name?
};
