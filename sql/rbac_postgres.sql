
-- 1) RBAC tables
CREATE TABLE IF NOT EXISTS sys_role (
  id BIGSERIAL PRIMARY KEY,
  role_code VARCHAR(64) NOT NULL UNIQUE, -- ADMIN / DEV / QA ...
  role_name VARCHAR(255),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS sys_permission (
  id BIGSERIAL PRIMARY KEY,
  permission_code VARCHAR(255) NOT NULL UNIQUE, -- e.g. container:logs:view
  permission_name VARCHAR(255),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  assigned_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  granted_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE,
  CONSTRAINT fk_role_perm_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id) ON DELETE CASCADE
);

-- API ↔ Permission mapping. permission_code kept for readability and redundancy;
-- ensure sys_permission.permission_code is unique so we can reference it if desired.
CREATE TABLE IF NOT EXISTS sys_permission (
  id BIGSERIAL PRIMARY KEY,
  url_pattern TEXT NOT NULL,        -- use '*' for wildcard segments, e.g. /api/containers/*/logs
  http_method VARCHAR(10) NOT NULL DEFAULT '*', -- GET, POST, etc. '*' means all methods
  permission_code VARCHAR(255) NOT NULL,
  description TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  CONSTRAINT uq_api_pattern_method UNIQUE (url_pattern, http_method)
);

-- Useful index for pattern matching (simple): you will use LIKE/REPLACE matching in queries.
CREATE INDEX IF NOT EXISTS idx_api_url_pattern ON sys_permission(url_pattern);
CREATE INDEX IF NOT EXISTS idx_role_code ON sys_role(role_code);
CREATE INDEX IF NOT EXISTS idx_permission_code ON sys_permission(permission_code);

-- 2) Seed roles (idempotent)
INSERT INTO sys_role (role_code, role_name)
VALUES
  ('ADMIN', 'Administrator'),
  ('PM', 'Project Manager'),
  ('LEADER', 'Development Leader'),
  ('DEV', 'Developer'),
  ('QA', 'QA'),
  ('OTHER', 'Other')
ON CONFLICT (role_code) DO NOTHING;

-- 3) Seed permissions (idempotent)
INSERT INTO sys_permission (permission_code, permission_name)
VALUES
  ('container:list:view', 'View container list'),
  ('container:inspect:view', 'Inspect container'),
  ('container:logs:view', 'View container logs'),
  ('container:restart:execute', 'Restart container'),
  ('container:start:execute', 'Start container'),
  ('container:stop:execute', 'Stop container')
ON CONFLICT (permission_code) DO NOTHING;

-- 4) Seed API → permission mappings (idempotent)
-- Use '*' wildcard in url_pattern to match segments; matching logic in app will convert '*'->'%' and use LIKE.
INSERT INTO sys_permission (url_pattern, http_method, permission_code, description)
VALUES
  ('/api/containers', 'GET', 'container:list:view', 'List containers'),
  ('/api/containers', 'POST', 'container:start:execute', 'Create / start container'),
  ('/api/containers/*', 'GET', 'container:inspect:view', 'Get container info'),
  ('/api/containers/*/logs', 'GET', 'container:logs:view', 'Get container logs'),
  ('/api/containers/*/restart', 'POST', 'container:restart:execute', 'Restart container'),
  ('/api/containers/*/start', 'POST', 'container:start:execute', 'Start container'),
  ('/api/containers/*/stop', 'POST', 'container:stop:execute', 'Stop container')
ON CONFLICT (url_pattern, http_method) DO NOTHING;

-- 5) Map permissions to roles according to your rules:
-- ADMIN: all permissions
-- LEADER: restart + read (list, inspect, logs)
-- PM/DEV/QA/OTHER: read-only (list, inspect, logs)

-- helper: insert mapping by selecting ids
-- ADMIN -> all permissions
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r CROSS JOIN sys_permission p
WHERE r.role_code = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- LEADER -> restart + read
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
  'container:list:view','container:inspect:view','container:logs:view','container:restart:execute'
)
WHERE r.role_code = 'LEADER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- PM, DEV, QA, OTHER -> read-only
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
  'container:list:view','container:inspect:view','container:logs:view'
)
WHERE r.role_code IN ('PM','DEV','QA','OTHER')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 6) (Optional) Example: ensure a default mapping exists so new users can be assigned by role_code
-- no-op here; assignment SQL in app can use (SELECT id FROM sys_role WHERE role_code = 'OTHER' LIMIT 1)

-- End of file

