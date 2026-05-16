CREATE TABLE IF NOT EXISTS production_ingredients (
    id INT AUTO_INCREMENT PRIMARY KEY,
    production_id INT NOT NULL,
    stock_id INT NOT NULL,
    quantity_used DOUBLE NOT NULL,
    FOREIGN KEY (production_id) REFERENCES productions(id) ON DELETE CASCADE,
    FOREIGN KEY (stock_id) REFERENCES stocks(id)
);
