# Project Instructions: QuantCraft

## Tech Stack

- **Language**: Java 17
- **Minecraft Version**: 1.20.4+ (Fabric)
- **Build Tool**: Gradle + Fabric Loom
- **Framework**: Fabric Modding Framework
- **Key Dependencies**: Cloth Config, ModMenu

## Code Style

- **File Naming**: PascalCase matching the class name (e.g., `MarketEngine.java`)
- **Class/Interface Naming**: PascalCase (e.g., `StockState`, `LiquidityBot`)
- **Method/Field Naming**: camelCase (e.g., `getCurrentPrice()`, `tickCounter`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_NEWS`, `MOD_ID`)
- **Package Structure**: `com.quantcraft.{module}` (market, blockentity, registry, etc.)

## Testing

- No automated test suite currently configured
- Manual testing via `/gradlew runClient` and in-game commands
- Test plan should cover: market open/close, buy/sell transactions, dividend payouts, screen UI

## Build & Run

- **Dev Environment**: `./gradlew runClient` (launches Minecraft with mod)
- **Build JAR**: `./gradlew build` (outputs to `build/libs/`)
- **IDE Setup**: `./gradlew idea` (IntelliJ) or `./gradlew eclipse`
- **Rebuild Mappings**: `./gradlew genSources` (regenerate Fabric Yarn mappings)

## Project Structure

### Core Market System
- `market/MarketEngine.java` — singleton managing all market state
- `market/StockRegistry.java` — stock definitions (static registry)
- `market/StockState.java` — per-stock price tracking & history
- `market/LiquidityBot.java` — AI price stabilization
- `market/PlayerPortfolio.java` — player holdings & balance
- `persistence/MarketPersistentState.java` — NBT serialization to disk

### Block Entities & UI
- `blockentity/TradingPostBlockEntity.java` — Trading UI block server side
- `blockentity/QuotronBlockEntity.java` — Price display block
- `screen/TradingPostScreen.java` — Client-side trading UI
- `screen/CommodityExchangeScreen.java` — Commodity trading UI
- `screen/QuotronScreen.java` — Price display UI

### Commands & Events
- `command/PlayerCommands.java` — `/qc`, `/portfolio` commands
- `command/AdminCommands.java` — `/admin` commands
- `events/MarketEventListener.java` — listens to Minecraft events (harvest, kill, etc.)

### Registry & Network
- `registry/ModItems.java`, `ModBlocks.java` — item/block registration
- `registry/ModBlockEntityTypes.java`, `ModScreenHandlerTypes.java`
- `network/ModPackets.java` — server↔client packet definitions

## Conventions

### Code Patterns
- **Singleton Access**: `MarketEngine.getInstance()` for core market logic
- **NBT Serialization**: Classes implement `fromNbt(NbtCompound)` static factory and `toNbt()` instance method
- **Event Registration**: Use Fabric event API (e.g., `ServerTickEvents.END_SERVER_TICK.register()`)
- **Block Entities**: Extend `BlockEntity`, implement `readNbt()` and `writeNbt()`
- **Screen Handlers**: Extend `ScreenHandler`, implement button click handlers
- **Logging**: Use `QuantCraftMod.LOGGER` for debug/info, never System.out

### Error Handling
- Use Minecraft's `Text.literal()` for player-facing error messages
- Validate input at command/network boundary (check balances, quantities, stock existence)
- Return early with `Text.literal("§c ERROR message")` for validation failures

### Git Workflow
- Commit message format: "Add/Fix/Refactor [component] — [description]" (e.g., "Add short positions — margin call logic")
- Work on `main` directly (single developer)

## Critical Files

- **Initialization**: [QuantCraftMod.java](src/main/java/com/quantcraft/QuantCraftMod.java) — registers all items, blocks, commands, event listeners
- **Market Simulation**: [MarketEngine.java](src/main/java/com/quantcraft/market/MarketEngine.java) — price updates, dividends, short positions
- **Persistence**: [MarketPersistentState.java](src/main/java/com/quantcraft/persistence/MarketPersistentState.java) — all NBT save/load logic
- **Client Init**: [QuantCraftModClient.java](src/client/java/com/quantcraft/QuantCraftModClient.java) — screen registration
- **Config**: [QuantCraftConfig.java](src/main/java/com/quantcraft/config/QuantCraftConfig.java) — tunable parameters

## Known Issues

1. **Security Audit (2026-05-14)**: 22 vulnerabilities identified, including:
   - Account balance manipulation via reflected packets
   - Missing input validation on order quantities
   - Data deserialization vulnerabilities in NBT
   - Privilege escalation in admin commands
   - Recommend prioritizing balance validation & packet signing

2. **No Test Suite**: Manual testing only — consider adding unit tests for MarketEngine logic

3. **Market Tick Interval**: Tuned via `QuantCraftConfig.getMarketTickInterval()` — default ~1 second

## Tips for Development

- **Add a new stock**: Edit `StockRegistry.STOCK_DEFS` and define base price, sector, total shares
- **Change market hours**: [QuantCraftMod.java:56](src/main/java/com/quantcraft/QuantCraftMod.java#L56) — modify `tod < 13000L` threshold
- **Adjust price volatility**: [MarketEngine.java `tickSimulated()`](src/main/java/com/quantcraft/market/MarketEngine.java) — tune random walk parameters
- **Add a command**: Create handler in `PlayerCommands` or `AdminCommands`, register in `QuantCraftMod.onInitialize()`
- **Sync data to client**: Use `ModPackets.broadcastMarketUpdate()` or create new packet method in `ModPackets.java`
- **Add NBT persistence**: Extend `MarketPersistentState.fromNbt()` and `.writeNbt()` with new fields
