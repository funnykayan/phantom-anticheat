# Phantom AntiCheat (basic)

Simple Paper plugin example providing basic anti-cheat checks (speed & fly) and a `verbose` debug option.

Build (Maven):

```bash
mvn package
```

Install: drop the generated `target/phantom-anticheat-1.0.0.jar` into your server `plugins/` folder.

Commands:
- `/anticheat reload` — reload config
- `/anticheat verbose <on|off>` — toggle verbose logging (for debugging)
- `/anticheat status` — print current config values

Config options (src/main/resources/config.yml):
- `verbose` — enable extra logging and player messages
- `checks.speed` — enable basic horizontal speed checks
- `checks.fly` — basic flying detection
- `max-speed` — allowed horizontal distance per move event
