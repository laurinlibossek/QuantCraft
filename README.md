# QuantCraft

A minecraft mod that runs a live stock market driven entirely by in-game player behavior and world events.

## How the market works

Prices update on a configurable server tick interval (default: every 60 seconds during market hours). Each tick, the engine applies several forces to every stock:

- **Supply pressure** — player commodity deposits push prices down. Buy orders push up, sell orders push down. The effect scales asymmetrically: sells hit harder than buys (`1.7×` vs `0.5×` multiplier), and pressure scales by total float so rare stocks crash harder than farmable ones.
- **Mean reversion** — each stock has a configured reversion coefficient that pulls its price back toward its base value over time.
- **Volatility** — each stock has an independent volatility parameter. Arcane stocks (blaze rods, ghast tears) swing much harder than agrarian ones (wheat, potato).
- **Market season** — the economy cycles through Recovery → Expansion → Peak → Contraction, each applying a global drift coefficient and volatility multiplier that affects all stocks.
- **Event pressure** — world events (boss kills, raids, weather, player actions) inject short-lived `ActiveMarketEvent` instances that add per-tick pressure to specific sectors for a fixed duration.

The market is open between dawn and dusk. No trading outside those hours.

Prices are floored at `1.0¢` and use a configurable per-stock absolute floor. Candle history is kept for the Quotron HUD charts.

## Liquidity bot

Each stock has a `LiquidityBot` that acts as a passive market maker. It holds a cash reserve and a share reserve, and places resting orders on both sides of the book. Without it, a stock with no player activity would just sit flat. With it, prices drift and mean-revert naturally even in singleplayer.

Bot parameters (order size, cash reserves) scale by stock float and are adjustable per-ticker via `/qcadmin bot`.

## Persistence

All market state — prices, candle history, player portfolios, limit orders, short positions, bot state, active events, current season — is stored in a `PersistentState` on the server's overworld. Survives restarts.

## Limit orders and short selling

Limit orders escrow the tax at placement time to prevent evasion on fill. Unfilled portions get the escrowed tax refunded on cancellation.

Short selling requires 100% margin upfront. There's no free leverage from immediate proceeds. Short size is capped at 25% of the bot's liquidity per position to prevent market manipulation.

## Mixins

- `CocaineCrashTracker` — tick-based polling replaces the old `onStatusEffectRemoved` mixin to avoid `ConcurrentModificationException` when crash effects expire.
- `MilkMixin` — snapshots active crash effects before milk clears them, then reapplies them after. Cocaine crashes are permanent until they wear off naturally.

## Stocks

27 stocks across 6 sectors. Each stock has its own: base price, volatility, mean reversion coefficient, total float, price floor, and supply pressure factor.

| Sector | Tickers |
|--------|---------|
| Agrarian | WHEAT, CRRT, POTAT, APPLE, MELON |
| Mining | COAL, IRON, GOLD, DIAM, EMER, LAPIS, RDST, QRTZ |
| Lumber | OAKW, BIRC, SPRCE |
| Arcane | EPRL, BLAZ, GLOW, GHST |
| Livestock | LEAT, WOOL, FTHR, SKEL |
| Manufactured | GLASS, BRICK, PAPER |

Dividend rates are calibrated per sector (~2–5% APY annualized) and paid every 3 in-game days. Dividends are multiplied by the current season's dividend coefficient.

## Commands

**Player** (`/qc`):

| Command | Description |
|---------|-------------|
| `/qc balance` | Cash balance |
| `/qc portfolio` | Holdings with avg cost and P&L |
| `/qc pnl` | Quick P&L summary |
| `/qc prices` | All current prices |
| `/qc price <ticker>` | Single stock price |
| `/qc buy <ticker> <amount>` | Market buy |
| `/qc sell <ticker> <amount>` | Market sell |
| `/qc limitbuy <ticker> <amount> <price>` | Limit buy order |
| `/qc limitsell <ticker> <amount> <price>` | Limit sell order |
| `/qc orders` | Open limit orders |
| `/qc cancelorder <ticker>` | Cancel order |
| `/qc short <ticker> <amount>` | Open short |
| `/qc covershort <ticker>` | Close short |
| `/qc shorts` | Open short positions |
| `/qc pay <player> <amount>` | Send cash |
| `/qc request <player> <amount>` | Request cash (expires after 60s) |
| `/qc offer <player> <ticker> <amount> <price>` | OTC share deal |
| `/qc float <ticker>` | Float and share info |
| `/qc news` | Latest market events |

**HUD** (client-side):

| Command | Description |
|---------|-------------|
| `/pin <ticker>` | Pin ticker to HUD overlay (max 3) |
| `/unpin <ticker>` | Remove from HUD |

**Admin** (`/qcadmin`, requires OP level 2):

| Command | Description |
|---------|-------------|
| `/qcadmin crash/boom <ticker>` | Force price crash or spike (±200 pressure) |
| `/qcadmin setprice <ticker> <price>` | Override price directly |
| `/qcadmin setbalance/give/take` | Manage player funds and shares |
| `/qcadmin freeze / unfreeze` | Halt all trading |
| `/qcadmin multiplier <value>` | Global volatility multiplier (0.1–10) |
| `/qcadmin event <event>` | Inject a market event by name |
| `/qcadmin season set <phase>` | Force a market season |
| `/qcadmin reset market / portfolios / all` | Wipe state |
| `/qcadmin info <ticker>` | Raw stock internals (price, pressure, bot state) |
| `/qcadmin bot <ticker> enable/disable/info/cash` | Manage liquidity bot |
| `/qcadmin listplayers` | All players with portfolios |
| `/qcadmin cancelorders <player>` | Cancel a player's open orders |

## Configuration

Cloth Config / ModMenu. Configurable fields:

- `taxRate` — transaction tax (default `0.02`)
- `marketTickInterval` — ticks between price updates
- `volatilityMultiplier` — global multiplier applied to all stock volatility
- `cocaineCraftingEnabled` — toggle cocaine recipe via resource condition
