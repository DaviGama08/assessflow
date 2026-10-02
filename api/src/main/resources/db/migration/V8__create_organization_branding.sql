CREATE TABLE organization_branding (
    organization_id uuid PRIMARY KEY REFERENCES organizations(id),
    display_name varchar(200) NOT NULL,
    logo_url varchar(500),
    primary_color varchar(7),
    secondary_color varchar(7),
    CONSTRAINT organization_branding_display_name_not_blank CHECK (length(btrim(display_name)) > 0),
    CONSTRAINT organization_branding_primary_color_hex
        CHECK (primary_color IS NULL OR primary_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT organization_branding_secondary_color_hex
        CHECK (secondary_color IS NULL OR secondary_color ~ '^#[0-9A-Fa-f]{6}$')
);
