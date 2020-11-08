

# ZILCraft
Looking for the download link? [**Click here**](https://github.com/Dodecane/ZILCraft/releases/download/v0.0.10/ZILCraft-0.0.10.jar) Supported Minecraft versions: 1.16.3+

ZILCraft is a plugin which adds Zilliqa wallets to your Spigot Server.<br>
Players can create their own Zilliqa wallets and send tokens to each other within the game. They can even do so without needing to know the recipient's address.<br>

## See it in action
Demo Video
[![Watch the video](https://img.youtube.com/vi/jA_NHfMYGJo/maxresdefault.jpg)](https://youtu.be/jA_NHfMYGJo)

Demo server [Coming soon]

## How to install

Drag and drop the jar file into your server's  _/plugins_  directory. Then, restart your server.  _**Do not use /reload, as it can cause intense memory leaks.**_

After the restart, you should notice a new folder called  _/ZILCraft_  in your server's plugin directory.

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
 - /zilcraft network
	 - Permission node: `zilcraft.network`(default all)
	 - Shows information about connected network
 - /zilcraft network set <mainnet | testnet>
	 - Permission node: `zilcraft.network.set`(default op)
	 - Changes network
 - /zilcraft unlock < account name> 
	 - Permission node: `zilcraft.unlock`(default all)
	 - Unlocks account
 - /zilcraft lock < account name>
	 - Permission node: `zilcraft.lock`(default all)
	 - Locks account
 - /zilcraft account create
	 - Permission node: `zilcraft.account.create`(default all)
	 - Creates new account
 - /zilcraft account import privatekey
	 - Permission node: `zilcraft.account.import`(default all)
	 - Imports existing account
 - /zilcraft account remove <account name | all>
	 - Permission node: `zilcraft.account.remove`(default all)
	 - Removes account(s)
 - /zilcraft account set < account name>
	 - Permission node: `zilcraft.account.set`(default all)
	 - Sets active account
 - /zilcraft account rename < account name> < new account name>
	 - Permission node: `zilcraft.account.rename`(default all)
	 - Renames account
 - /zilcraft account list
	 - Permission node: `zilcraft.account.list`(default all)
	 - Lists accounts
 - /zilcraft send <username | address>
	 - Permission node: `zilcraft.send`(default all)
	 - Sends tokens to a player or address
 - /zilcraft token add <mainnet | testnet> < contract address>
	 - Permission node: `zilcraft.token.add`(default all)
	 - Adds token by contract address
 - /zilcraft token remove <mainnet | testnet> <token name(s)>
	 - Permission node: `zilcraft.token.remove`(default all)
	 - Removes added token(s)
 - /zilcraft token list
	 - Permission node: `zilcraft.token.list`(default all)
	 - Lists added tokens

## Screenshots
- Sending tokens

![Sending tokens](https://i.imgur.com/eUER1oA.png)

- Account unlocking

![Account unlocking](https://i.imgur.com/gl6mhYr.png)

- Account creation

![Account creation](https://i.imgur.com/dNfAh4H.png)

- Account removal

![Account removal](https://i.imgur.com/8Ku38tP.png)

- Account import

![Account import](https://i.imgur.com/8GRA6S3.png)
