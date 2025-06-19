import ballerina/io;

type Employee record {|
    string name;
    decimal salary;
|};

function sortEmployees(Employee[] employees) returns Employee[] = @code {
    prompt: string `Give me a new array with the employees sorted by
                        1. salary in descending order and then
                        2. name in ascending order`
} external;

public function main() {
    Employee[] employees = [
        {name: "Charlie", salary: 50000},
        {name: "Bob", salary: 60000},
        {name: "Alice", salary: 50000},
        {name: "David", salary: 70000}
    ];

    Employee[] sortEmployeesResult = sortEmployees(employees);
    io:println(sortEmployeesResult);
}
