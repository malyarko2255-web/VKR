-- advance-reference V2: начальные справочные данные

-- Нормы ГСМ по АМ-23-р
INSERT INTO fuel_norms (id, vehicle_category, base_norm, effective_from, source_document) VALUES
    (gen_random_uuid(), 'TRUCK_MEDIUM', 28.5, '2024-01-01', 'АМ-23-р'),
    (gen_random_uuid(), 'TRUCK_LARGE',  35.0, '2024-01-01', 'АМ-23-р'),
    (gen_random_uuid(), 'VAN',          12.0, '2024-01-01', 'АМ-23-р');

-- Маршруты
INSERT INTO routes (id, name, origin, destination, distance_km, estimated_hours, active) VALUES
    (gen_random_uuid(), 'Москва — Санкт-Петербург', 'Москва',          'Санкт-Петербург', 713.0,  8.0,  TRUE),
    (gen_random_uuid(), 'Москва — Екатеринбург',    'Москва',          'Екатеринбург',    1786.0, 22.0, TRUE),
    (gen_random_uuid(), 'Москва — Казань',           'Москва',          'Казань',          820.0,  10.5, TRUE),
    (gen_random_uuid(), 'Москва — Новосибирск',      'Москва',          'Новосибирск',     3191.0, 38.0, TRUE),
    (gen_random_uuid(), 'Санкт-Петербург — Казань',  'Санкт-Петербург', 'Казань',          1530.0, 18.0, TRUE);

-- Контрагент
INSERT INTO contractors (id, name, inn, ogrn, contact_phone, active) VALUES
    ('a1000000-0000-0000-0000-000000000001',
     'ООО ЛогиТранс', '7712345678', '1027700132195', '+7-495-000-01-01', TRUE);

-- Водители
INSERT INTO drivers (id, user_id, full_name, license_no, phone, contractor_id, active) VALUES
    ('d1000000-0000-0000-0000-000000000001', NULL,
     'Иванов Иван Иванович',    '77АА123456', '+7-916-000-00-01',
     'a1000000-0000-0000-0000-000000000001', TRUE),
    ('d1000000-0000-0000-0000-000000000002', NULL,
     'Петров Пётр Петрович',    '77ВВ654321', '+7-916-000-00-02',
     'a1000000-0000-0000-0000-000000000001', TRUE),
    ('d1000000-0000-0000-0000-000000000003', NULL,
     'Сидоров Сидор Сидорович', '77СС112233', '+7-916-000-00-03',
     'a1000000-0000-0000-0000-000000000001', TRUE);

-- Лимиты водителей по всем типам авансов
INSERT INTO driver_limits (id, driver_id, advance_type, monthly_limit, daily_limit, auto_approve_threshold)
SELECT
    gen_random_uuid(),
    d.id,
    t.advance_type,
    t.monthly_limit,
    t.daily_limit,
    t.auto_approve_threshold
FROM
    (VALUES
        ('d1000000-0000-0000-0000-000000000001'),
        ('d1000000-0000-0000-0000-000000000002'),
        ('d1000000-0000-0000-0000-000000000003')
    ) AS d(id)
CROSS JOIN (
    VALUES
        ('FUEL',           50000.00, 10000.00, 5000.00),
        ('LOADING_WORK',   30000.00,  5000.00, 3000.00),
        ('DAILY_ALLOWANCE',15000.00,  2000.00, 2000.00),
        ('REPAIR',         80000.00, 20000.00, 5000.00),
        ('PLANNED',       100000.00, 25000.00, 5000.00)
    ) AS t(advance_type, monthly_limit, daily_limit, auto_approve_threshold);
