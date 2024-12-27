[![Status Badge](https://github.com/ilotterytea/bot/actions/workflows/build.yml/badge.svg)](https://github.com/ilotterytea/bot/actions/workflows/build.yml)

# Huinyabot

A utility and entertainment multi-chat Twitch bot. The bot is built in Java, Gradle and uses Twitch4j as the Twitch API.
This project is for me to learn more about Java and all its tricks.

## Prerequisites

+ PostgreSQL 15
+ JDK 11

## Building from sources

### 1. Cloning the repo

```shell
git clone https://github.com/ilotterytea/bot.git -b 1.5.0
cd bot
```

### 2. Build the source

```shell
./gradlew shadowJar
cd build/libs
```

### 3. Create a configuration file (config.properties)

Replace the fields `DATABASE_NAME`, `USERNAME`, `PASSWORD`.

```properties
twitch.oauth2_token=oauth:your_token_here
twitch.access_token=access_token_from_your_twitch_application
hibernate.connection.url=jdbc:postgresql://localhost:5432/DATABASE_NAME
hibernate.connection.username=DB_USERNAME
hibernate.connection.password=DB_PASSWORD
``` 

> The `twitch.access_token` is the bot's token received when logging into the application from the bot's account.
> Token must grant rights `moderator:read:chatters`, otherwise will not work `!massping`, `MASSPING` flag for stream
> events.
> Also, the bot must be a moderator on channels that run massping related commands.

### 4. Run the bot

```shell
java -jar bot-1.5.0-all.jar
```