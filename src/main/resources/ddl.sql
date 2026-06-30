-- askops_schema.container_info definition

-- Drop table

-- DROP TABLE askops_schema.container_info;

CREATE TABLE askops_schema.container_info (
	id bigserial NOT NULL,
	node_ip varchar(64) NOT NULL,
	container_id varchar(128) NOT NULL,
	container_name varchar(256) NOT NULL,
	image varchar(512) NOT NULL,
	state varchar(32) NOT NULL,
	status varchar(256) NOT NULL,
	created_at timestamp NOT NULL,
	last_seen_time timestamp NOT NULL,
	is_deleted bool DEFAULT false NOT NULL,
	ports_json jsonb NULL,
	extra_json jsonb NULL,
	CONSTRAINT container_info_node_ip_container_id_key UNIQUE (node_ip, container_id),
	CONSTRAINT container_info_pkey PRIMARY KEY (id)
);


-- askops_schema.sys_audit_log definition

-- Drop table

-- DROP TABLE askops_schema.sys_audit_log;

CREATE TABLE askops_schema.sys_audit_log (
	id bigserial NOT NULL,
	trace_id varchar(64) DEFAULT NULL::character varying NULL,
	user_id int8 NULL,
	super_admin bool DEFAULT false NULL,
	"module" varchar(100) DEFAULT NULL::character varying NULL,
	operation varchar(50) DEFAULT NULL::character varying NULL,
	request_path varchar(255) DEFAULT NULL::character varying NULL,
	request_method varchar(20) DEFAULT NULL::character varying NULL,
	request_ip varchar(50) DEFAULT NULL::character varying NULL,
	user_agent varchar(1000) DEFAULT NULL::character varying NULL,
	request_params text NULL,
	response_result text NULL,
	http_code int4 NULL,
	cost_time int8 NULL,
	status varchar(20) DEFAULT NULL::character varying NULL,
	error_msg text NULL,
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	CONSTRAINT sys_audit_log_pkey PRIMARY KEY (id)
);


-- askops_schema.sys_role definition

-- Drop table

-- DROP TABLE askops_schema.sys_role;

CREATE TABLE askops_schema.sys_role (
	id serial4 NOT NULL,
	role_code varchar(50) NOT NULL,
	role_name varchar(255) NOT NULL,
	is_super_admin bool DEFAULT false NULL,
	CONSTRAINT sys_role_pkey PRIMARY KEY (id),
	CONSTRAINT uk_role_code UNIQUE (role_code)
);


-- askops_schema.sys_user definition

-- Drop table

-- DROP TABLE askops_schema.sys_user;

CREATE TABLE askops_schema.sys_user (
	id serial4 NOT NULL,
	username varchar(255) NOT NULL,
	"password" varchar(255) NOT NULL,
	enabled bool DEFAULT true NULL,
	permission_version int8 DEFAULT 1 NULL,
	CONSTRAINT sys_user_pkey PRIMARY KEY (id),
	CONSTRAINT sys_user_username_key UNIQUE (username)
);


-- askops_schema.t_agent definition

-- Drop table

-- DROP TABLE askops_schema.t_agent;

CREATE TABLE askops_schema.t_agent (
	id bigserial NOT NULL,
	"name" varchar(64) NOT NULL,
	ip varchar(32) NOT NULL,
	port int4 NOT NULL,
	group_id int8 NULL,
	status varchar(16) DEFAULT 'REGISTERING'::character varying NOT NULL,
	last_heartbeat_time timestamp NULL,
	failcount int4 DEFAULT 0 NOT NULL,
	heartbeat_timeout_sec int4 DEFAULT 30 NOT NULL,
	client_id varchar(64) NOT NULL,
	create_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
	update_time timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
	CONSTRAINT t_agent_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_agent_name ON askops_schema.t_agent USING btree (name);
CREATE UNIQUE INDEX uk_client_id ON askops_schema.t_agent USING btree (client_id);

-- Table Triggers

create trigger trigger_t_agent_update before
update
    on
    askops_schema.t_agent for each row execute function askops_schema.update_modified_column();


-- askops_schema.t_group definition

-- Drop table

-- DROP TABLE askops_schema.t_group;

CREATE TABLE askops_schema.t_group (
	id bigserial NOT NULL,
	group_name varchar(100) NOT NULL,
	CONSTRAINT t_group_group_name_key UNIQUE (group_name),
	CONSTRAINT t_group_pkey PRIMARY KEY (id)
);


-- askops_schema.role_group_mapping definition

-- Drop table

-- DROP TABLE askops_schema.role_group_mapping;

CREATE TABLE askops_schema.role_group_mapping (
	role_id int4 NOT NULL,
	group_id int4 NOT NULL,
	CONSTRAINT role_group_mapping_pkey PRIMARY KEY (role_id, group_id),
	CONSTRAINT role_group_mapping_group_id_fkey FOREIGN KEY (group_id) REFERENCES askops_schema.t_group(id)
);


-- askops_schema.sys_permission definition

-- Drop table

-- DROP TABLE askops_schema.sys_permission;

CREATE TABLE askops_schema.sys_permission (
	id serial4 NOT NULL,
	permission_code varchar(100) NOT NULL,
	permission_name varchar(100) NOT NULL,
	parent_id int4 NULL,
	"type" varchar(20) NOT NULL,
	url_pattern varchar(255) NULL,
	http_method varchar(10) NULL,
	"path" varchar(255) NULL,
	component varchar(255) NULL,
	icon varchar(50) NULL,
	sort int4 DEFAULT 0 NULL,
	visible bool DEFAULT true NULL,
	description varchar(255) NULL,
	created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	CONSTRAINT sys_permission_permission_code_key UNIQUE (permission_code),
	CONSTRAINT sys_permission_pkey PRIMARY KEY (id),
	CONSTRAINT fk_parent FOREIGN KEY (parent_id) REFERENCES askops_schema.sys_permission(id)
);


-- askops_schema.user_role_mapping definition

-- Drop table

-- DROP TABLE askops_schema.user_role_mapping;

CREATE TABLE askops_schema.user_role_mapping (
	user_id int4 NOT NULL,
	role_id int4 NOT NULL,
	CONSTRAINT user_role_mapping_pkey PRIMARY KEY (user_id, role_id),
	CONSTRAINT user_role_mapping_user_id_fkey FOREIGN KEY (user_id) REFERENCES askops_schema.sys_user(id)
);


-- askops_schema.role_permission_mapping definition

-- Drop table

-- DROP TABLE askops_schema.role_permission_mapping;

CREATE TABLE askops_schema.role_permission_mapping (
	role_id int4 NOT NULL,
	permission_id int4 NOT NULL,
	CONSTRAINT role_permission_mapping_pkey PRIMARY KEY (role_id, permission_id),
	CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES askops_schema.sys_permission(id)
);