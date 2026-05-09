create table payments (
    id uuid primary key,
    idempotency_key varchar(255) unique,
    request_hash varchar(128) not null,
    amount numeric(19, 2) not null,
    currency varchar(3) not null,
    reference varchar(255) not null,
    state varchar(32) not null,
    attempt_count integer not null,
    max_attempts integer not null,
    next_retry_at timestamptz,
    last_error text,
    payment_service_response_id varchar(255),
    payment_service_status varchar(64),
    payment_service_response text,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    completed_at timestamptz,
    version bigint not null
);

create index idx_payments_state_retry on payments (state, next_retry_at);
create index idx_payments_state_updated on payments (state, updated_at);

create table payment_attempts (
    id uuid primary key,
    payment_id uuid not null references payments(id),
    attempt_number integer not null,
    started_at timestamptz not null,
    finished_at timestamptz,
    result varchar(64),
    http_status integer,
    duration_ms bigint,
    error_message text,
    raw_response text
);

create index idx_payment_attempts_payment on payment_attempts (payment_id, attempt_number);

create table outbox_events (
    id uuid primary key,
    aggregate_id uuid not null,
    event_type varchar(128) not null,
    payload text not null,
    status varchar(32) not null,
    attempt_count integer not null,
    next_publish_at timestamptz not null,
    created_at timestamptz not null,
    published_at timestamptz,
    last_error text
);

create index idx_outbox_due on outbox_events (status, next_publish_at, created_at);

