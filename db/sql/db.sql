CREATE TABLE cryptocurrencies
(
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE crypto_prices
(
    id SERIAL PRIMARY KEY,
    crypto_id INTEGER NOT NULL,
    usd DECIMAL(20, 8),
    usd_24h_change DECIMAL(10, 8),
    eur DECIMAL(20, 8),
    eur_24h_change DECIMAL(10, 8),
    uah DECIMAL(20, 8),
    uah_24h_change DECIMAL(10, 8),
    last_updated_at TIMESTAMP
    WITHOUT TIME ZONE,
    FOREIGN KEY
    (crypto_id) REFERENCES cryptocurrencies
    (id)
);

    INSERT INTO cryptocurrencies
        (name)
    VALUES
        ('bitcoin'),
        ('ethereum'),
        ('ripple'),
        ('litecoin'),
        ('cardano'),
        ('polkadot'),
        ('bitcoin-cash'),
        ('stellar'),
        ('chainlink'),
        ('binancecoin');