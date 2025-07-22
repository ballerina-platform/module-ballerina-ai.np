# Overview

This is the library module for natural programming - specifically the compile-time code generation component of natural programming. This module also generates JSON schema corresponding to types used with natural expressions.

For more information about natural programming, see [Natural Language is Code: A hybrid approach with Natural Programming](https://blog.ballerina.io/posts/2025-04-26-introducing-natural-programming/).

### Sample of compile-time code generation

```ballerina
const int COUNT = 25;

type Employee record {
    string name;
    int id;
    decimal salary;
    Department department;
};

type Department record {
    string name;
    int[] employees;
};

public function main() returns error? {
    Employee[] employees = const natural {
        Give me a list of ${COUNT} employees.
    
        Make sure to reflect real-world data. For example, make sure the data is proportionate to 
        the department sizes and IDs of employees from the same department aren't always contiguous.
    };
    
    printEmployeeDataByDepartment(employees);
}

function printEmployeeDataByDepartment(Employee[] employees) = @natural:code {
    prompt: string `Print the employee data grouped by department, in the following format.
    
    Department: <Department Name>
    =================================================
    Employee ID: <Employee 1 ID>, Name: <Employee 1 Name>, Salary: <Employee 1 Salary>
    ...
    Employee ID: <Employee n ID>, Name: <Employee n Name>, Salary: <Employee n Salary>
    `
} external;
```

### Configuring the Copilot for compile-time code generation 

The `BAL_CODEGEN_URL` and `BAL_CODEGEN_TOKEN` environment variables need to be set.

You can currently use the configuration generated via the `Ballerina: Configure default WSO2 model provider` VS Code command as the values.
