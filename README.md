# 95 Degrees Radio
95 Degrees Radio was a Discord radio bot which operated within the [95 Degrees Discord server](https://www.95degrees.cafe/). The code is still heavily optimised for this specific server, and is **not designed to operate across multiple servers**. You might run into weird results if you try. It is also set up to load configuration from a MongoDB database and will probably explode if it's not set up just right.

### Quick Facts
- Uses the JDA Discord library
- Uses LavaPlayer for basically everything to do with audio

## Structure
The vast majority of the code is split up into different modules inside of the root package. All modules are registered in the root *Radio* class, which is the entrypoint to the bot.

### SongOrchestrator
*SongOrchestrator* is the main class responsible for determining what the radio should be playing at a given point. This is a bit more convoluted than normal music bots since by design it should always be playing *something*. The orchestrator gets its songs by querying an active playlist as to which song to play next (Playlist#provideNextSong) - there is always one active at all times.

### Playlists
There are a few types of playlists. The most common type is the *RadioPlaylist*. This essentially just contains a shuffled queue of songs and jingles. When asked for a song, it'll retrieve the next one in the queue and then add it back to the end of the queue (if it is not a suggestion & instead part of the regular rotation).

Radio playlists can be loaded in a few ways: as a physical *playlist-info.txt* file containing the playlist metadata, as a Spotify playlist (where songs are converted their best into equivelent YouTube files), or loaded from MongoDB.

### Song Types
Basically anything that the radio is currently playing is classed as a song type, even if it's not technically a song. Each song type would have slightly different behaviour in terms of how album art would display, what status the bot should show, etc.

Specifically:
- **songs** - well this actually is for songs. Both for user suggestions & the ones normally in the queue
- **jingle** - this is a radio after all
- **special** - this is for any types of audio which isn't really categorised. It is used in a few places for some of the scheduled tasks, like the scheduled Monstercat weekly event
- **advertisement** - yes we once had adverts on the radio! Not actual adverts, just fun little shoutouts to a few community members to make the whole thing even more radio-like
- **reward** - one way we rewarded people for listening to the radio was occasionally giving them rewards in the form of degreecoins (virtual currency for 95 Degrees). This was a reminder to tell people they can claim their rewards
- **quiz** - specifically for quiz questions which has their own behaviour, explained below

### Quiz Mode
Did you know 95 Degrees Radio contains an entire trivia engine? This powered 95 Degrees Trivia - a short lived event which replicated HQ Trivia (when it was actually a thing lol). Because this involved a lot of audio synchronising (e.g. for when a question is active, waiting music, sound effects for showing the results), it made sense to roll these features into the radio which is designed to already accomplish a lot of what is needed.

Aside from audio stuff, the quiz mode is fairly simple and most of the logic is just contained within *QuizPlaylist*. Though there is a cool remote control socket thingy for it inside *QuizManager*.

### Spotify
The radio has Spotify integrations in a few ways.
- Songs can be queued up by passing in a Spotify track URL. The radio basically attempts to resolve the name & author, and will try do a search for it on YouTube so it's able to play it. Most of the time it's fairly accurate.
- Songs can be queued by adding songs within Spotify itself to a special playlist, which the radio continuously polls for new entries.
- Spotify is a source for some of the default rotating playlists. This made it a lot easier to update the radio with new songs.
- The radio can copy what you're listening to on Spotify and queue it on the radio by looking at your Discord status.

##### (I know there's a Spotify secret key in the repo but I'm too lazy to remove it from the commit history so it's just been rotated)

### Remote Control
95 Degrees Radio also consisted of a C# desktop app, which allowed you to queue up songs (from Spotify, YouTube, or directly uploaded), display live lyrics, become a DJ (if you were staff), and show your listening status as a Discord rich presence. Sometimes it broke mainly because of issues with Socket.io versions not working nicely together across C# to Java.

Most of the server-side code for this is inside *RPCSocketManager*.

## Why is it Java??
It was going to be C# originally. But then I realised C# audio libraries for Discord (at the time) were just bad. The most popular approach was using LavaLink - which is essentially using LavaPlayer but remotely and not quite as nicely.
