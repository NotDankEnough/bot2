# Settings

> This command is for broadcaster only.


The `!set` command gives broadcasters ability to customize the bot as they need it to be more fitted for chat.

## Features

+ `notify_7tv_events` - Notify about 7TV updates on the channel.
+ `notify_betterttv_events` - Notify about BetterTTV updates on the channel.
+ `silent_mode` - Makes it so that a bot can no longer talk in chat
+ `markov_responses` - The bot will send a Markov-generated response if you mention it.
+ `random_markov_responses` - The bot will send a Markov-generated response to a random message (also,
  `markov_responses` must be enabled for this)

## Syntax

### Set the bot localization for the chat

`!set locale [lang]`

+ `[lang]` - Language name in English and lowercase.
  Available languages at the moment: **english**, **russian**.

### Set the bot prefix

`!set prefix [characters]`

+ `[characters]` - Characters to be set as a prefix.

### Enable/disable the bot features

`!set feature [feature]`

+ `[feature]` - Feature.

## Usage

### Setting the bot localization

+ `!set locale russian`
+ `!set locale english`

### Setting the bot prefix

+ `!set prefix ~`
+ `!set prefix ?!`

### Enabling/disabling the bot features

+ `!set feature notify_7tv_events`
+ `!set feature notify_bttv_events`

## Responses

### Setting the bot localization

+ `Успешно установил язык чата на русский!`
+ `Successfully set the chat language to English!`

### Setting the bot prefix

+ `Successfully set the chat prefix to "~"`
+ `Successfully set the chat prefix to "?!"`

### Enabling/disabling the bot features

+ `The feature "notify_7tv_events" has been enabled for this chat!`
+ `The feature "notify_bttv_events" has been disabled for this chat!`

## Error handling

+ [Not enough arguments](/wiki/errors)
+ [Incorrect argument](/wiki/errors)
+ [Not found](/wiki/errors)
+ [Something went wrong](/wiki/errors)
