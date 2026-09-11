alter table date_events add column category varchar(20) default 'together';
update date_events set category = 'together' where category is null;
