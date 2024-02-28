-- Create users table
CREATE TABLE users
(
    user_id uuid PRIMARY KEY DEFAULT gen_random_uuid (),
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    age INT,
    gender VARCHAR(10),
    country VARCHAR(50),
    phone_number VARCHAR(35)
);

-- Insert dummy data
INSERT INTO users
    (username, email, age, gender, country, phone_number)
VALUES
    ('JohnDoe', 'john.doe@example.com', 28, 'Male', 'USA', '+1 (555) 123-4567'),
    ('JaneSmith', 'jane.smith@example.com', 25, 'Female', 'Canada', '+1 (555) 987-6543'),
    ('BobJohnson', 'bob.johnson@example.com', 32, 'Male', 'UK', '+44 20 7123 4567');