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
    is_super_admin bool NOT NULL DEFAULT FALSE
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




CREATE TABLE askops_schema.t_agent (
	id bigserial NOT NULL,
	ip varchar(64) NOT NULL,
	port int4 NOT NULL,
	group_id int8 NOT NULL UNIQUE,
	"name" varchar(100) NOT NULL,
	status varchar(20) DEFAULT 'REGISTERING'::character varying NOT NULL,
	last_heartbeat_time timestamp NULL,
	create_time timestamp DEFAULT now() NOT NULL,
	update_time timestamp DEFAULT now() NOT NULL,
	CONSTRAINT t_agent_name_key UNIQUE (name),
	CONSTRAINT t_agent_pkey PRIMARY KEY (id)
);


CREATE TABLE t_group (
    id BIGSERIAL PRIMARY KEY,
    group_name VARCHAR(100) UNIQUE NOT NULL
);
CREATE TABLE user_role_mapping (
    user_id BIGINT REFERENCES sys_user(id),
    role_id BIGINT REFERENCES sys_role(id),
    PRIMARY KEY (user_id, role_id)
);
CREATE TABLE role_permission_mapping (
    role_id BIGINT REFERENCES sys_role(id),
    permission_id BIGINT REFERENCES sys_permission(id),
    PRIMARY KEY (role_id, permission_id)
);
CREATE TABLE role_group_mapping (
    role_id BIGINT REFERENCES sys_role(id),
    group_id BIGINT REFERENCES t_group(id),
    PRIMARY KEY (role_id, group_id)
);
INSERT INTO askops_schema.sys_role (role_code, role_name, is_super_admin)
VALUES
('opsadmin', 'opsadmin', TRUE);
('opsadmin', 'opsadmin', TRUE);
INSERT INTO askops_schema.sys_role (role_code, role_name)
VALUES
    ('other', 'other'),
    ('dqsl-dev', 'dqsl-dev'),
    ('dqma-dev', 'dqma-dev'),
    ('rosetta-dev', 'rosetta-dev'),
    ('rtds-dev', 'rtds-dev'),
    ('onetbui-dev', 'onetbui-dev'),
    ('ssdr-dev', 'ssdr-dev'),
    ('dqsl-leader', 'dqsl-leader'),
    ('dqma-leader', 'dqma-leader'),
    ('rosetta-leader', 'rosetta-leader'),
    ('rtds-leader', 'rtds-leader'),
    ('onetbui-leader', 'onetbui-leader'),
    ('ssdr-leader', 'ssdr-leader')
ON CONFLICT (role_code) DO NOTHING;  -- 若 role_code 已存在，则跳过该条插入，避免报错


insert into askops_schema.role_permission_mapping (role_id, permission_id)
values
    ((select id from askops_schema.sys_role where role_code = 'admin'), (select id from askops_schema.sys_permission where permission_code = 'containers:read')),
    ((select id from askops_schema.sys_role where role_code = 'admin'), (select id from askops_schema.sys_permission where permission_code = 'containers:logs')),
    ((select id from askops_schema.sys_role where role_code = 'admin'), (select id from askops_schema.sys_permission where permission_code = 'containers:write')),
    ((select id from askops_schema.sys_role where role_code = 'admin'), (select id from askops_schema.sys_permission where permission_code = 'containers:restart')),
    (select id from askops_schema.sys_role where role_code = 'dqsl-dev'), (select id from askops_schema.sys_permission where permission_code = 'containers:read')),
    ((select id from askops_schema.sys_role where role_code = 'dqma-dev'), (select id from askops_schema.sys_permission where permission_code = 'containers:read')),
    ((select id from askops_schema.sys_role where role_code = 'dqsl-leader'), (select id from askops_schema.sys_permission where permission_code = 'containers:read')),
    ((select id from askops_schema.sys_role where role_code = 'dqma-leader'), (select id from askops_schema.sys_permission where permission_code = 'containers:read')),
    (select id from askops_schema.sys_role where role_code = 'dqsl-dev'), (select id from askops_schema.sys_permission where permission_code = 'containers:logs')),
    ((select id from askops_schema.sys_role where role_code = 'dqma-dev'), (select id from askops_schema.sys_permission where permission_code = 'containers:logs')),
    ((select id from askops_schema.sys_role where role_code = 'dqsl-leader'), (select id from askops_schema.sys_permission where permission_code = 'containers:logs')),
    ((select id from askops_schema.sys_role where role_code = 'dqma-leader'), (select id from askops_schema.sys_permission where permission_code = 'containers:logs')),
    ((select id from askops_schema.sys_role where role_code = 'dqsl-leader'), (select id from askops_schema.sys_permission where permission_code = 'containers:restart')),
    ((select id from askops_schema.sys_role where role_code = 'dqma-leader'), (select id from askops_schema.sys_permission where permission_code = 'containers:restart'));


