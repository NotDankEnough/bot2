# Stream events

> This command is for broadcaster only.


The `!event` command gives broadcasters the ability to manage events for streams.

## Event types

+ live
+ offline
+ title
+ category
+ message *(Placeholders: `{username}` - Username, `{channel}` - Channel, `{message}` - User message)*
+ github *(Placeholders: `%{sha}` - Commit SHA, `%{author}` - Commit author, `%{message}` - Commit message)*
+ custom

## Event flags

+ `massping` - Massping everyone in chat regardless of their subscription to the event.

## Syntax

### Create a new event

`!event on [name]:[type] [message...]`

+ `[name]` - Twitch username or event name *(custom type only)*.
+ `[type]` - Event type.
+ `[message]` - The message that will be sent with the event.

> Events with types *category* and *title* use *{0}* and *{1}* placeholders
> for old and new values respectively.
> This means that the bot can show changes if you set them
> (e.g. *forsen changed the title from **{0}** to **{1}*** will replace
> with *forsen changed the title from **Just Chatting** to **PUBG***).

### Delete the event

`!event off [name]:[type]`

+ `[name]` - Twitch username or event name *(custom type only)*.
+ `[type]` - Event type.

### Flag/unflag the event

`!event flag [name]:[type] [flag]`

+ `[name]` - Twitch username or event name *(custom type only)*.
+ `[type]` - Event type.
+ `[flag]` - Event flag.

### Call the event

> The bot requires moderator privileges on events with the **"massping"** flag.


`!event call [name]:[type]`

+ `[name]` - Twitch username or event name *(custom type only)*.
+ `[type]` - Event type.

## Usage

### Creating a new event

+ `!event on forsen:live forsen live!`
+ `!event on nymnion/forsen:github %{author} made a new commit (%{sha}): %{message}`
+ `!event on forsen:message forsen just sent a message in #{channel}: {message}`

### Deleting the event

+ `!event off forsen:live`

### Flag/unflag the event

+ `!event flag forsen:live massping`

### Calling the event

+ `!event call forsen:live`

## Responses

### Creating a new event

+ `A new "forsen:live" event has been successfully created! It will send a message when the event occurs.`

### Deleting the event

+ `The "forsen:live" (ID ...) event has been successfully deleted!`

### Adding the flag to the event

+ `Flag "massping" is set for the "forsen:live" event.`

### Removing the flag from the event

+ `Flag "massping" has been removed from the "forsen:live" event.`

### Calling the event

+ `⚡ forsen live!`
+ `⚡ nymnion made a new commit (v12ulu1): fixed typo`

## Important notes

+ If the specified event name does not belong to a Twitch user,
  the event type will automatically be considered ***custom***.

## Error handling

+ [Not enough arguments](/wiki/errors)
+ [Incorrect argument](/wiki/errors)
+ [Insufficient rights](/wiki/errors)
+ [Namesake creation](/wiki/errors)
+ [Not found](/wiki/errors)
+ [Something went wrong](/wiki/errors)
