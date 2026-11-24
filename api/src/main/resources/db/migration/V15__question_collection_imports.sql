CREATE TABLE question_collection_imports (
    organization_id uuid NOT NULL REFERENCES organizations(id),
    collection varchar(100) NOT NULL,
    PRIMARY KEY (organization_id, collection)
);
