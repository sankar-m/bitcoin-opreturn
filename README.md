***Bitcoin OP_RETURN Data Extractor***

This project is a Kotlin-based application that connects to the Bitcoin signet network, retrieves OP_RETURN data from specific transactions, and stores it in a PostgreSQL database. Additionally, it provides a RESTful API using Ktor to serve the stored OP_RETURN data.

**Features**

Connects to the Bitcoin signet network using JSON-RPC.

Extracts and stores OP_RETURN data from Bitcoin transactions.

Provides a RESTful API to retrieve stored OP_RETURN data.

Stores data in a PostgreSQL database.

**Prerequisites**

Kotlin 1.9.0

Gradle 8.10 (or compatible version)

PostgreSQL (version 14 or compatible)

Bitcoin Core (configured for the signet network)

Setup
1. Clone the Repository
````
   git clone https://github.com/yourusername/bitcoin-opreturn-extractor.git
   cd bitcoin-opreturn-extractor
````
2. Install Dependencies
   Make sure you have the necessary dependencies installed. You can install them using Gradle:

````
./gradlew build --refresh-dependencies
````

3. Configure PostgreSQL
   Start PostgreSQL (if not already running):

````
brew services start postgresql@14
Create the Database:

createdb opreturn_db
Create the Table:
````

````

CREATE TABLE op_return_data (
id SERIAL PRIMARY KEY,
txid VARCHAR(64) NOT NULL,
blockhash VARCHAR(64) NOT NULL,
op_return_hex TEXT NOT NULL
);

````
4. Configure Bitcoin Core
   Ensure that Bitcoin Core is configured for the signet network and that RPC is enabled:

Edit bitcoin.conf:

````
signet=1
rpcuser=testuser
rpcpassword=testpass
Start Bitcoin Core:
````

````bitcoind -signet -conf=/Users/yourusername/.bitcoin/bitcoin.conf -daemon````

Running the Application
1. Start the Application
   To start the application, use:

```
./gradlew run
```

2. Access the REST API
   The application will start a Ktor server on localhost:8080. You can retrieve OP_RETURN data using the following endpoint:


```curl http://localhost:8080/opreturn/{txid}```

Replace {txid} with the transaction ID you want to query.