insert into askops_schema.user_role_mapping (user_id, role_id)
values
    (2, (select id from askops_schema.sys_role where role_code = 'admin')),
    (6, (select id from askops_schema.sys_role where role_code = 'dqsl-dev')),
    (6, (select id from askops_schema.sys_role where role_code = 'dqma-dev')),
    (8, (select id from askops_schema.sys_role where role_code = 'dqsl-leader')),
    (8, (select id from askops_schema.sys_role where role_code = 'dqma-leader'));
insert into askops_schema.t_group (group_name) values
('admin'),
('other'),
('dqsl'),
('dqma'),
('rosetta'),
('rtds'),
('onetbui'),
('ssdr');

insert into askops_schema.role_group_mapping (role_id, group_id)
values
    ((select id from askops_schema.sys_role where role_code = 'admin'), (select id from askops_schema.t_group where group_name = 'admin')),
    ((select id from askops_schema.sys_role where role_code = 'other'), (select id from askops_schema.t_group where
    group_name = 'other')),
    ((select id from askops_schema.sys_role where role_code = 'dqsl-dev'), (select id from askops_schema.t_group where group_name = 'dqsl')),
    ((select id from askops_schema.sys_role where role_code = 'dqma-dev'), (select id from askops_schema.t_group where group_name = 'dqma')),
    ((select id from askops_schema.sys_role where role_code = 'dqsl-leader'), (select id from askops_schema.t_group where group_name = 'dqsl')),
    ((select id from askops_schema.sys_role where role_code = 'dqma-leader'), (select id from askops_schema.t_group where group_name = 'dqma')),
    ((select id from askops_schema.sys_role where role_code = 'rosetta-dev'), (select id from askops_schema.t_group where group_name = 'rosetta')),
    ((select id from askops_schema.sys_role where role_code = 'rosetta-leader'), (select id from askops_schema.t_group where group_name = 'rosetta')),
    ((select id from askops_schema.sys_role where role_code = 'rtds-dev'), (select id from askops_schema.t_group where group_name = 'rtds')),
    ((select id from askops_schema.sys_role where role_code = 'rtds-leader'), (select id from askops_schema.t_group where group_name = 'rtds')),
    ((select id from askops_schema.sys_role where role_code = 'onetbui-dev'), (select id from askops_schema.t_group where group_name = 'onetbui')),
    ((select id from askops_schema.sys_role where role_code = 'onetbui-leader'), (select id from askops_schema.t_group where group_name = 'onetbui')),
    ((select id from askops_schema.sys_role where role_code = 'ssdr-dev'), (select id from askops_schema.t_group where group_name = 'ssdr')),
    ((select id from askops_schema.sys_role where role_code = 'ssdr-leader'), (select id from askops_schema.t_group where group_name = 'ssdr'));

## 当创建一个用户，分配权限过程
#### 分配获取agent权限
insert into user_role_mapping (user_id, role_id)
values
    (9, (select id from sys_role where role_code = 'rosetta-dev'));
#### 分配Api permission
insert into role_permission_mapping (role_id, permission_id)
values
    ((select id from sys_role where role_code = 'rosetta-dev'), (1)),
    ((select id from sys_role where role_code = 'rosetta-dev'), (2));
