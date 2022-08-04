-- apply alter tables
alter table url_check alter column title type varchar(255) using title::varchar(255);
alter table url_check alter column h1 type varchar(255) using h1::varchar(255);
