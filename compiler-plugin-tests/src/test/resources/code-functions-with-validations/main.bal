import ballerina/io;

configurable decimal taxRate = 0.08;
configurable decimal discountThreshold = 100.00;

final decimal DISCOUNT_RATE = 0.1;
final decimal MINIMUM_ITEM_PRICE = 1.00;

public function calculateTotalPrice(decimal[] itemPrices) returns decimal = @code {
                prompt: string `calculates the final total price of items by summing valid items
                    (above minimum price), applying a discount if the subtotal exceeds a threshold,
                    and adding tax to the discounted amount`
} external;

public function main() {
    decimal[] itemPrices = [25.50, 15.75, 40.00, 19.99, 12.49];
    decimal total = calculateTotalPrice(itemPrices);
    io:println("Total price: ", total);
}


