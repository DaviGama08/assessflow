ALTER TABLE users ADD COLUMN account_type varchar(20) NOT NULL DEFAULT 'STUDENT'
    CHECK (account_type IN ('STUDENT', 'TEACHER'));

UPDATE users u SET account_type = 'TEACHER'
WHERE EXISTS (SELECT 1 FROM organization_members m WHERE m.user_id = u.id
    AND m.status = 'ACTIVE' AND m.role IN ('OWNER', 'ADMIN', 'INSTRUCTOR'));
