create table spark_structured_streaming_throughput
(
    id         bigint auto_increment primary key,
    timestamp  timestamp not null,
    throughput int       not null,
    job        varchar(255)   not null,
    fault      varchar(255)   not null,
    test_number int      not null
);
create table spark_structured_streaming_latency
(
    id         bigint auto_increment primary key,
    end_to_end_latency  bigint not null,
    processing_latency  bigint not null,
    output_timestamp  timestamp not null,
    job        varchar(255)   not null,
    fault      varchar(255)   not null,
    test_number int      not null
);
create table spark_structured_streaming_validate
(
    id         bigint auto_increment primary key,
    output_number  bigint not null,
    purchases_number  bigint not null,
    timestamp timestamp not null,
    job        varchar(255)   not null,
    fault      varchar(255)   not null,
    test_number int      not null
);

