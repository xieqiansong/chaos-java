INSERT INTO t_user (id, name, age, email, status, tenant_id, version, deleted) VALUES
(1, 'Alice', 20, 'alice@example.com', 1, 0, 1, 0),
(2, 'Bob', 35, 'bob@example.com', 1, 0, 1, 0),
(3, 'Carol', 28, 'carol@example.com', 1, 0, 1, 0),
(4, 'Dave', 42, 'dave@example.com', 1, 0, 1, 0),
(5, 'Eve', 17, 'eve@example.com', 1, 0, 1, 0);

INSERT INTO t_order (id, user_id, amount, create_time) VALUES
(1, 1, 100.00, '2024-01-01 10:00:00'),
(2, 1, 200.00, '2024-02-01 11:00:00'),
(3, 2, 150.00, '2024-03-01 12:00:00'),
(4, 3, 300.00, '2024-04-01 13:00:00'),
(5, 4, 250.00, '2024-05-01 14:00:00'),
(6, 4, 400.00, '2024-06-01 15:00:00');

INSERT INTO tenant_data (id, tenant_id, biz_data) VALUES
(1, 1, 'T1-data-1'),
(2, 1, 'T1-data-2'),
(3, 2, 'T2-data-1'),
(4, 2, 'T2-data-2');
