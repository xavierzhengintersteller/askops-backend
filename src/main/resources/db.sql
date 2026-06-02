CREATE TABLE askops_schema.sys_user (
	id serial4 NOT NULL,
	username varchar(255) NOT NULL,
	"password" varchar(255) NOT NULL,
	enabled bool DEFAULT true NULL,
	permission_version int8 DEFAULT 1 NULL,
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

CREATE TABLE sys_permission (
                                id SERIAL PRIMARY KEY,

                                permission_code VARCHAR(100) NOT NULL UNIQUE,

                                permission_name VARCHAR(100) NOT NULL,   -- 👉 前端展示用

                                parent_id INT DEFAULT NULL,              -- 👉 树结构核心

                                type VARCHAR(20) NOT NULL,               -- 👉 menu / button / api

                                url_pattern VARCHAR(255),                -- 👉 后端鉴权用
                                http_method VARCHAR(10),

                                path VARCHAR(255),                       -- 👉 前端路由
                                component VARCHAR(255),                  -- 👉 前端组件路径

                                icon VARCHAR(50),                        -- 👉 UI

                                sort INT DEFAULT 0,                      -- 👉 排序

                                visible BOOLEAN DEFAULT TRUE,            -- 👉 是否显示菜单

                                description VARCHAR(255),

                                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_parent_permission
                                    FOREIGN KEY (parent_id) REFERENCES sys_permission(id)
);

CREATE TABLE sys_role (
    id SERIAL PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL,
    role_name VARCHAR(255) NOT NULL,
    is_super_admin bool NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_role_code UNIQUE (role_code)
);




INSERT INTO sys_permission (permission_code, url_pattern, http_method, description) VALUES
('containers:read',   '/api/containers/**',        'GET',  '查看容器'),
('containers:logs',   '/api/containers/**/logs',   'GET',  '查看日志'),
('containers:write',  '/api/containers/**',        'POST', '创建/修改容器'),
('containers:restart','/api/containers/restart/**', 'POST', '重启容器');



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
('admin', 'admin', TRUE);

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
    ((select id from askops_schema.sys_role where role_code = 'dqsl-dev'), (select id from askops_schema
    .sys_permission where permission_code = 'containers:read')),
    ((select id from askops_schema.sys_role where role_code = 'dqma-dev'), (select id from askops_schema.sys_permission where permission_code = 'containers:read')),
    ((select id from askops_schema.sys_role where role_code = 'dqsl-leader'), (select id from askops_schema.sys_permission where permission_code = 'containers:read')),
    ((select id from askops_schema.sys_role where role_code = 'dqma-leader'), (select id from askops_schema.sys_permission where permission_code = 'containers:read')),
    ((select id from askops_schema.sys_role where role_code = 'dqsl-dev'), (select id from askops_schema
    .sys_permission where permission_code = 'containers:logs')),
    ((select id from askops_schema.sys_role where role_code = 'dqma-dev'), (select id from askops_schema.sys_permission where permission_code = 'containers:logs')),
    ((select id from askops_schema.sys_role where role_code = 'dqsl-leader'), (select id from askops_schema.sys_permission where permission_code = 'containers:logs')),
    ((select id from askops_schema.sys_role where role_code = 'dqma-leader'), (select id from askops_schema.sys_permission where permission_code = 'containers:logs')),
    ((select id from askops_schema.sys_role where role_code = 'dqsl-leader'), (select id from askops_schema.sys_permission where permission_code = 'containers:restart')),
    ((select id from askops_schema.sys_role where role_code = 'dqma-leader'), (select id from askops_schema.sys_permission where permission_code = 'containers:restart'));


insert into askops_schema.user_role_mapping (user_id, role_id)
values
    ((select id from askops_schema.sys_user where username = 'admin'), (select id from askops_schema.sys_role where role_code = 'admin')),
    ((select id from askops_schema.sys_user where username = 'dev1'), (select id from askops_schema.sys_role where
    role_code = 'dqsl-dev')),
    ((select id from askops_schema.sys_user where username = 'dev1'),  (select id from askops_schema.sys_role where
    role_code = 'dqma-dev'));
--    (8, (select id from askops_schema.sys_role where role_code = 'dqsl-leader')),
--    (8, (select id from askops_schema.sys_role where role_code = 'dqma-leader'));
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

INSERT INTO askops_schema.sys_permission
( permission_code, url_pattern, http_method, description)
VALUES( 'containers:batch-restart', '/api/containers/batch-restart', 'POST','批量重启容器');

INSERT INTO askops_schema.role_permission_mapping (role_id, permission_id)
SELECT
    1,
    id
FROM askops_schema.sys_permission
WHERE permission_code = 'containers:batch-restart';

alter table t_agent add column fail_count int4


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


## 修改当前权限表
-- 1. 扩展 permission_code 字段长度
ALTER TABLE sys_permission
ALTER COLUMN permission_code TYPE VARCHAR(100);

-- 2. 修改原有字段 url_pattern 允许 NULL
ALTER TABLE sys_permission
    ALTER COLUMN url_pattern DROP NOT NULL;

-- 3. 新增字段（先不加 NOT NULL，给默认值）
ALTER TABLE sys_permission
    ADD COLUMN permission_name VARCHAR(100) DEFAULT '',
ADD COLUMN parent_id INT DEFAULT NULL,
ADD COLUMN type VARCHAR(20) DEFAULT 'api',
ADD COLUMN path VARCHAR(255),
ADD COLUMN component VARCHAR(255),
ADD COLUMN icon VARCHAR(50),
ADD COLUMN sort INT DEFAULT 0,
ADD COLUMN visible BOOLEAN DEFAULT TRUE,
ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- 4. 移除默认值并添加 NOT NULL 约束
ALTER TABLE sys_permission
    ALTER COLUMN permission_name DROP DEFAULT,
ALTER COLUMN permission_name SET NOT NULL,
ALTER COLUMN type DROP DEFAULT,
ALTER COLUMN type SET NOT NULL;

-- 5. 添加自关联外键
ALTER TABLE sys_permission
    ADD CONSTRAINT fk_parent_permission
        FOREIGN KEY (parent_id) REFERENCES sys_permission(id);

-- 6. 删除旧的唯一约束（如果存在）
ALTER TABLE sys_permission
DROP CONSTRAINT IF EXISTS uk_permission_code;


DROP TABLE IF EXISTS sys_audit_log;
CREATE TABLE sys_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    trace_id        VARCHAR(64)          DEFAULT NULL,
    user_id         BIGINT               DEFAULT NULL,
    super_admin     BOOLEAN              DEFAULT FALSE,
    module          VARCHAR(100)         DEFAULT NULL,
    operation       VARCHAR(50)          DEFAULT NULL,
    request_path    VARCHAR(255)         DEFAULT NULL,
    request_method  VARCHAR(20)          DEFAULT NULL,
    request_ip      VARCHAR(50)          DEFAULT NULL,
    user_agent      VARCHAR(1000)        DEFAULT NULL,
    request_params  TEXT                 DEFAULT NULL,
    response_result TEXT                 DEFAULT NULL,
    http_code       INT                  DEFAULT NULL,
    cost_time       BIGINT               DEFAULT NULL,
    status          VARCHAR(20)          DEFAULT NULL,
    error_msg       TEXT                 DEFAULT NULL,
    create_time     TIMESTAMP            DEFAULT CURRENT_TIMESTAMP
);
????
-- 索引：加速日志查询（必加）
CREATE INDEX idx_sys_audit_log_trace_id ON sys_audit_log (trace_id);
CREATE INDEX idx_sys_audit_log_user_id ON sys_audit_log (user_id);
CREATE INDEX idx_sys_audit_log_create_time ON sys_audit_log (create_time);