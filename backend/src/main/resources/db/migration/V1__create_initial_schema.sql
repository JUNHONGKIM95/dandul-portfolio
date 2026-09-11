create table couple_profile (
    id varchar(36) primary key,
    boyfriend_name varchar(60) not null,
    girlfriend_name varchar(60) not null,
    relationship_start_date date not null,
    cover_photo_url varchar(1000),
    created_at timestamp not null,
    updated_at timestamp not null
);

create table date_events (
    id varchar(36) primary key,
    event_date date not null,
    meeting_time time,
    place varchar(255) not null,
    title varchar(120) not null,
    memo varchar(2000),
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_date_events_date on date_events(event_date);

create table media_items (
    id varchar(36) primary key,
    event_id varchar(36),
    media_type varchar(20) not null,
    url varchar(1000) not null,
    original_file_name varchar(255) not null,
    title varchar(120) not null,
    memo varchar(2000),
    favorite boolean not null default false,
    captured_at date,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_media_event foreign key (event_id) references date_events(id) on delete set null
);

create index idx_media_items_event_id on media_items(event_id);
create index idx_media_items_favorite on media_items(favorite);
create index idx_media_items_created_at on media_items(created_at);

insert into couple_profile (
    id,
    boyfriend_name,
    girlfriend_name,
    relationship_start_date,
    cover_photo_url,
    created_at,
    updated_at
) values (
    'default',
    '김준홍',
    '전소민',
    date '2024-03-30',
    null,
    current_timestamp,
    current_timestamp
);
