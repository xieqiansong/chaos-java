-- ============ 样例数据 ============
INSERT INTO t_user (name, age, email, status, tenant_id) VALUES
    ('Alice', 20, 'alice@demo.com', 0, 0),
    ('Bob',   35, 'bob@demo.com',   0, 0),
    ('Carol', 28, 'carol@demo.com', 1, 0),
    ('Dave',  42, 'dave@demo.com',  0, 0),
    ('Eve',   17, 'eve@demo.com',   2, 0);

INSERT INTO t_order (user_id, amount, create_time) VALUES
    (1, 100.00, '2024-01-01 10:00:00'),
    (1, 200.50, '2024-02-01 10:00:00'),
    (2, 50.00,  '2024-03-01 10:00:00'),
    (3, 300.00, '2024-04-01 10:00:00'),
    (4, 80.00,  '2024-05-01 10:00:00'),
    (4, 120.00, '2024-06-01 10:00:00');

INSERT INTO tenant_data (tenant_id, biz_data) VALUES
    (1, 'tenant-1 data A'),
    (1, 'tenant-1 data B'),
    (2, 'tenant-2 data C');

INSERT INTO log_record_2024 (content, create_time) VALUES
    ('log 2024-01', '2024-01-15 10:00:00'),
    ('log 2024-02', '2024-02-15 10:00:00');
INSERT INTO log_record_2025 (content, create_time) VALUES
    ('log 2025-01', '2025-01-15 10:00:00');
