
function calculateThePriceNPGenerated(float[] prices) returns float {
    float discount = 0.1;
    float subtotal = 0;
    foreach float price in prices {
        subtotal += price;
    }
    return subtotal * (1 - discount);
}

