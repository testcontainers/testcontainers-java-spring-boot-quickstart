create table todos
(
    id           varchar(100)    not null,
    title        varchar(200) not null,
    completed    boolean default false,
    order_number int,
    primary key (id)
)