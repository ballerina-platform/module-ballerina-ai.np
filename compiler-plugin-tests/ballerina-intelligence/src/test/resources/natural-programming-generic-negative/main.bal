// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/ai;

public type Product record {|
    string id;
    decimal price;
|};

function filterProducts(Product[] products, decimal priceThreshold) returns Product[] = @natural:code {
    prompt: "Filter products with a price greater than the given threshold."
} external;

public function main() returns error? {
    decimal minPrice = check natural (check ai:getDefaultModelProvider()) {
        Give me a random number between 100 and 1000
    };

    Product[] products = [
        {id: "PROD001", price: minPrice + 100},
        {id: "PROD002", price: minPrice + 250}
    ];

    Product[] _ = filterProducts(products, 500);
}
