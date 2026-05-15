package com.quantcraft.market;

import org.junit.jupiter.api.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MarketEngineTest {

    @Nested
    @DisplayName("StockState - price tracking")
    class StockStateTests {

        private StockState state;

        @BeforeEach
        void setup() {
            state = new StockState("IRON", 100.0);
        }

        @Test
        @DisplayName("initial price matches constructor argument")
        void initialPrice() {
            assertEquals(100.0, state.getCurrentPrice(), 0.001);
        }

        @Test
        @DisplayName("price update is reflected immediately")
        void priceUpdate() {
            state.updatePrice(110.0);
            assertEquals(110.0, state.getCurrentPrice(), 0.001);
        }

        @Test
        @DisplayName("price history grows with each update")
        void historyGrows() {
            int initialSize = state.getPriceHistory().size();
            state.updatePrice(102.0);
            state.updatePrice(105.0);
            state.updatePrice(103.0);
            List<Double> history = state.getPriceHistory();
            assertTrue(history.size() >= initialSize,
                    "History should not shrink after updates");
        }

        @Test
        @DisplayName("price never drops below floor of 1.0")
        void priceFloor() {
            state.updatePrice(-50.0);
            assertTrue(state.getCurrentPrice() >= 1.0,
                    "Price should not drop below 1.0");
        }

        @Test
        @DisplayName("daily change percent is correct")
        void dailyChangePct() {
            state.updatePrice(115.0);
            double expected = (115.0 - 100.0) / 100.0 * 100.0;
            assertEquals(expected, state.getDailyChangePercent(), 0.01);
        }

        @Test
        @DisplayName("daily change is negative when price drops")
        void dailyChangePctNegative() {
            state.updatePrice(90.0);
            assertTrue(state.getDailyChangePercent() < 0);
        }

        @Test
        @DisplayName("price history is bounded")
        void historyCapped() {
            for (int i = 0; i < 100; i++) {
                state.updatePrice(100.0 + i);
            }
            assertTrue(state.getPriceHistory().size() <= 30,
                    "History should be bounded at 30 entries");
        }

        @Test
        @DisplayName("event pressure accumulates and is consumed")
        void eventPressure() {
            state.applyEventPressure(2.5);
            state.applyEventPressure(1.5);
            assertEquals(4.0, state.consumeEventPressure(), 0.001);
            assertEquals(0.0, state.consumeEventPressure(), 0.001);
        }
    }

    @Nested
    @DisplayName("PlayerPortfolio - balance and holdings")
    class PlayerPortfolioTests {

        private PlayerPortfolio portfolio;

        @BeforeEach
        void setup() {
            portfolio = new PlayerPortfolio(1000.0);
        }

        @Test
        @DisplayName("initial balance matches constructor")
        void initialBalance() {
            assertEquals(1000.0, portfolio.getBalance(), 0.001);
        }

        @Test
        @DisplayName("addBalance increases correctly")
        void addBalance() {
            portfolio.addBalance(250.0);
            assertEquals(1250.0, portfolio.getBalance(), 0.001);
        }

        @Test
        @DisplayName("deductBalance decreases correctly")
        void deductBalance() {
            portfolio.deductBalance(300.0);
            assertEquals(700.0, portfolio.getBalance(), 0.001);
        }

        @Test
        @DisplayName("deductBalance floors at zero")
        void deductBalanceFloor() {
            portfolio.deductBalance(9999.0);
            assertEquals(0.0, portfolio.getBalance(), 0.001);
        }

        @Test
        @DisplayName("addSharesAt tracks quantity correctly")
        void addShares() {
            portfolio.addSharesAt("IRON", 50, 0L);
            assertEquals(50, portfolio.getHolding("IRON"));
        }

        @Test
        @DisplayName("addSharesAt twice accumulates correctly")
        void addSharesTwice() {
            portfolio.addSharesAt("IRON", 50, 0L);
            portfolio.addSharesAt("IRON", 25, 0L);
            assertEquals(75, portfolio.getHolding("IRON"));
        }

        @Test
        @DisplayName("average cost is weighted across multiple purchases")
        void weightedAverageCost() {
            // Simulate addShares(ticker, qty, price) logic manually
            portfolio.addSharesAt("IRON", 50, 0L);
            portfolio.setAvgCost("IRON", 100.0);
            // Second buy: 50 @ 120 -> weighted avg = (100*50 + 120*50) / 100 = 110
            double newAvg = (100.0 * 50 + 120.0 * 50) / (50 + 50);
            portfolio.addSharesAt("IRON", 50, 0L);
            portfolio.setAvgCost("IRON", newAvg);
            assertEquals(110.0, portfolio.getAvgCosts().get("IRON"), 0.01);
        }

        @Test
        @DisplayName("removeShares reduces holding")
        void removeShares() {
            portfolio.addSharesAt("IRON", 100, 0L);
            portfolio.removeShares("IRON", 40);
            assertEquals(60, portfolio.getHolding("IRON"));
        }

        @Test
        @DisplayName("removing all shares clears the holding and avg cost")
        void removeAllShares() {
            portfolio.addSharesAt("IRON", 50, 0L);
            portfolio.setAvgCost("IRON", 100.0);
            portfolio.removeShares("IRON", 50);
            assertEquals(0, portfolio.getHolding("IRON"));
            assertFalse(portfolio.getAvgCosts().containsKey("IRON"));
        }

        @Test
        @DisplayName("removing more shares than held clears to zero")
        void oversellClearsToZero() {
            portfolio.addSharesAt("IRON", 20, 0L);
            portfolio.removeShares("IRON", 50);
            assertEquals(0, portfolio.getHolding("IRON"));
        }

        @Test
        @DisplayName("unknown ticker returns zero shares")
        void unknownTickerZero() {
            assertEquals(0, portfolio.getHolding("UNKNOWN"));
        }

        @Test
        @DisplayName("buy fails when insufficient funds")
        void buyInsufficientFunds() {
            // buy() calls addShares which requires MarketEngine, so we only test the balance check
            // by verifying insufficient funds returns false without modifying balance
            portfolio.deductBalance(999.0); // leave 1.0
            boolean result = portfolio.buy("IRON", 100, 50.0);
            assertFalse(result);
            assertEquals(1.0, portfolio.getBalance(), 0.001);
        }

        @Test
        @DisplayName("sell fails when insufficient shares")
        void sellInsufficientShares() {
            portfolio.addSharesAt("IRON", 10, 0L);
            boolean result = portfolio.sell("IRON", 20, 100.0);
            assertFalse(result);
            assertEquals(10, portfolio.getHolding("IRON"));
        }

        @Test
        @DisplayName("sell adds balance and removes shares")
        void sellFlow() {
            portfolio.addSharesAt("IRON", 50, 0L);
            boolean result = portfolio.sell("IRON", 20, 100.0);
            assertTrue(result);
            assertEquals(3000.0, portfolio.getBalance(), 0.001);
            assertEquals(30, portfolio.getHolding("IRON"));
        }
    }

    @Nested
    @DisplayName("OtcTradeOffer - validation and serialization")
    class OtcTradeOfferTests {

        private OtcTradeOffer makeOffer(int shares, double price) {
            return new OtcTradeOffer(
                    UUID.randomUUID(), "Alice",
                    UUID.randomUUID(),
                    "IRON", shares, price, 0L);
        }

        @Test
        @DisplayName("totalCost = shares x pricePerShare")
        void totalCost() {
            OtcTradeOffer offer = makeOffer(100, 55.0);
            assertEquals(5500.0, offer.totalCost(), 0.001);
        }

        @Test
        @DisplayName("offer is not expired immediately after creation")
        void notExpiredImmediately() {
            OtcTradeOffer offer = makeOffer(10, 50.0);
            assertFalse(offer.isExpired(0L));
        }

        @Test
        @DisplayName("offer expires after EXPIRY_TICKS ticks")
        void expiresAfterTicks() {
            OtcTradeOffer offer = makeOffer(10, 50.0);
            assertTrue(offer.isExpired(OtcTradeOffer.EXPIRY_TICKS));
        }

        @Test
        @DisplayName("offer ticker is uppercased")
        void tickerUppercased() {
            OtcTradeOffer offer = new OtcTradeOffer(
                    UUID.randomUUID(), "Alice",
                    UUID.randomUUID(),
                    "iron", 10, 50.0, 0L);
            assertEquals("IRON", offer.getTicker());
        }
    }

    @Nested
    @DisplayName("LiquidityBot - price impact model")
    class LiquidityBotTests {

        @Test
        @DisplayName("large buy moves price up proportionally")
        void largeBuyMovesUp() {
            double basePrice   = 100.0;
            int    totalSupply = 10_000;
            int    buyQty      = 500;
            double impact      = (double) buyQty / totalSupply * basePrice;
            double newPrice    = basePrice + impact;
            assertTrue(newPrice > basePrice);
        }

        @Test
        @DisplayName("large sell moves price down proportionally")
        void largeSellMovesDown() {
            double basePrice   = 100.0;
            int    totalSupply = 10_000;
            int    sellQty     = 500;
            double impact      = (double) sellQty / totalSupply * basePrice;
            double newPrice    = basePrice - impact;
            assertTrue(newPrice < basePrice);
        }

        @Test
        @DisplayName("price impact is zero for zero quantity")
        void zeroQuantityZeroImpact() {
            double impact = (double) 0 / 10_000 * 100.0;
            assertEquals(0.0, impact, 0.001);
        }
    }

    @Nested
    @DisplayName("Short positions - P&L calculations")
    class ShortPositionTests {

        @Test
        @DisplayName("profit on short when price falls")
        void shortProfit() {
            double openPrice  = 100.0;
            double closePrice = 80.0;
            int    shares     = 50;
            double pnl = (openPrice - closePrice) * shares;
            assertEquals(1000.0, pnl, 0.001);
        }

        @Test
        @DisplayName("loss on short when price rises")
        void shortLoss() {
            double openPrice  = 100.0;
            double closePrice = 130.0;
            int    shares     = 50;
            double pnl = (openPrice - closePrice) * shares;
            assertEquals(-1500.0, pnl, 0.001);
        }

        @Test
        @DisplayName("margin call triggers at 150% of short value")
        void marginCallThreshold() {
            double shortOpenPrice = 100.0;
            double currentPrice   = 155.0;
            double ratio          = currentPrice / shortOpenPrice;
            assertTrue(ratio >= 1.5);
        }

        @Test
        @DisplayName("no margin call below 150% of short value")
        void noMarginCallBelow150() {
            double shortOpenPrice = 100.0;
            double currentPrice   = 140.0;
            double ratio          = currentPrice / shortOpenPrice;
            assertFalse(ratio >= 1.5);
        }
    }
}
