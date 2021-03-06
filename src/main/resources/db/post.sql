create table post (
  id serial,
  status varchar(100) not null,
  writer_id int,
  title varchar(5000) not null,
  content text,
  view int,
  comment_count bigint default 0,
  dev_post_url varchar(800),
  path_base varchar(500),
  image_path varchar[],
  create_at timestamp with time zone not null default now(),
  update_at timestamp with time zone not null default now(),
  constraint pk_post_id primary key(id),
  constraint fk_post_writer foreign key (writer_id) references member(id)
)