create table google_calendar_connections (
    username varchar(40) primary key,
    encrypted_access_token text not null,
    encrypted_refresh_token text not null,
    access_token_expires_at timestamp not null,
    connected_at timestamp not null,
    updated_at timestamp not null
);

create table google_calendar_event_links (
    id varchar(36) primary key,
    username varchar(40) not null,
    date_event_id varchar(36) not null,
    google_event_id varchar(1024) not null,
    synced_at timestamp not null,
    constraint uk_google_calendar_event_link unique (username, date_event_id)
);

create index idx_google_calendar_event_links_username
    on google_calendar_event_links(username);

create index idx_google_calendar_event_links_date_event
    on google_calendar_event_links(date_event_id);
