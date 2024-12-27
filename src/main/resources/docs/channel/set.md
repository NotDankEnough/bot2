# Settings

> This command is for broadcaster only.


The `!set` command gives broadcasters ability to customize the bot as they need it to be more fitted for chat.

## Syntax

### Set the bot localization for the chat

`!set locale [lang]`

+ `[lang]` - Language name in English and lowercase.
  Available languages at the moment: **english**, **russian**.

### Set the bot prefix

`!set prefix [characters]`

+ `[characters]` - Characters to be set as a prefix.

## Usage

### Setting the bot localization

+ `!set locale russian`
+ `!set locale english`

### Setting the bot prefix

+ `!set prefix ~`
+ `!set prefix ?!`

## Responses

### Setting the bot localization

+ `Успешно установил язык чата на русский!`
+ `Successfully set the chat language to English!`

### Setting the bot prefix

+ `Successfully set the chat prefix to "~"`
+ `Successfully set the chat prefix to "?!"`

## Error handling

+ [Not enough arguments](/wiki/errors)
+ [Incorrect argument](/wiki/errors)
+ [Not found](/wiki/errors)
+ [Something went wrong](/wiki/errors)
