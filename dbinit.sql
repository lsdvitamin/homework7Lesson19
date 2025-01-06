create table if not exists users (id bigserial primary key, login varchar(255), pass varchar(255), nickname varchar(255));
create table if not exists  bonuses (id bigserial primary key, owner_login varchar(255), amount int);
