alter table date_events add column created_by varchar(40) default 'junhong';
alter table date_events add column creator_nickname varchar(40) default '준홍';

alter table media_items add column created_by varchar(40) default 'junhong';
alter table media_items add column creator_nickname varchar(40) default '준홍';

alter table hiking_records add column created_by varchar(40) default 'junhong';
alter table hiking_records add column creator_nickname varchar(40) default '준홍';
