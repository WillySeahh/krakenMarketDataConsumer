# Candle 1m Problem

## Assumptions / Important Context
1. Used only 1 symbol/trading pair to build the orderbook and 1m candle. Chosen: **ALGO/USD**
2. Used Kraken's Spot Websocket API v2 (https://docs.kraken.com/api/docs/websocket-v2/book/)
3. Assumed that the response received has not been tampered with (did not perform CRC checksum on validity of packet)
   and **no packet drops**.
   - Packet drops happens once in awhile, and this may lead to bestBid >= bestAsk. 
4. Response of the Kraken uses time format "2022-12-25T09:30:59.123456Z", when we refer to time we also use
the same format. (Not in epoch)
   
5. Upon starting the program, we have a Scheduler that runs once every 15s that prints out all the 1m candle. 
The first time it runs there is no candle output as the book is still building.
Afterwards it should like something like this 
   
> ===================================     
Printing all 1m candles every 15s       
Candle{timestamp=2024-10-05T16:38:00Z, open=0.126090, high=0.126105, low=0.126020, close=0.126080, ticks=205}              
Candle{timestamp=2024-10-05T16:37:00Z, open=0.126185, high=0.126300, low=0.126090, close=0.126090, ticks=361}       
...      
End of candles. Next candles printing in 15s.       
===================================     

6. Used Maven 3.6.3, Java Azul Zulu 17.0.12, required dependencies are in pom.xml
7. Github Repo: https://github.com/WillySeahh/gsrTest 
8. Intentionally left some print statements, but commented out, to assist tracking of incoming message and orderbook state

## Results

### Completed main task and expected result
In `KrakenClient.main` hit Run. 
Should print out 1m candles every 15s

> New connection opened    
Sent Subscription Message: {"method": "subscribe", "params": {"channel": "book", "symbol": ["ALGO/USD"]}}      
===================================    
Printing all 1m candles every 15s      
End of candles. Next candles printing in 15s.      
===================================    
Error: Do not have at least 1 bid or 1 ask.     
===================================    
Printing all 1m candles every 15s      
Candle{timestamp=2024-10-07T12:31:00Z, open=0.127100, high=0.127115, low=0.127010, close=0.127010, ticks=118}     
End of candles. Next candles printing in 15s.      
===================================    
===================================    
Printing all 1m candles every 15s      
Candle{timestamp=2024-10-07T12:32:00Z, open=0.127010, high=0.127010, low=0.127010, close=0.127010, ticks=3}    
Candle{timestamp=2024-10-07T12:31:00Z, open=0.127100, high=0.127115, low=0.127010, close=0.127010, ticks=145}     
End of candles. Next candles printing in 15s.      
===================================    

The first `Error: Do not have at least 1 bid or 1 ask.` can be ignored as the orderbook initially has not bid and ask as
it is being built. 

### Completed bonus task and expected result
`KrakenClient` will continue printing the candles once every 15s, as mentioned above, **but will also
publish `candles` in string format on `localhost:9092` and topic `1m_Candle`**.

1. In `KrakenClient` search for `[Bonus task]` and remove the 5 commented out lines to set up Kafka Publisher
2. Download apache kafka, and start `zookeper-server-start.sh` passing in zookeper.properties files -> then start another terminal window
   -> start `kafka-server-start.sh` passing in server.properties files. Ensure it is started on localhost:9092
   
3. Run `KrakenClient.main` and `KafkaConsumerClass.main`
4. Expected to see the same candle being printed once every 15s. 


## Indepth explanation of code
There are 4 main sub problem here, and I will go through how I tackled each sub problem below.

- Integrate with an exchange (we recommend Kraken) to get tick level data using Websockets
  
- Using snapshot and delta messages, build the correct view of the tick level orderbook in memory
  - **Benchmarking in performance** - Done and explained below
   
- Use this tick level order book data to compute 1m candle data and log the output to console
  
- Bonus task: publishing 1m candle to kafka, polling and printing it. - Done and explained below


