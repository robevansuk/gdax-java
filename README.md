# gdax-java

[![Join the chat at https://gitter.im/irufus/gdax-java](https://badges.gitter.im/irufus/gdax-java.svg)](https://gitter.im/irufus/gdax-java?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Java based wrapper for the [GDAX API](https://docs.gdax.com/#introduction) that follows the development style similar to [coinbase-java](https://github.com/coinbase/coinbase-java)

# Notes:

> GDAX primary data sources and servers run in the Amazon US East data center. To minimize latency for API access, we recommend making requests from servers located near the US East data center.
> Some of the methods do not yet have tests and so may not work as expected until a later date. Please raise an issue in github if you want something in particular as a priority.
> This codebase is maintained independently of Coinbase. We are not in any way affiliated with coinbase or gdax.

# Functions supported:
- [x] Authentication (GET, POST, DELETE supported)
- [x] Get Account
- [x] Get Accounts
- [x] Get Account History
- [x] Get Holds
- [x] Place a new Order (limit order)
- [x] Get an Order
- [x] Cancel an Order
- [x] List all open Orders
- [x] Get Market Data
- [x] List fills
- [x] List Products
- [x] HTTP Error code support
- [x] List of Currencies - from Accounts
- [x] Withdrawals - from coinbase accounts / payment methods / crypto account address
- [x] Deposits - from coinbase accounts / payment methods
- [x] Transfers - from coinbase accounts
- [x] Payment methods - coinbase / payment methods
- [x] Reports
- [x] Pagination support for all calls that support it.
- [x] Pagination support for all calls that support it.
- [x] Sandbox support - *sandbox support was dropped by gdax so this is now redundant*
- [x] LiveOrderBook implementation
    
# In Development

Desktop client GUI.
Check the issues on the repo for open items to work on.
Please join the gitter channel if you have any questions. Support always welcome.

# Contributing

Please see CONTRIBUTE.md if your interested in getting involved.

# Usage
--------

To build and run the application you can use the gradle script - this requires no installation as the "gradle wrapper" is included as part of the source code. All you need to do is:

1. supply your API key, secret and passphrase as environment or command line variables. NEVER commit these details to your repo, as you may lose any funds from your account(s). Spring Boot is smart enough to pick up the values for these variables from various places including the application.yml properties file, the system environment, command line variables and more.
1. 1. For environment variables set: `gdax.key`, `gdax.passphrase`, `gdax.secret`
1. 1. For command line variables `-Dgdax.key="apiKey" -Dgdax.passphrase="passphrase" -Dgdax.secret="secret"` should work
1. 1. For command line variables with the gradle command use `-Pgdax.key="apiKey" -Pgdax.passphrase="passphrase" -Pgdax.secret="secret"` should work
1. open a command line terminal
1. navigate to the root directory of this project (where `build.gradle` is)
1. execute `./gradlew bootRun` (Mac/unix). For equivalent Windows commands just remove the `./` from the commands, since there's a gradlew.bat included as well.

This won't actually do much on its own but the beginnings of a GUI have been developed and you can test this out by enabling the GUI in the application.yml config and restarting the application.

1. tests can also be run with  `./gradlew test` - simple.

For a lib:

1. If you'd rather work purely in java then you can build an executable jar file `./gradlew jar` and you should be able to find the jar in the build directory.

To run the gdax-java codebase from a .jar you'll need to pass all config via directives. The following has been tested and works:

`java -jar -Dgdax.key="yourKey" -Dgdax.secret="youSecret" -Dgdax.passphrase="yourPassphrase" -Dgdax.api.baseUrl="https://api.gdax.com/" -Dgui.enabled=true -Dliveorderbook.defaultProduct="BTC-GBP" -Dliveorderbook.timeout=15 -Dwebsocket.baseUrl="wss://we-feed.gdax.com/" -Dwebsocket.enabled=true build/gdax-java-{VERSION}.jar`

If the config changes from the above you should see a relevant error message in the output informing you.

The other alternative is to include all config in the application.yml, build the jar and export it somewhere.

# Examples

To make use of this library you only need a reference to the Service that you want. For Accounts, get an instance of the AccountService. For MarketData, use the MarketDataService, and so on.

In order to get an instance of the various services from the Spring Dependency Injector, you simply need to create a new component class, and then in your constructor add the Autowired annotation, then declare in the constructor signature the various services you want to have references to within your code, use variable setting then to store the references Autowiring will provide so you can use them in your class:

```
@Component
public class MyClassThatDoesSomethingReallyUseful{

  private LiveOrderBook liveOrderBook;

  @Autowired
  public MyClassThatDoesSomethingReallyUseful(LiveOrderBook liveOrderBook){
    this.LiveOrderBook = liveOrderBook;
    OrderBookModel bids = liveOrderBook.getBids();
    OrderBookModel asks = liveOrderBook.getAsks();

    String highestBidPrice = (String) bids.getValueAt(0, PRICE_COL);
    String lowestAskPrice = (String) asks.getValueAt(0, PRICE_COL);

    // do something useful
  }
}
```

The two annotations in the code above are pretty straight forward.
@Component - tells Spring to create an instance of this class and store it in the 'SpringContext'. This means we can 'wire' it into something else later.
@Autowired - tells Spring to look in its 'SpringContext' for an instance of the object that needs to be autowired in. In the above example, we're asking spring to AutoWire in an AccountService object so we can use it. If you look at the AccountService you'll notice it is also annotated with the @Component annotation, so its readily available where ever we want it within the codebase.

The API and this code follows MVC design pattern - model, view, control.
- Models are the data objects received from the API,
- Views are the items you'll likely create on top of this codebase - e.g. Swing GUI or some webpage output
- Control - all application logic goes in the control layer.

#Notes:

> GDAX primary data sources and servers run in the Amazon US East data center. To minimize latency for API access, we recommend making requests from servers located near the US East data center.

> Some of the methods do not yet have tests and so may not work as expected until a later date. Please raise an issue in github if you want something in particular as a priority. I'll be looking to fully flesh this out if possible over the coming months.

#Examples
--------

To make use of this library you only need a reference to the service that you want.

At present the classes match the interface specified in the coinbase/gdax api here: https://docs.gdax.com/#api

e.g. 
`public OrderService orderService(){
    new OrderService();
}`

This works better if you declare the above method as a spring object (@Component) and then plug it in to your classes using dependency injection.

Then in your method you can carry out any of the public API operations such as `orderService().createOrder(NewSingleOrder order);` - this creates a limit order. Currently this is only the basic order.

# API
--------

The Api for this application/library is as follows:
(Note: this section is likely to change but is provided on the basis it will work well for example usage)

- `AccountService.getAccounts()` - returns a List Accounts
- `AccountService.getAccountHistory(String accountId)` - returns the history for a given account as a List
- `AccountService.getHolds(String accountId)` - returns a List of all held funds for a given account.
- `DepositService.depositViaPaymentMethod(BigDecimal amount, String currency, String paymentMethodId)` - makes a deposit from a stored payment method into your GDAX account
- `DepositService.coinbaseDeposit(BigDecimal amount, String currency, String coinbaseAccountId)` - makes a deposit from a coinbase account into your GDAX account
- `MarketDataService.getMarketDataOrderBook(String productId, String level)` - a call to ProductService.getProducts() will return the order book for a given product. You can then use the WebsocketFeed api to keep your orderbook up to date. This is implemented in this codebase. Level can be 1 (top bid/ask only), 2 (top 50 bids/asks only), 3 (entire order book - takes a while to pull the data.)
- `OrderService.getOpenOrders(String accountId)` - returns a List of Orders for any outstanding orders
- `OrderService.cancelOrder(String orderId)` - cancels a given order
- `OrderService.createOrder(NewOrderSingle aSingleOrder)` - construct an order and send it to this method to place an order for a given product on the exchange.
- `PaymentService.getCoinbaseAccounts()` - gets the coinbase accounts for the logged in user
- `PaymentService.getPaymentTypes()` - gets the payment types available for the logged in user
- `ProductService.getProducts()` - returns a List of Products available from the exchange - BTC-USD, BTC-EUR, BTC-GBP, etc.
- `ReportService.createReport(String product, String startDate, String endDate)` - not certain about this one as I've not tried it but presumably generates a report of a given product's trade history for the dates supplied


# WebsocketFeed API 
---------------------

The WebsocketFeed is implemented and works. To use the WSF check out the API documentation and look at websocketFeed.subscribe(String, LiveOrderBook) method implementation as an example that already works.

# Updates - v 0.9.1
-------------------
- building an order book that works ready for a desktop client.

# Updates
---------
- converted to using Gradle
- converted to using SpringBoot for DI and request building
- updated all libraries used - removed some unnecessary libraries
- refactored the code to remove error handling from every method (rightly/wrongly) - its easier to maintain and extend now as a result
- more modular code that matches the service api - favour composition over inheritance
- removed a lot of boilerplate code
- logging added - Logging will output an equivalent curl command now for each get/post/delete request so that when debugging you can copy the curl request and execute it on the command line.
- service tests added for sanity - no unit tests against the data objects
- better configuration options using `application.yml` for your live environment and `application-test.yml` for your sandbox environment.
- banner displayed (specific to each environment) :)
- generally more structure.
- added pagination to all the relevant calls (some not supported since it seems pointless due to the limited offering from gdax - e.g. products)
- GDAX is updating its API without updating documentation - I've fixed an issue with market data because of this.
- WebsocketFeed added
- OrderBook GUI component added - enable in the `application.yml` by setting enabled to `true`
- LiveOrderBook (full channel) Implemented and viewable via the GUI when enabled

# TODO
-------
- add pagination versions of all endpoints, or offer a way to append to the endpoint urls.
- smarted up the GUI


From the GDAX API documentation:
  Send a subscribe message for the product(s) of interest and the full channel.
  Queue any messages received over the websocket stream.
  Make a REST request for the order book snapshot from the REST feed.
  Playback queued messages, discarding sequence numbers before or equal to the snapshot sequence number.
  Apply playback messages to the snapshot as needed (see below).
  After playback is complete, apply real-time stream messages in sequential order, queuing any that arrive out of order for later processing.
  Discard messages once they've been processed.

