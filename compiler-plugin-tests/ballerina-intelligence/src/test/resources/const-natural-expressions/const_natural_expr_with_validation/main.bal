import ballerina/http;
import ballerina/io;

const END = 100000;

int number = 3;

final http:Client 'client = check new ("http://localhost:9090");

const annotation record {|string value;|} RecordAnnot on type;

@RecordAnnot {
    value: "val"
}
type recordType record {
    int a;
};

class TestClass {
    public int num;

    function init(int num) {
        self.num = num;
    }

    public function getNum() returns int {
        return self.num;
    }
}

function getIntegers() returns (int|string)[]|error => const natural {
    Give me an array of with integers between 1000 and ${END} and strings.
};

public function main() {
    worker w1 {

    }

    io:println(getIntegers());
}

function getInteger() returns int {
    return END;
}

function testIntOrError() returns int|error {
    return END;
}

function getString() returns string {
    return "1";
}
