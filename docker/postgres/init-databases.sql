-- Initialize separate databases for each microservice
CREATE DATABASE docintel_auth;
CREATE DATABASE docintel_docs;
CREATE DATABASE docintel_ai;

-- Grant all privileges to postgres user
GRANT ALL PRIVILEGES ON DATABASE docintel_auth TO postgres;
GRANT ALL PRIVILEGES ON DATABASE docintel_docs TO postgres;
GRANT ALL PRIVILEGES ON DATABASE docintel_ai TO postgres;
