CREATE TABLE askops_schema.sys_user (
	id serial4 NOT NULL,
	username varchar(255) NOT NULL,
	"password" varchar(255) NOT NULL,
	enabled bool DEFAULT true NULL,
	CONSTRAINT sys_user_pkey PRIMARY KEY (id),
	CONSTRAINT sys_user_username_key UNIQUE (username)
);

-- 权限表：存储系统所有权限
CREATE TABLE sys_permission (
    id SERIAL PRIMARY KEY,  -- 使用 SERIAL 实现自增
    permission_code VARCHAR(50) NOT NULL UNIQUE, -- 唯一约束
    url_pattern VARCHAR(255) NOT NULL,
    http_method VARCHAR(10),
    description VARCHAR(100),
    CONSTRAINT uk_permission_code UNIQUE (permission_code) -- 显式定义唯一约束
);


-- 用户权限关联表：存储用户与权限的映射关系 带外键注入
CREATE TABLE sys_user_permission (
    user_id INT NOT NULL,
    permission_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, permission_code),
    CONSTRAINT fk_user
        FOREIGN KEY (user_id)
        REFERENCES sys_user(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_permission
        FOREIGN KEY (permission_code)
        REFERENCES sys_permission(permission_code)
        ON DELETE CASCADE
);

CREATE TABLE sys_role (
    id SERIAL PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(255) NOT NULL,
    CONSTRAINT uk_role_code UNIQUE (role_code)
);
-- 用户角色关联表
CREATE TABLE sys_user_role (
    user_id INT NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role_code),
    CONSTRAINT fk_user_role_user
        FOREIGN KEY (user_id)
        REFERENCES sys_user(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role
        FOREIGN KEY (role_code)
        REFERENCES sys_role(role_code)
        ON DELETE CASCADE
);
CREATE TABLE sys_role_permission (
    role_code VARCHAR(50) NOT NULL,
    permission_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (role_code, permission_code),
    CONSTRAINT fk_rp_role
        FOREIGN KEY (role_code)
        REFERENCES sys_role(role_code)
        ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission
        FOREIGN KEY (permission_code)
        REFERENCES sys_permission(permission_code)
        ON DELETE CASCADE
);


INSERT INTO sys_permission (permission_code, url_pattern, http_method, description) VALUES
('containers:read',   '/api/containers/**',        'GET',  '查看容器'),
('containers:logs',   '/api/containers/**/logs',   'GET',  '查看日志'),
('containers:write',  '/api/containers/**',        'POST', '创建/修改容器'),
('containers:restart','/api/containers/*/restart', 'POST', '重启容器');

UPDATE sys_permission
SET url_pattern = '/api/containers/restart/**'
WHERE permission_code = 'containers:restart'
  AND url_pattern = '/api/containers/*/restart'
  AND http_method = 'POST'
  AND description = '重启容器';

-- 用户1001拥有所有权限
INSERT INTO sys_user_permission (user_id, permission_code) VALUES
(2, 'containers:read'),
(2, 'containers:logs'),
(2, 'containers:write');
INSERT INTO sys_user_permission (user_id, permission_code) VALUES
(2, 'containers:restart');

insert into sys_role (role_code, role_name) values
('ADMIN', 'Administrator'),
('DEV', 'Developer'),
('QA', 'QA Engineer');

insert into sys_user_role (user_id, role_code) values
(2, 'ADMIN');
insert into sys_role_permission (role_code, permission_code) values
('ADMIN', 'containers:read'),
('ADMIN', 'containers:logs'),
('ADMIN', 'containers:write'),
('ADMIN', 'containers:restart');