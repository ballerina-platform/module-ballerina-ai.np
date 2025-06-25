import ballerina/io;
import ballerina/http;

const END = 100000;

int n = 3;

final http:Client cl = check new ("http://localhost:9090");

const annotation record{|string value;|} testAnnot on type;

@testAnnot {
    value: "val"
}
type A record {
    int a;
};

class C {
    public int a = 1;
    function init(int a) {
        self.a = a;
    }

    public function getA() returns int {
        return self.a;
    }
}

function getIntegers() returns (int|string)[]|error => const natural {
    Give me an array of with integers between 1000 and ${END} and strings.
};

public function main() {
    worker workerName {

    }

    io:println(getIntegers());
}

function test() returns int {
    return END;
}

function testIntOrError() returns int|error {
    return END;
}

function testStr() returns string {
    return "1";
}
