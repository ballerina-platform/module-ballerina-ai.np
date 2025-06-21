import ballerina/io;

configurable float discount = 0.1;

function calculateThePrice(float[] prices) returns float = @code {
    prompt: string `Calculate total price after discount`
} external;

public function main() {
    float[] cart = [100.0, 200.0, 300.0];
    float total = calculateThePrice(cart);
    io:println("Total after discount: " + total.toString());
}
