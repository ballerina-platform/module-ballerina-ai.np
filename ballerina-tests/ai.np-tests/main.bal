// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org).
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/ai;

configurable string serviceUrl = ?;
configurable string accessToken = ?;

final ai:Wso2ModelProvider mp = check new (serviceUrl, accessToken);

function queryAboutCountry(string query) returns string|error => natural (mp) {
    Which country is ${query}?
};

function queryACalculation() returns string|error => natural (mp) {
    What is 1 + 2 ?
};

function getParsedValues(string[] arr) returns (string|int)[]|error => natural (mp) {
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
      returns SportsPerson|error => natural (mp) {
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

function getPlacesOfInterest(string country, 
                             string interest,
                             int count = 3) returns PlaceOfInterest[]|error => natural (mp) {
    Tell me about places in the specified country that could be a good destination 
    to someone who has the specified interest.

    Include only the number of places specified by the count parameter.
};

public class Obj {
    int def = 0;
}

function getPlacesOfInterestWithNonAnydataParams(string country, 
                             string interest,
                             Obj operator,
                             int count = 3) returns PlaceOfInterest[]|error => natural (mp) {
    Tell me about places in the specified country that could be a good destination 
    to someone who has the specified interest.

    Include only the number of places specified by the count parameter.
};

function getPopularSportsPersonWithUnusedParams(string nameSegment, 
                                string thisParamIsNotUsed,
                                int alsoNotUsed = 4, 
                                int decadeStart = 1990)
      returns SportsPerson|error? => natural (mp) {
    Who is a popular sportsperson that was born in the decade starting
    from ${decadeStart} with ${nameSegment} in their name?
};

function getResultOfBallerinaProgram(int x, int y) returns int|error => natural (mp) {
    What's the output of the Ballerina code below?

    ```ballerina
    import ballerina/io;

    public function main() {
        int x = ${x};
        int y = ${y};
        io:println(x + y);
    \}
    ```
};

function getSum(int a, int b = 2, int... c) returns int|error => natural (mp) {
    What's the sum of these values?
};

type IntRecord record {|
    int val;
|};

function getSumWithIncludedRecordParam(int d, *IntRecord e, int... f) returns int|error {
    int|error res = natural (mp) {
        Give me the sum of these values
    };
    return res;
}

