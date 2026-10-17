-- 可重复执行；既有 item.project_id 保留为主项目，不改动现有记录。
CREATE TABLE IF NOT EXISTS item_project_link (
    item_id BIGINT NOT NULL REFERENCES item(id) ON DELETE CASCADE,
    project_id BIGINT NOT NULL REFERENCES project(id) ON DELETE CASCADE,
    PRIMARY KEY (item_id, project_id)
);
CREATE INDEX IF NOT EXISTS idx_item_project_link_project ON item_project_link(project_id, item_id);
