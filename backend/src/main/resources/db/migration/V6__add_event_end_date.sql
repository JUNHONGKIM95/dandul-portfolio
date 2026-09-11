alter table date_events add column end_date date;
update date_events set end_date = event_date where end_date is null;
