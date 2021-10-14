create table market_quote
(
	id int auto_increment
		primary key,
	curve_name varchar(255) not null,
	instrument_type varchar(255) not null,
	instrument_name varchar(255) not null,
	tenor varchar(255) not null,
	quote varchar(255) not null,
	maturity_date varchar(255) not null,
	m_h_rep_date varchar(255) not null
);

