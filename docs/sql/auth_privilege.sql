-- 学习中心管理端接口权限，数据库：zk_auth
-- 网关会拦截配置在 privilege 表中的接口，因此新增管理端接口后必须补齐权限数据。
-- 执行后需重启 auth-service，或通过权限管理接口触发 Redis 权限缓存刷新。

USE zk_auth;

-- 管理端笔记接口
INSERT INTO privilege (id, menu_id, intro, method, uri, internal)
SELECT 1800000000000000001, NULL, '管理端分页查询笔记', 'GET', '/admin/notes/page', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM privilege WHERE method = 'GET' AND uri = '/admin/notes/page');
INSERT INTO privilege (id, menu_id, intro, method, uri, internal)
SELECT 1800000000000000002, NULL, '管理端查询笔记详情', 'GET', '/admin/notes/{id}', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM privilege WHERE method = 'GET' AND uri = '/admin/notes/{id}');
INSERT INTO privilege (id, menu_id, intro, method, uri, internal)
SELECT 1800000000000000003, NULL, '管理端隐藏笔记', 'PUT', '/admin/notes/{id}/hidden/{hidden}', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM privilege WHERE method = 'PUT' AND uri = '/admin/notes/{id}/hidden/{hidden}');

-- 管理端回答/评论接口
INSERT INTO privilege (id, menu_id, intro, method, uri, internal)
SELECT 1800000000000000004, NULL, '管理端分页查询回答或评论', 'GET', '/admin/replies/page', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM privilege WHERE method = 'GET' AND uri = '/admin/replies/page');
INSERT INTO privilege (id, menu_id, intro, method, uri, internal)
SELECT 1800000000000000005, NULL, '管理端查询回答或评论详情', 'GET', '/admin/replies/{id}', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM privilege WHERE method = 'GET' AND uri = '/admin/replies/{id}');
INSERT INTO privilege (id, menu_id, intro, method, uri, internal)
SELECT 1800000000000000006, NULL, '管理端隐藏回答或评论', 'PUT', '/admin/replies/{id}/hidden/{hidden}', 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM privilege WHERE method = 'PUT' AND uri = '/admin/replies/{id}/hidden/{hidden}');

-- 绑定到超级管理员角色（role_id = 1）
INSERT INTO role_privilege (role_id, privilege_id)
SELECT 1, p.id
FROM privilege p
WHERE p.uri IN (
    '/admin/notes/page',
    '/admin/notes/{id}',
    '/admin/notes/{id}/hidden/{hidden}',
    '/admin/replies/page',
    '/admin/replies/{id}',
    '/admin/replies/{id}/hidden/{hidden}'
)
AND NOT EXISTS (
    SELECT 1 FROM role_privilege rp WHERE rp.role_id = 1 AND rp.privilege_id = p.id
);
