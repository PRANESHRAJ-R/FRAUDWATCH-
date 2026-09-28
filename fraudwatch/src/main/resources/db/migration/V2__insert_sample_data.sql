INSERT INTO accounts (account_number, owner_name, balance, currency, active)
VALUES
('ACC1001', 'John Doe', 100000.00, 'INR', TRUE),
('ACC1002', 'Jane Smith', 50000.00, 'INR', TRUE),
('ACC1003', 'Alice Johnson', 75000.00, 'INR', TRUE),
('ACC1004', 'Bob Williams', 30000.00, 'INR', TRUE);

INSERT INTO rules (name, code, description, enabled, amount_threshold, transaction_count_threshold, window_seconds)
VALUES
('Large Amount Rule', 'AMOUNT_THRESHOLD', 'Flags transactions exceeding amount threshold', TRUE, 50000.00, NULL, NULL),
('Rapid Repeated Transfers', 'VELOCITY', 'Flags more than N transfers from the same account within a short window', TRUE, NULL, 3, 60);
