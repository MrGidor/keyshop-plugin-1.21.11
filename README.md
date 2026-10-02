# KeyShop

A Paper 1.21 Minecraft plugin that lets players buy CrazyCrates keys from a simple in-game GUI shop using Vault.

## Features

- GUI-based key shop for players
- Supports selling custom key items for CrazyCrates
- Economy integration via Vault
- Admin commands to add, remove, and reload keys
- Flexible command-based delivery

## Requirements

- Paper / Spigot-compatible server (targeting 1.21)
- Java 21
- Vault
- CrazyCrates (optional, but recommended for key-based crate rewards)

## Installation

1. Build the plugin with Maven:
   ```bash
   mvn clean package
   ```

2. Copy the generated jar from `target/` to your server's `plugins/` folder.

3. Start the server and ensure Vault is installed.

4. If using CrazyCrates, install it as well.

## Commands

- `/keyshop` — Open the key shop
- `/keyshop addkey <key_name> <price> <slot>` — Save a key item to the shop
- `/keyshop removekey <key_name>` — Remove a key from the shop
- `/keyshop reload` — Reload configuration and key data

## Permissions

- `keyshop.use` — Allows opening and buying from the shop
- `keyshop.admin` — Allows managing keys and reloading the plugin

## Configuration

The plugin reads settings from `plugins/KeyShop/config.yml`.

Key options include:

- GUI title and rows
- Price lore formatting
- Delivery mode (`item` or `command`)
- Sound settings
- Custom success/error messages

## Example

After adding a key:

```text
/keyshop addkey rare_key 250 10
```

Players can then open `/keyshop` and purchase the configured item using the server economy.

## Notes

This plugin is designed for SMP servers that want to sell crate keys directly in-game as a shop system.

## License

This project is licensed under the GNUv3 License. See the `LICENSE` file for details.
