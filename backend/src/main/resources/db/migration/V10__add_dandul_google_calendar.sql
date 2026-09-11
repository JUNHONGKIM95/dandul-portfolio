alter table google_calendar_connections
    add column google_calendar_id varchar(1024);

alter table google_calendar_connections
    add column granted_scope varchar(1000);

alter table google_calendar_event_links
    add column google_calendar_id varchar(1024);

update google_calendar_event_links
set google_calendar_id = 'primary'
where google_calendar_id is null;

alter table google_calendar_event_links
    alter column google_calendar_id set not null;
