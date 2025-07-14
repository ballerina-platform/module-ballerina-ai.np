import ballerina/io;

configurable decimal taxRate = 0.08;
configurable decimal discountThreshold = 100.00;

public final decimal DISCOUNT_RATE = 0.1;
public final decimal MINIMUM_ITEM_PRICE = 1.00;

public function calculateTotalPrice(decimal[] itemPrices) returns decimal = @code {
                prompt: string `Calculate the total price of items by summing up valid prices
                        (above the minimum price), applying a discount if the subtotal exceeds a threshold,
                        and adding tax calculated based on the total after discount`
} external;

public function main() {
    decimal[] itemPrices = [25.50, 15.75, 40.00, 19.99, 12.49];
    decimal total = calculateTotalPrice(itemPrices);
    io:println("Total price: ", total);
}
