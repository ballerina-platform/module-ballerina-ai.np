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

import ballerina_np/np_test_commons as _;
import ballerina/test;

@test:Config
function testWithNaturalFunctionAnnotation() returns error? {
    string result = check queryAboutCountry("known as the pearl of the Indian Ocean");
    test:assertEquals(result, "Sri Lanka");
}

@test:Config
function testComplexTypeWithNaturalFunctionAnnotation() returns error? {
    (string|int)[] result = check getParsedValues(["foo", "1", "bar", "2.3", "4"]);
    test:assertEquals(result, ["foo", 1, "bar", "2.3", 4]);
}

@test:Config
function testComplexTypeWithDocsWithNaturalFunctionAnnotation() returns error? {
    SportsPerson? person = check getPopularSportsPerson("Simone", 1990);
    test:assertEquals(person, <SportsPerson> {
        firstName: "Simone",
        lastName: "Biles",
        sport: "Gymnastics",
        yearOfBirth: 1997
    });
}

@test:Config
function testParameterInjectionToNaturalFunctionWhenThereAreNoInterpolations() returns error? {
    PlaceOfInterest[] places = check getPlacesOfInterest("Sri Lanka", "beach");
    test:assertEquals(places, <PlaceOfInterest[]> [
        {"name":"Unawatuna Beach","city":"Galle","country":"Sri Lanka","description":"A popular beach known for its golden sands and vibrant nightlife."},
        {"name":"Mirissa Beach","city":"Mirissa","country":"Sri Lanka","description":"Famous for its stunning sunsets and opportunities for whale watching."},
        {"name":"Hikkaduwa Beach","city":"Hikkaduwa","country":"Sri Lanka","description":"A great destination for snorkeling and surfing, lined with lively restaurants."}
    ]);
}

@test:Config
function testParameterInjectionToNaturalFunctionWhenThereAreNonAnydataParams() returns error? {
    PlaceOfInterest[] places = check getPlacesOfInterestWithNonAnydataParams("UAE", "skyscrapers", new Obj(), 2);
    test:assertEquals(places, <PlaceOfInterest[]> [
        {"name":"Burj Khalifa","city":"Dubai","country":"UAE","description":"The tallest building in the world, offering panoramic views of the city."},
        {"name":"Ain Dubai","city":"Dubai","country":"UAE","description":"The world's tallest observation wheel, providing breathtaking views of the Dubai skyline."}
    ]);
}

@test:Config
function testParametersNotBeingInjectedWhenThereAreInterpolations() returns error? {
    SportsPerson? person = check getPopularSportsPersonWithUnusedParams("Simone", "unused value");
    test:assertEquals(person, <SportsPerson> {
        firstName: "Simone",
        lastName: "Biles",
        sport: "Gymnastics",
        yearOfBirth: 1997
    });
}
