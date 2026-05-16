CREATE TABLE IF NOT EXISTS productions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    quantity DOUBLE DEFAULT 0,
    price DOUBLE DEFAULT 0,
    unit VARCHAR(20),
    status VARCHAR(20) DEFAULT 'No Stock', -- Options: 'No Stock', 'Low Stock', 'In Stock'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
