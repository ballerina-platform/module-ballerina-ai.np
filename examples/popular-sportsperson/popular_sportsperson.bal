import ballerina/ai;
import ballerina/io;

final ai:ModelProvider model = check ai:getDefaultModelProvider();

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
      returns SportsPerson|error => natural (model) {
    Who is a popular sportsperson that was born in the decade starting 
    from ${decadeStart} with ${nameSegment} in their name?
};

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
