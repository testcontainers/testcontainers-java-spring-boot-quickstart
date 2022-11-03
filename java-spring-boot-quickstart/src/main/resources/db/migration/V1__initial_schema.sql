create table todos
(
    id           bigserial    not null,
    title        varchar(200) not null,
    completed    boolean default false,
    order_number int,
    primary key (id)
)