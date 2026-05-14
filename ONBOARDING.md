# Onboarding Guide: QuantCraft

## Overview

QuantCraft is a Minecraft mod that adds a fully-featured stock market simulation to the game. Players can trade stocks through Trading Posts and Commodity Exchanges, track prices on Quotrons (display blocks), and read market news via Newspapers. The market system includes real-time price simulation, liquidity bots, dividend payouts, and world structures where trading occurs.

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 17 |
| Minecraft Version | 1.20.4+ |
| Modding Framework | Fabric | - |
| Build Tool | Gradle | - |
| Config Library | Cloth Config | - |
| Menu Integration | ModMenu | 9.0.0+ |

## Architecture

The mod follows a layered architecture:

```
┌─────────────────────────────────────────────────────────┐
│  Client Layer (UI & Networking)                         │
│  ├─ Screens (TradingPostScreen, CommodityExchangeScreen) │
│  ├─ Screen Handlers (TradingPostScreenHandler, etc.)     │
│  └─ Client Packets & Sync (ModPacketsClient)            │
├─────────────────────────────────────────────────────────┤
│  Server Layer (Core Game Logic)                         │
│  ├─ Market Engine (price simulation & events)           │
│  ├─ Stock Registry (stock definitions & metadata)       │
│  ├─ Liquidity Bot (AI price stabilization)              │
│  └─ Commands (admin & player market access)             │
├─────────────────────────────────────────────────────────┤
│  Persistence Layer                                      │
│  ├─ MarketPersistentState (NBT serialization)           │
│  └─ Player Portfolios (holdings & balances)             │
├─────────────────────────────────────────────────────────┤
│  Registry & Block Entities                              │
│  ├─ ModItems, ModBlocks (item/block registration)       │
│  ├─ BlockEntity Types (Quotron, TradingPost, etc.)      │
│  └─ Screen Handlers (UI handlers for blocks)            │
└─────────────────────────────────────────────────────────┘
```

## Key Entry Points

- **Main Mod Class**: [QuantCraftMod.java](src/main/java/com/quantcraft/QuantCraftMod.java) — initializes registries, commands, and server tick handlers
- **Market Simulation**: [MarketEngine.java](src/main/java/com/quantcraft/market/MarketEngine.java) — core price simulation, dividend payouts, short positions
- **Market Persistence**: [MarketPersistentState.java](src/main/java/com/quantcraft/persistence/MarketPersistentState.java) — saves/loads market state to NBT
- **Client UI**: [QuantCraftModClient.java](src/client/java/com/quantcraft/QuantCraftModClient.java) — client-side initialization, screen registration
- **Blocks & Items**: [ModBlocks.java](src/main/java/com/quantcraft/registry/ModBlocks.java), [ModItems.java](src/main/java/com/quantcraft/registry/ModItems.java)
- **Commands**: [PlayerCommands.java](src/main/java/com/quantcraft/command/PlayerCommands.java), [AdminCommands.java](src/main/java/com/quantcraft/command/AdminCommands.java)

## Directory Map

```
src/main/java/com/quantcraft/
├── QuantCraftMod.java              ← Main mod entry point
├── market/                          ← Market simulation engine
│   ├── MarketEngine.java            ← Core market logic
│   ├── StockRegistry.java           ← Stock definitions
│   ├── StockState.java              ← Per-stock price state
│   ├── LiquidityBot.java            ← AI price stabilizer
│   ├── PlayerPortfolio.java         ← Player holdings & balance
│   ├── CommodityMarket.java         ← Commodity market subset
│   ├── OrderBook.java               ← Limit order management
│   └── ActiveMarketEvent.java       ← Market-moving events (NEW)
├── blockentity/                     ← Block entities (servers side)
│   ├── TradingPostBlockEntity.java  ← Trading UI block
│   ├── QuotronBlockEntity.java      ← Price display block
│   └── CommodityExchangeBlockEntity.java
├── persistence/                     ← NBT serialization
│   └── MarketPersistentState.java   ← World state save/load
├── registry/                        ← Minecraft registry
│   ├── ModItems.java                ← Item registration
│   ├── ModBlocks.java               ← Block registration
│   ├── ModBlockEntityTypes.java     ← Block entity type registry
│   ├── ModScreenHandlerTypes.java   ← Screen handler registry
│   ├── ModItemGroups.java           ← Item groups (NEW)
│   └── ModStructures.java           ← Structure registry
├── command/                         ← Player & admin commands
│   ├── PlayerCommands.java          ← /qc, /portfolio commands
│   └── AdminCommands.java           ← /admin commands
├── events/                          ← Minecraft event listeners
│   └── MarketEventListener.java     ← Reacts to world events (crop harvest, kill, etc.)
├── network/                         ← Packet definitions
│   └── ModPackets.java              ← Server↔Client sync packets
├── config/                          ← Configuration
│   └── QuantCraftConfig.java        ← Config values & defaults
├── block/                           ← Block implementations
│   ├── TradingPostBlock.java
│   ├── QuotronBlock.java
│   └── CommodityExchangeBlock.java
├── item/                            ← Item implementations
│   ├── NewspaperItem.java           ← Market news delivery
│   ├── DollarBillItem.java          ← Currency item
│   └── NewspaperEffects.java        ← News event handlers (NEW)
└── structure/                       ← World generation
    ├── InvestmentCenterGenerator.java ← Generates trading hubs
    └── InvestmentCenterPiece.java   ← Structure piece

src/client/java/com/quantcraft/
├── QuantCraftModClient.java         ← Client initialization
├── screen/                          ← Client-side screens
│   ├── TradingPostScreen.java       ← Trading UI
│   ├── CommodityExchangeScreen.java ← Commodity UI
│   ├── QuotronScreen.java           ← Price display UI
│   └── NewspaperScreen.java         ← News reader UI (NEW)
├── network/                         ← Client packet handlers
│   └── ModPacketsClient.java
└── config/                          ← Client config
    └── QuantCraftConfigScreen.java  ← Config GUI
```

