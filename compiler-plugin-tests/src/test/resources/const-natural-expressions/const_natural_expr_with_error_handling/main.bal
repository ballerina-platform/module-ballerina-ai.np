import ballerina/io;

const END = 100000;

int n = 3;

function getIntegers() returns (int|string)[]|error => const natural {
    Give me an array of with integers between 1000 and ${END} and strings.
};

public function main() {
    io:println(getIntegers());
}

function test() returns int {
    return END;
}

function testStr() returns string {
    return "1";
}
