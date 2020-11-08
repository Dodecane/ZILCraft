
# ZILCraft
Looking for the download link? [**Click here**](https://github.com/Dodecane/ZILCraft/releases/download/v0.0.9/ZILCraft-0.0.9.jar)

ZILCraft is a plugin which adds Zilliqa wallets to your Spigot Server.<br>
Players can create their own Zilliqa wallets and send transactions to each other within the game. They can even do so without needing to know the recipients address.<br>

## Demo Video
[Coming soon]

## How to install

Drag and drop the jar file into your servers  _/plugins_  directory. Then, restart your server.  _**Do not use /reload, as it can cause intense memory leaks.**_

After the restart, you should notice a new folder called  _/ZILCraft_  in your servers plugin directory.

## Configuring ZILCraft

This ZILCraft folder contains the files config.yml and zilcraft.data. 
The zilcraft.data file is used to store player information and can be backed up and transferred between servers.

config.yml

```
# Available network types are "mainnet" and "testnet", add custom tokens in the following format (all 3 fields are required)
network: <testnet | mainnet>
tokens:
	<name of token>:
		mainnet: <mainnet address>
		testnet: <testnet address>
		formatting: <minecraft format code surrounded by quotes> e.g. "&9"
```

## Commands and Permission Node List

 - /zilcraft help
	 - Shows help for ZILCraft commands
 - /zilcraft about
	 - Shows information about ZILCraft
 - /zilcraft reload 
	 - Permission node: `zilcraft.reload`(default op)
	 - Reloads ZILCraft config and user accounts
 - /zilcraft save 
	 - Permission node: `zilcraft.save`(default op)
	 - Manually saves ZILCraft config and user accounts
 - /zilcraft balance <account name | address> 
	 - Permission node: `zilcraft.balance`(default all)
	 - Retrieves token balances of account or address
 - /zilcraft network
	 - Permission node: `zilcraft.network`(default all)
	 - Shows information about connected network
 - 

## Screenshots
[WIP]
