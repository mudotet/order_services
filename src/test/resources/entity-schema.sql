drop all objects;

create table users (
    id char(36) not null primary key,
    user_name varchar(255) not null,
    phone_number varchar(20),
    dob date,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null
);

create table addresses (
    id char(36) not null primary key,
    address varchar(500) not null,
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null
);

create table carts (
    id char(36) not null primary key,
    user_id char(36) not null unique references users(id),
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null
);

create table discounts (
    id char(36) not null primary key,
    discount_type varchar(36) not null,
    discount_value decimal(15,2) not null,
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null
);

create table notifications (
    id char(36) not null primary key,
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null
);

create table order_states (
    id char(36) not null primary key,
    state varchar(50) not null unique,
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null
);

create table payments (
    id char(36) not null primary key,
    payment_method varchar(50) not null,
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null
);

create table orders (
    id char(36) not null primary key,
    user_id char(36) not null references users(id),
    discount_id char(36) references discounts(id),
    payment_id char(36) references payments(id),
    address_id char(36) not null references addresses(id),
    order_state_id char(36) not null references order_states(id),
    subtotal decimal(15,2) default 0.00 not null check (subtotal >= 0),
    discount_amount decimal(15,2) default 0.00 not null check (discount_amount >= 0 and discount_amount <= subtotal),
    shipping_fee decimal(15,2) default 0.00 not null check (shipping_fee >= 0),
    total decimal(15,2) default 0.00 not null check (total >= 0),
    estimated_delivery date,
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null
);

create table order_returns (
    id char(36) not null primary key,
    return_code varchar(50) not null unique,
    order_id char(36) not null references orders(id),
    status varchar(50) not null,
    origin_type varchar(50) not null,
    refund_amount decimal(15,2) default 0.00 not null check (refund_amount >= 0),
    requested_at datetime default CURRENT_TIMESTAMP not null,
    received_at datetime,
    inspected_at datetime,
    restocked_at datetime,
    refunded_at datetime,
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null
);

create table permissions (
    id char(36) not null primary key,
    permission_name varchar(100) not null unique,
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null
);

create table products (
    id char(36) not null primary key,
    product_name varchar(255) not null,
    product_type varchar(100),
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null
);

create table product_variants (
    id char(36) not null primary key,
    product_id char(36) not null references products(id),
    product_variant varchar(255) not null,
    price decimal(15,2) not null check (price >= 0),
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null
);

create table cart_items (
    id char(36) not null primary key,
    cart_id char(36) not null references carts(id),
    product_variant_id char(36) not null references product_variants(id),
    product_quantity int not null check (product_quantity > 0),
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null,
    unique (cart_id, product_variant_id)
);

create table inventories (
    id char(36) not null primary key,
    product_variant_id char(36) not null unique references product_variants(id),
    quantity_in_stock int default 0 not null check (quantity_in_stock >= 0),
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null
);

create table order_items (
    id char(36) not null primary key,
    order_id char(36) not null references orders(id),
    product_variant_id char(36) not null references product_variants(id),
    unit_price decimal(15,2) not null check (unit_price >= 0),
    quantity int not null check (quantity > 0),
    line_total decimal(15,2) not null check (line_total >= 0),
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null
);

create table order_return_items (
    id char(36) not null primary key,
    order_return_id char(36) not null references order_returns(id),
    order_item_id char(36) not null references order_items(id),
    quantity int not null check (quantity > 0),
    reason_type varchar(50) not null,
    reason_detail varchar(500),
    condition_status varchar(50),
    unit_price decimal(15,2) not null check (unit_price >= 0),
    refund_amount decimal(15,2) not null check (refund_amount >= 0),
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null,
    unique (order_return_id, order_item_id)
);

create table roles (
    id char(36) not null primary key,
    role_name varchar(100) not null unique,
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null
);

create table role_permissions (
    role_id char(36) not null references roles(id),
    permission_id char(36) not null references permissions(id),
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null,
    primary key (role_id, permission_id)
);

create table tracking_logs (
    id char(36) not null primary key,
    order_id char(36) not null references orders(id),
    old_status char(36) references order_states(id),
    new_status char(36) not null references order_states(id),
    take_note varchar(500),
    location char(36) references addresses(id),
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null
);

create table user_discounts (
    id char(36) not null primary key,
    user_id char(36) not null references users(id),
    discount_id char(36) not null references discounts(id),
    status varchar(50) not null,
    received_at datetime,
    used_at datetime,
    expired_at datetime,
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null,
    unique (user_id, discount_id)
);

create table user_roles (
    user_id char(36) not null references users(id),
    role_id char(36) not null references roles(id),
    created_at datetime default CURRENT_TIMESTAMP not null,
    created_by char(36) references users(id),
    updated_at datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP not null,
    updated_by char(36) references users(id),
    deleted_at datetime,
    deleted tinyint default 0 not null,
    primary key (user_id, role_id)
);
