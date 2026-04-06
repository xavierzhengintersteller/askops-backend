INSERT INTO askops_schema.sys_permission (permission_code,permission_name,parent_id,"type",url_pattern,http_method,"path",component,icon,sort,visible,description,created_at) VALUES
	 ('container:menu','容器管理',NULL,'menu',NULL,NULL,'/container','container/Index','el-icon-box',1,true,'容器管理菜单','2026-03-28 22:17:51.746311'),
	 ('container:list','查看容器',3,'api','/api/container/**','GET',NULL,NULL,NULL,0,true,'查看容器','2026-03-28 22:18:40.308904'),
	 ('container:create','创建容器',3,'api','/api/container','POST',NULL,NULL,NULL,0,true,'创建容器','2026-03-28 22:18:40.308904'),
	 ('container:delete','删除容器',3,'button',NULL,NULL,NULL,NULL,NULL,0,true,'删除按钮','2026-03-28 22:18:56.157655'),
	 ('dashboard:menu','主页',NULL,'menu',NULL,NULL,'/dashboard','dashboard/Index','el-icon-house',0,true,'主页菜单','2026-03-29 14:57:06.892202'),
	 ('admin:menu','后台管理',NULL,'menu',NULL,NULL,'/admin','admin/Index','el-icon-setting',10,true,'后台管理菜单','2026-03-29 14:57:06.892202'),
	 ('admin:user:menu','账号管理',27,'menu',NULL,NULL,'/admin/user','admin/user/Index','el-icon-user',20,true,'账号管理菜单','2026-03-29 14:57:06.892202'),
	 ('admin:group:menu','组管理',27,'menu',NULL,NULL,'/admin/group','admin/group/Index','el-icon-user-solid',30,true,'组管理菜单','2026-03-29 14:57:06.892202'),
	 ('admin:permission:menu','权限管理',27,'menu',NULL,NULL,'/admin/permission','admin/permission/Index','el-icon-lock',40,true,'权限管理菜单','2026-03-29 14:57:06.892202'),
	 ('admin:audit:menu','Audit日志管理',27,'menu',NULL,NULL,'/admin/audit','admin/audit/Index','el-icon-document',50,true,'审计日志菜单','2026-03-29 14:57:06.892202');
INSERT INTO askops_schema.sys_permission (permission_code,permission_name,parent_id,"type",url_pattern,http_method,"path",component,icon,sort,visible,description,created_at) VALUES
	 ('admin:user:list','查看账号',28,'api','/api/admin/user/**','GET',NULL,NULL,NULL,0,true,'查看账号','2026-03-29 14:57:06.892202'),
	 ('admin:user:create','创建账号',28,'api','/api/admin/user','POST',NULL,NULL,NULL,0,true,'创建账号','2026-03-29 14:57:06.892202'),
	 ('admin:user:update','修改账号',28,'api','/api/admin/user','PUT',NULL,NULL,NULL,0,true,'修改账号','2026-03-29 14:57:06.892202'),
	 ('admin:user:delete','删除账号',28,'button',NULL,NULL,NULL,NULL,NULL,0,true,'删除账号按钮','2026-03-29 14:57:06.892202'),
	 ('admin:group:list','查看组',33,'api','/api/admin/group/**','GET',NULL,NULL,NULL,0,true,'查看组','2026-03-29 14:57:06.892202'),
	 ('admin:group:create','创建组',33,'api','/api/admin/group','POST',NULL,NULL,NULL,0,true,'创建组','2026-03-29 14:57:06.892202'),
	 ('admin:group:update','修改组',33,'api','/api/admin/group','PUT',NULL,NULL,NULL,0,true,'修改组','2026-03-29 14:57:06.892202'),
	 ('admin:group:delete','删除组',33,'button',NULL,NULL,NULL,NULL,NULL,0,true,'删除组按钮','2026-03-29 14:57:06.892202'),
	 ('admin:permission:list','查看权限',38,'api','/api/admin/permission/**','GET',NULL,NULL,NULL,0,true,'查看权限','2026-03-29 14:57:06.892202'),
	 ('admin:permission:update','分配权限',38,'api','/api/admin/permission','PUT',NULL,NULL,NULL,0,true,'分配权限','2026-03-29 14:57:06.892202');
INSERT INTO askops_schema.sys_permission (permission_code,permission_name,parent_id,"type",url_pattern,http_method,"path",component,icon,sort,visible,description,created_at) VALUES
	 ('admin:audit:list','查看日志',41,'api','/api/admin/audit/**','GET',NULL,NULL,NULL,0,true,'查看审计日志','2026-03-29 14:57:06.892202');
