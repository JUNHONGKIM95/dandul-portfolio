create table hiking_records (
    id varchar(36) primary key,
    mountain_name varchar(120) not null,
    location varchar(255) not null,
    elevation_meter integer not null,
    climbed_at date not null,
    latitude double precision,
    longitude double precision,
    memo varchar(1000),
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_hiking_records_climbed_at on hiking_records(climbed_at);
create index idx_hiking_records_mountain_name on hiking_records(mountain_name);
