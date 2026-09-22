-- Travel domain: trips, activities, and transport

CREATE TABLE IF NOT EXISTS travels (
    id                BIGSERIAL PRIMARY KEY,
    title             VARCHAR(255) NOT NULL,
    description       TEXT,
    destination_country VARCHAR(100) NOT NULL,
    destination_city  VARCHAR(100) NOT NULL,
    start_date        DATE NOT NULL,
    end_date          DATE NOT NULL,
    duration_days     INTEGER NOT NULL,
    price             NUMERIC(12, 2) NOT NULL,
    capacity          INTEGER NOT NULL,
    available_slots   INTEGER NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    manager_id        BIGINT NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_travels_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_travels_slots CHECK (available_slots >= 0 AND available_slots <= capacity),
    CONSTRAINT chk_travels_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_travels_duration CHECK (duration_days > 0),
    CONSTRAINT chk_travels_price CHECK (price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_travels_manager ON travels (manager_id);
CREATE INDEX IF NOT EXISTS idx_travels_status ON travels (status);
CREATE INDEX IF NOT EXISTS idx_travels_dates ON travels (start_date, end_date);

CREATE TABLE IF NOT EXISTS travel_activities (
    id          BIGSERIAL PRIMARY KEY,
    travel_id   BIGINT NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    day_number  INTEGER,
    start_time  TIME,
    end_time    TIME,
    CONSTRAINT fk_travel_activities_travel FOREIGN KEY (travel_id) REFERENCES travels (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_travel_activities_travel ON travel_activities (travel_id);

CREATE TABLE IF NOT EXISTS travel_transport (
    id          BIGSERIAL PRIMARY KEY,
    travel_id   BIGINT NOT NULL,
    type        VARCHAR(50) NOT NULL,
    provider    VARCHAR(255),
    departure   VARCHAR(255),
    arrival     VARCHAR(255),
    departure_time TIMESTAMPTZ,
    arrival_time   TIMESTAMPTZ,
    CONSTRAINT fk_travel_transport_travel FOREIGN KEY (travel_id) REFERENCES travels (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_travel_transport_travel ON travel_transport (travel_id);
