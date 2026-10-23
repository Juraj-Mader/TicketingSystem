INSERT INTO venue (id, name, address, total_capacity)
VALUES (1, 'Stadion Tehelne Pole', 'Bratislava', 22500);

INSERT INTO event (id, name, venue_id, total_capacity, left_capacity, ticket_price)
VALUES (1, 'BMTH Concert', 1, 22000, 22000, 99.99);

INSERT INTO customer (id, name, email, address)
VALUES (1, 'Jozko Mrkvicka', 'jozko.mrkvicka@sxlovexnsko.com', 'Bratislavova 12');