## Request Lifecycle: Buy/Sell Transaction

1. **Player Action** → Client screen sends `/qc buy TICKER QTY` command or uses screen button
2. **Command Processing** → Server receives command in `PlayerCommands.handleBuy()`
3. **Validation** → Check player balance, stock availability, liquidity bot reserve
4. **Transaction Execution**:
   - Deduct coins from player portfolio
   - Add shares to player holdings
   - Update liquidity bot reserve
   - Record acquisition tick for dividend tracking
5. **State Update** → `MarketPersistentState.saveStates()` persists to NBT
6. **Client Sync** → `ModPackets.broadcastMarketUpdate()` sends snapshot to all clients
7. **UI Update** → Client screens refresh with new balance/holdings

## Tick Flow (Server Side)

Every `QuantCraftConfig.getMarketTickInterval()` ticks (~1 second):

1. `MarketEngine.tick()` runs
2. `tickSimulated()` — applies price movements based on active events
3. `processShortPositions()` — handles short position interest & margin calls
4. `payDividendsIfDue()` — distributes earnings if timer expired
5. `MarketPersistentState.saveStates()` — persists to NBT
6. `ModPackets.broadcastMarketUpdate()` — syncs to all clients

Market opens/closes once per day (in-game time):
- Dawn (tick 0-13000): market open, broadcast top gainers/losers
- Dusk (tick 13000+): snapshot closing prices, market close

## Conventions

**Naming**:
- Classes: PascalCase (e.g., `MarketEngine`, `PlayerPortfolio`)
- Methods: camelCase (e.g., `getCurrentPrice()`, `addShares()`)
- Constants: UPPER_SNAKE_CASE (e.g., `MAX_NEWS`, `MOD_ID`)
- File names: PascalCase matching class name

**Code Patterns**:
- **Singleton instances**: `MarketEngine.getInstance()`, `StockRegistry` (static methods)
- **NBT Serialization**: `fromNbt()` static factory + `toNbt()` instance method
- **Minecraft Events**: Register via Fabric's event API (e.g., `ServerTickEvents.END_SERVER_TICK.register()`)
- **Screen Handlers**: Extend `ScreenHandler`, implement `onButtonClick()`
- **Block Entities**: Extend `BlockEntity`, implement `readNbt()` and `writeNbt()`
- **Error handling**: Use Minecraft's `Text.literal()` for user-facing messages, `LOGGER` for debug

**Git Workflow**:
- Commit message style: "Add/Fix/Refactor [component] — [brief description]"
- Branch strategy: Work directly on `main` (simple project)
- PR not required (single developer)

## Common Tasks

| Task | Command |
|------|---------|
| Build mod JAR | `./gradlew build` |
| Run dev server | `./gradlew runServer` |
| Run client | `./gradlew runClient` |
| Refresh IDE | `./gradlew idea` (IntelliJ) / `./gradlew eclipse` (Eclipse) |
| Format code | Use IDE's auto-formatter (target Java 17) |
| Add a new stock | Edit `StockRegistry`, add to `STOCK_DEFS` map |
| Add a command | Create in `PlayerCommands` or `AdminCommands`, register in `QuantCraftMod` |
| Add a block | Create block class, add to `ModBlocks.register()`, create models in `src/main/resources/assets/` |

## Where to Look

| Goal | Location |
|------|----------|
| Change market open/close times | [QuantCraftMod.java:56-67](src/main/java/com/quantcraft/QuantCraftMod.java#L56-L67) (time-of-day logic) |
| Adjust price volatility | [MarketEngine.java](src/main/java/com/quantcraft/market/MarketEngine.java) (`tickSimulated()` method) |
| Add dividend logic | [MarketEngine.java](src/main/java/com/quantcraft/market/MarketEngine.java) (`payDividendsIfDue()`) |
| Add a new item/block | Create in `item/` or `block/`, register in `ModItems`/`ModBlocks` |
| Add a new command | `PlayerCommands.java` or `AdminCommands.java` |
| Change player UI | `src/client/java/com/quantcraft/screen/` |
| Add world structure | `structure/InvestmentCenterGenerator.java` |
| Modify persistence | `MarketPersistentState.java` (NBT serialization) |
| Send packets to client | `ModPackets.broadcastMarketUpdate()` or add new packet methods |

## Recent Security Audit

**Status**: 22 vulnerabilities identified (2026-05-14). High-priority issues include:
- Account balance manipulation via reflected packets
- Missing input validation on order quantities
- Data deserialization vulnerabilities in NBT
- Privilege escalation in admin commands

See Observation #3 for full audit report. Fixes should prioritize deserialization & balance validation.

## Next Steps

1. Read [QuantCraftMod.java](src/main/java/com/quantcraft/QuantCraftMod.java) to understand initialization
2. Trace a market tick in [MarketEngine.java](src/main/java/com/quantcraft/market/MarketEngine.java)
3. Explore a player screen in `src/client/java/com/quantcraft/screen/`
4. Run `/gradlew runClient` to test the mod locally
