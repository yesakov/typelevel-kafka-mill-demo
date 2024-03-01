# typelevel-kafka-mill-demo

This is a simple demo app that demonstrates the usage of the Typelevel stack (Cats Effect, http4s, fs2, fs2-kafka, circe, doobie),
alongside Apache Kafka, and PostgreSQL DB. This project uses [Mill](https://mill-build.com/mill/Intro_to_Mill.html) as a build tool.
For the web app part, Scala.js with the [Tyrian](https://tyrian.indigoengine.io/) library is used.

## Structure and Description

The project consists of 3 standalone modules (`producer`, `consumer`, and `web_client`) and one `shared` module across all 3 modules.

The `producer` module periodically fetches open crypto data from the CoinGecko API and produces this data to Kafka.<br>
The `consumer` module consumes crypto data from Kafka, stores it in the DB, and provides an endpoint for retrieving the latest crypto data and a WebSocket endpoint 
for receiving real-time data from Kafka. <br>
The `web_client` is a simple single-page app that displays real-time crypto data using a WebSocket connection.<br>
The `shared` module only contains a case class for storing crypto data across other modules.

Considering the final result of this project (displaying real-time data), the architecture is absolutely overcomplicated; the main point is just to 
utilize the mentioned libraries and technologies and demonstrate how they work.

## Setting Up

To build and run this project, you need to have `docker`, `mill`, and `npm` installed.
First, run <br>
```shell
docker-compose up
```
<br>from the `/db` folder; this will create a PostgreSQL DB with 2 tables and basic inserts. <br>Then,
run 
```shell
docker-compose up
``` 

from the `/kafka` folder; this will run a Kafka container with [kafka-ui](https://github.com/provectus/kafka-ui). 
You can check the UI at [http://localhost:8088/](http://localhost:8088/).

Then, to build and run modules use the 
```shell
mill producer
```

and 
```shell
mill consumer
```

command from the root folder, and 
```shell
mill web_client.fastLinkJS
``` 
to compile Scala.js `web_client`. 
To run `web_client` from the `/web_client` folder, run
```shell
npm install
``` 
and then 
```shell
npm run start
```
this will run a server at [http://localhost:1234](http://localhost:1234). That's it.
Web app page will looks like this: <br><br>
![Alt text](web_app.png?raw=true "Title")

### Useful Commands

`mill mill.bsp.BSP/install` to install a BSP connection file `.bsp/mill-bsp.json` for [Mill](https://mill-build.com/mill/Installation_IDE_Support.html#_build_server_protocol_bsp).

`docker exec -it db-db-1 psql -U docker` to enter the PostgreSQL shell.
