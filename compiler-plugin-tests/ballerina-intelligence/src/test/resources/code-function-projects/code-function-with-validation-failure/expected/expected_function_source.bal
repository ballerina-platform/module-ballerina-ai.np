
public function calculateTotalPriceNPGenerated(decimal[] itemPrices) returns decimal {
    decimal taxRate = 0.08;
    decimal discountThreshold = 100.00;
    decimal discountRate = 0.1;
    decimal minItemPrice = 1.00;

    decimal subtotal = 0.0;
    foreach decimal price in itemPrices {
        if (price < minItemPrice) {
            continue;
        }
        subtotal += price;
    }
    decimal discountedTotal = subtotal;
    if (subtotal > discountThreshold) {
        decimal discount = subtotal * discountRate;
        discountedTotal = subtotal - discount;
    }
    decimal tax = discountedTotal * taxRate;
    decimal finalTotal = discountedTotal + tax;
    return finalTotal;
}

