import ballerina/io;

const START = 1001;

function getIntegers() returns int[]|error => const natural {
    Give me an array of length 10, with integers between ${START} and 2000.
};

public function main() {
    io:println(getIntegers());
}
