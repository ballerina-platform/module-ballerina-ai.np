
function sortEmployeesNPGenerated(Employee[] employees) returns Employee[] {
    Employee[] sortedEmployees = employees.clone();
    int n = sortedEmployees.length();

    // Bubble sort implementation
    int i = 0;
    while i < n - 1 {
        int j = 0;
        while j < n - i - 1 {
            Employee emp1 = sortedEmployees[j];
            Employee emp2 = sortedEmployees[j + 1];

            // Compare salary (descending) and name (ascending)
            if emp1.salary < emp2.salary ||
                (emp1.salary == emp2.salary && emp1.name > emp2.name) {
                // Swap employees
                sortedEmployees[j] = emp2;
                sortedEmployees[j + 1] = emp1;
            }
            j += 1;
        }
        i += 1;
    }

    return sortedEmployees;
}

