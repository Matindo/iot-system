INSERT INTO platform.subscription_tiers
    (name, max_messages_per_day, max_devices, max_projects, data_retention_days,
     max_message_rate_per_sec, storage_limit_mb, api_calls_per_minute, price_kes_monthly, features)
VALUES
    ('FREE',         10000,    5,   2,   7,   10,    500,   60,   0,      '{"webhooks": false, "csv_export": true,  "downsampling": false}'),
    ('STARTER',      100000,   25,  5,   30,  50,    5000,  300,  1500,   '{"webhooks": true,  "csv_export": true,  "downsampling": true}'),
    ('PROFESSIONAL', 1000000,  100, 20,  90,  200,   50000, 1000, 6000,   '{"webhooks": true,  "csv_export": true,  "downsampling": true, "api_access": true}'),
    ('ENTERPRISE',   -1,      -1,  -1,  -1,  -1,    -1,    -1,   0,      '{"webhooks": true,  "csv_export": true,  "downsampling": true, "api_access": true, "sla": true}')
ON CONFLICT (name) DO NOTHING;
