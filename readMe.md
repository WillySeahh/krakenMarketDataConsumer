# Candle 1m Problem

## Assumptions / Important Context
1. Chose only 1 symbol/trading pair to build the orderbook and 1m candle. Chosen: **ALGO/USD**
2. Used Kraken's Spot Websocket API v2 (https://docs.kraken.com/api/docs/websocket-v2/book/)
3. Assumed that the response received has not been tampered with (did not perform CRC checksum on validity of packet)
4. Since the response of the Kraken uses time format "2022-12-25T09:30:59.123456Z", when we refer to time we also use
the same format. (Not in epoch)
   
5. Upon starting the program, we have a Scheduler that runs once every 15s that prints out all the 1m candle. 
The first time it runs there is no candle output as the book is still building.
   
Else it should like something like this
>===================================     
Printing all 1m candles every 15s       
Candle{timestamp=2024-10-05T16:38:00Z, open=0.126090, high=0.126105, low=0.126020, close=0.126080, ticks=205}              
Candle{timestamp=2024-10-05T16:37:00Z, open=0.126185, high=0.126300, low=0.126090, close=0.126090, ticks=361}       
Candle{timestamp=2024-10-05T16:36:00Z, open=0.126485, high=0.126485, low=0.126185, close=0.126185, ticks=429}       
Candle{timestamp=2024-10-05T16:35:00Z, open=0.126380, high=0.126495, low=0.126380, close=0.126485, ticks=333}       
Candle{timestamp=2024-10-05T16:34:00Z, open=0.126380, high=0.126380, low=0.126380, close=0.126380, ticks=11}        
End of candles. Next candles printing in 15s.       
===================================     

6. Entry point of application is: **KrakenClient#main()**
7. Used Maven 3.6.3, Java Azul Zulu 17.0.12, required dependencies are in pom.xml


## Task
There are 3 main sub problem here, and I will go through how I tackled each sub problem below.
- Integrate with an exchange (we recommend Kraken) to get tick level data using Websockets
- Using snapshot and delta messages, build the correct view of the tick level orderbook in memory
- Use this tick level order book data to compute 1m candle data and log the output to console


## Integrate with an exchange to get tick level data using Websockets
I used an open source public library (Credit: https://github.com/TooTallNate/Java-WebSocket?tab=readme-ov-file)
to get a skeleton Websocket client up and running. 

Since the response is in Json format, I also made use of a Json parsing library
(The dependencies are in pom.xml)

When we start the program, able to see these messages 

(For final submission: Removed redundant print statement to keep terminal clean, hence might not see below)
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
Built an orderbook using OrderBookTreeMap which contains 2 treemap, one for bids and one for asks. 
The one for bids will be a maxHeapTreeMap, while the asks will be minHeapTreeMap. 

After subscribing to the websocket, 1 snapshot will be automatically sent. Use it to build the initial orderbook view
and then the subsequent updates will update tick by tick.

View the orderbook status using `System.out.println(book.toString());`

### Performed benchmarking on how long it needs to process each update tick
For HFT/Market makers, latency is crucial and bench marking have to be done. 
Hence, I performed a simple performance benchmarking. 


I performed benchmarking (locally) over 15min intervals and found that the
- Two treemap method average processing time of every update tick is **12 microseconds** 
- Used: `long elapsedTimeMicros = (endTime.toEpochMilli() - startTime.toEpochMilli()) * 1000 + (endTime.getNano() - startTime.getNano()) / 1000`


With additional optimization such as Thread Affinity / pinning main thread to 1 cpu core reducing context switches, we are able to get
down into the high nanoseconds ranges. 

## Use this tick level order book data to compute 1m candle data and log the output to console
With the orderbook built, we can build the candle. After processing every tick, we
can derive the BestBid/BestAsk (BB BA) from the book, and use it to compute the candle fields. 

Candles timestamp are rounded down to the minute. 

## Other information
1. Did perform simple sanity checks such as:
    - Ensure that highest bid < lowest ask when building the tick level order book
        - When this happens, log it out instead of throwing an exception. 
2. There’s always at least one bid and ask present
    - This should only happens once, upon the very start of the program where the book 
    is not built yet.
   - When this happens, log it out instead of throwing an exception. 