## Integrate with an exchange to get tick level data using Websockets
I used an open source public library (Credit: https://github.com/TooTallNate/Java-WebSocket?tab=readme-ov-file)
to get a skeleton Websocket client up and running. 

Since the response is in Json format, I also made use of a Json parsing library
(The dependencies are in pom.xml)

When we start the program, able to see these messages 

(For final submission: Removed redundant print statement to keep terminal clean, hence might not see below)

> new connection opened    
Sent Subscription Message: {"method": "subscribe", "params": {"channel": "book", "symbol": ["ALGO/USD"]}}      
received message: {"channel":"status","data":[{"api_version":"v2","connection_id":14949390454975033707,"system":"online","version":"2.0.8"}],"type":"update"}       
received message: {"channel":"book","type":"snapshot","data":[{"symbol":"ALGO/USD","bids":[{"price":0.12291,"qty":27039.62776299},{"price":0.12290,"qty":8136.33288180},{"price":0.12287,"qty":10984.20817349},{"price":0.12283,"qty":28027.05025639},{"price":0.12280,"qty":9705.60000000},{"price":0.12279,"qty":15215.53522714},{"price":0.12275,"qty":4067.00000000},{"price":0.12274,"qty":30639.39949109},{"price":0.12268,"qty":2531.96119986},{"price":0.12267,"qty":16299.00000000}],"asks":[{"price":0.12307,"qty":600.00000000},{"price":0.12308,"qty":4042.11919517},{"price":0.12312,"qty":10284.53891000},{"price":0.12314,"qty":16155.08885299},{"price":0.12315,"qty":5186.05778473},{"price":0.12316,"qty":32828.24000000},{"price":0.12317,"qty":7751.65718582},{"price":0.12318,"qty":32298.82000000},{"price":0.12319,"qty":1864.66899600},{"price":0.12322,"qty":4718.99755630}],"checksum":2375857985}]}        
received message: {"channel":"book","type":"update","data":[{"symbol":"ALGO/USD","bids":[{"price":0.12275,"qty":6597.51731160}],"asks":[],"checksum":1100635717,"timestamp":"2024-10-03T02:46:57.050170Z"}]}       
... will receive update tick message continuously      


## Using snapshot and delta messages, build the correct view of the tick level orderbook in memory
Built an orderbook using OrderBookTreeMap which contains 2 treemap, one for bids and one for asks. 
The one for bids will be a maxHeapTreeMap, while the asks will be minHeapTreeMap. 

After subscribing to the websocket, 1 snapshot will be automatically sent. Use it to build the initial orderbook view
and then the subsequent updates will update tick by tick.

View the orderbook status using `System.out.println(book.toString());`

### Performed benchmarking on how long it needs to process each update tick
For HFT/Market makers, latency is crucial and benchmarking have to be done. 
Hence, I performed a simple performance benchmarking. 

I performed benchmarking (locally) over 15min intervals and found that the

- Two treemap method average processing time of every update tick is **12 microseconds** 
  
- Used: `long elapsedTimeMicros = (endTime.toEpochMilli() - startTime.toEpochMilli()) * 1000 + (endTime.getNano() - startTime.getNano()) / 1000`

With additional optimization such as Thread Affinity / pinning main thread to 1 cpu core reducing context switches, we should be able to get
down into the high nanoseconds ranges. 

## Use this tick level order book data to compute 1m candle data and log the output to console
With the orderbook built, we can build the candle. After processing every tick, we
can derive the BestBid/BestAsk (BB BA) from the book, and use it to compute the candle fields. 

Candles timestamp are rounded down to the minute. 

In `KrakenClient.main` there is a schedules task that runs once every 15s, printing out all the 1m candles sorted
in descending timestamp. 

> ===================================       
Printing all 1m candles every 15s       
Candle{timestamp=2024-10-06T04:12:00Z, open=0.125470, high=0.125470, low=0.125465, close=0.125470, ticks=36}        
Candle{timestamp=2024-10-06T04:11:00Z, open=0.125465, high=0.125470, low=0.125465, close=0.125470, ticks=17}        
Candle{timestamp=2024-10-06T04:10:00Z, open=0.125655, high=0.125655, low=0.125465, close=0.125465, ticks=223}       
Candle{timestamp=2024-10-06T04:09:00Z, open=0.125560, high=0.125670, low=0.125560, close=0.125655, ticks=136}       
Candle{timestamp=2024-10-06T04:08:00Z, open=0.125565, high=0.125565, low=0.125555, close=0.125560, ticks=58}        
Candle{timestamp=2024-10-06T04:07:00Z, open=0.125465, high=0.125565, low=0.125465, close=0.125565, ticks=148}       
Candle{timestamp=2024-10-06T04:06:00Z, open=0.125350, high=0.125470, low=0.125330, close=0.125465, ticks=163}       
Candle{timestamp=2024-10-06T04:05:00Z, open=0.125460, high=0.125460, low=0.125235, close=0.125350, ticks=315}       
Candle{timestamp=2024-10-06T04:04:00Z, open=0.125460, high=0.125460, low=0.125460, close=0.125460, ticks=12}        
End of candles. Next candles printing in 15s.       
===================================

## Bonus task: Publishing and Polling from Kafka
Download Kafka, start `zookeper-server-start.sh` passing in zookeper.properties files -> then start another terminal window 
-> start `kafka-server-start.sh` passing in server.properties files. 

Using default port of localhost:9092, and topic is `1m_Candle`

Publisher:
Using the same scheduled task that runs every 15s in `KrakenClient.main` it will also publish a kafka message
similar to what I printed out. 

Consumer:  
Run `KafkaConsumerClass.main`, it should receive the kafka messages and print out in its own run tab.
The printed candles should be the same as the one in `KrakenClient` run tab, it should also print once every 15s.

## Other information
1. Did perform simple sanity checks such as:
    - Ensure that highest bid < lowest ask when building the tick level order book
        - When this happens, log it out instead of throwing an exception.
        - May occur once in awhile due to packet drops. 
2. There’s always at least one bid and ask present
    - This should only happens once, upon the very start of the program where the book
      is not built yet.
    - When this happens, log it out instead of throwing an exception. 
   
