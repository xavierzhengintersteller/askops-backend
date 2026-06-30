INSERT INTO askops_schema.sys_permission (permission_code,permission_name,parent_id,"type",url_pattern,http_method,"path",component,icon,sort,visible,description,created_at) VALUES
	 ('admin:role:list','查看role权限',33,'api','/api/admin/role/**','GET',NULL,NULL,NULL,0,true,'查看role权限','2026-05-10 22:59:43.994222'),
	 ('admin:role:all','修改role权限',33,'api','/api/admin/role/**','POST',NULL,NULL,NULL,0,true,'修改role权限','2026-05-10 22:56:15.507306'),
	 ('admin:role-group:list','查看角色组权限',33,'api','/api/admin/role-group/**','GET',NULL,NULL,NULL,0,true,'查看角色组权限','2026-05-10 23:20:09.722639'),
	 ('admin:user:list','查看账号',28,'api','/api/admin/user/**','GET',NULL,NULL,NULL,0,true,'查看账号','2026-03-29 14:57:06.892202'),
	 ('admin:role-group:all','分配角色组权限',33,'api','/api/admin/role-group/**','POST',NULL,NULL,NULL,0,true,'分配角色组权限','2026-05-10 23:20:09.722639'),
	 ('admin:role-permission:all','分配角色permission权限',33,'api','/api/admin/role-permission/**','POST',NULL,NULL,NULL,0,true,'分配角色permission权限','2026-05-10 23:20:09.722639'),
	 ('container:menu','容器管理',NULL,'menu',NULL,NULL,'/container','./container','el-icon-box',1,true,'容器管理菜单','2026-03-28 22:17:51.746311'),
	 ('user:permission:list','检索权限',NULL,'api','/api/user/permission-menu','GET',NULL,NULL,NULL,0,true,'检索权限','2026-04-06 19:18:25.14713'),
	 ('admin:user:menu','账号管理',27,'menu',NULL,NULL,'/admin/user','./admin/user','el-icon-user',20,true,'账号管理菜单','2026-03-29 14:57:06.892202'),
	 ('admin:menu','后台管理',NULL,'menu',NULL,NULL,'/admin','./admin','el-icon-setting',10,true,'后台管理菜单','2026-03-29 14:57:06.892202');
INSERT INTO askops_schema.sys_permission (permission_code,permission_name,parent_id,"type",url_pattern,http_method,"path",component,icon,sort,visible,description,created_at) VALUES
	 ('admin:role:menu','角色管理',27,'menu',NULL,NULL,'/admin/role','./admin/role','el-icon-user-solid',30,true,'角色管理菜单','2026-03-29 14:57:06.892202'),
	 ('dashboard:menu','主页',NULL,'menu',NULL,NULL,'/home','./home','el-icon-house',0,true,'主页菜单','2026-03-29 14:57:06.892202'),
	 ('admin:permission:list','查看permission权限',33,'api','/api/admin/permission/list','GET',NULL,NULL,NULL,0,true,'查看permission权限','2026-05-10 23:06:36.849694'),
	 ('admin:audit-log','审计日志',27,'menu',NULL,NULL,'/admin/audit-log','./admin/audit-log','el-icon-document',21,true,'审计日志管理菜单','2026-06-02 22:25:26.598134'),
	 ('container:restart:all','容器重启（单个）',3,'api','/api/containers/restart','POST',NULL,NULL,NULL,5,true,'容器重启/批量重启通用权限','2026-04-06 19:59:43.476052'),
	 ('container:restart','容器重启（批量）',3,'api','/api/containers/restart-batch','POST','','','',0,true,'重启容器权限','2026-06-06 11:54:38.409189'),
	 ('node:list','查看node列表权限',3,'api','/api/containers/nodes','GET','','','',0,true,'查看node列表权限','2026-06-06 11:54:38.409189'),
	 ('container:list','查看容器权限',3,'api','/api/containers/container**','GET','','','',0,true,'查看容器权限','2026-06-06 11:54:38.409189');
