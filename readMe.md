# Candle 1m Problem

## Assumptions / Important Context
1. Chose only 1 symbol/trading pair to build the orderbook and 1m candle. Chosen: **ALGO/USD**
2. Used Kraken's Spot Websocket API v2 (https://docs.kraken.com/api/docs/websocket-v2/book/)
3. Assumed that the response received has not been tampered with (did not perform CRC checksum on validity of packet)
4. Since the response of the Kraken uses time format "2022-12-25T09:30:59.123456Z", when we refer to time we also use
the same format. (Not in epoch)
   
5. Upon starting the program, we have a Scheduler that runs once every minute that prints out all the 1m candle. 
The first time it runs there is no candle output as the book is still building.
   
On the 2nd minute onwards it should be printing something like the below. 
> This task runs every minute.
> 
> Candle{startTime=2024-10-03T02:10:00Z, open=0.122705, high=0.122885, low=0.122680, close=0.122885, volume=0.0, ticks=386}
> Candle{startTime=2024-10-03T02:09:00Z, open=0.122485, high=0.122720, low=0.122485, close=0.122710, volume=0.0, ticks=132}

6. Entry point of application is: **KrakenClient#main()**

## Task
There are 3 main sub problem here and I will go through how I tackled each sub problem below.
- Integrate with an exchange (we recommend Kraken) to get tick level data using Websockets
- Using snapshot and delta messages, build the correct view of the tick level orderbook in memory
- Use this tick level order book data to compute 1m candle data and log the output to console


## Integrate with an exchange to get tick level data using Websockets
I used an open source public library (Credit: https://github.com/TooTallNate/Java-WebSocket?tab=readme-ov-file)
to get a skeleton Websocket client up and running. 

In the pom.xml 
>         <dependency>
>            <groupId>org.java-websocket</groupId>
>            <artifactId>Java-WebSocket</artifactId>
>            <version>1.5.7</version>
>        </dependency>


Since the response is in Json format, I also made use of a Json parsing library
>         <dependency>
>            <groupId>com.google.code.gson</groupId>
>            <artifactId>gson</artifactId>
>            <version>2.10.1</version>
>        </dependency>

When we start the program, able to see these messages
> new connection opened
> 
> Sent Subscription Message: {"method": "subscribe", "params": {"channel": "book", "symbol": ["ALGO/USD"]}}
> 
> received message: {"channel":"status","data":[{"api_version":"v2","connection_id":14949390454975033707,"system":"online","version":"2.0.8"}],"type":"update"}
>
> received message: {"channel":"book","type":"snapshot","data":[{"symbol":"ALGO/USD","bids":[{"price":0.12291,"qty":27039.62776299},{"price":0.12290,"qty":8136.33288180},{"price":0.12287,"qty":10984.20817349},{"price":0.12283,"qty":28027.05025639},{"price":0.12280,"qty":9705.60000000},{"price":0.12279,"qty":15215.53522714},{"price":0.12275,"qty":4067.00000000},{"price":0.12274,"qty":30639.39949109},{"price":0.12268,"qty":2531.96119986},{"price":0.12267,"qty":16299.00000000}],"asks":[{"price":0.12307,"qty":600.00000000},{"price":0.12308,"qty":4042.11919517},{"price":0.12312,"qty":10284.53891000},{"price":0.12314,"qty":16155.08885299},{"price":0.12315,"qty":5186.05778473},{"price":0.12316,"qty":32828.24000000},{"price":0.12317,"qty":7751.65718582},{"price":0.12318,"qty":32298.82000000},{"price":0.12319,"qty":1864.66899600},{"price":0.12322,"qty":4718.99755630}],"checksum":2375857985}]}
>
> received message: {"channel":"book","type":"update","data":[{"symbol":"ALGO/USD","bids":[{"price":0.12275,"qty":6597.51731160}],"asks":[],"checksum":1100635717,"timestamp":"2024-10-03T02:46:57.050170Z"}]}
> ... will receive update tick message continously


## Using snapshot and delta messages, build the correct view of the tick level orderbook in memory





