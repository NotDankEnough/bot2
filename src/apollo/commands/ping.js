// Libraries.
const { readFileSync } = require("fs");
const tmi = require("tmi.js");

/**
 * Help.
 */
module.exports.help = {
    name: "Ping!",
    author: "ilotterytea",
    description: "Checking if it's alive, and a bunch of other data.",
    cooldownMs: 0,
    superUserOnly: false
}

/**
 * Run the command.
 * @param {*} client Client.
 * @param {*} target Target.
 * @param {*} user User.
 * @param {*} msg Message.
 * @param {*} args Arguments.
 */
 exports.run = async (client, target, user, msg, args = {
    emote_data: any,
    emote_updater: any
}) => {
    function timeformat(seconds){
        function pad(s){
            return (s < 10 ? '' : '') + s;
        }
        var days = Math.floor(seconds / (60*60*24))
        var hours = Math.floor(seconds / (60 * 60) % 24);
        var minutes = Math.floor(seconds % (60*60) / 60);
        var sec = Math.floor(seconds % 60); 
    
        
        return `${pad(days)}d. ${pad(hours)}:${pad(minutes)}:${pad(sec)}`;
    }

    const emotes = JSON.parse(readFileSync(`./saved/emote_data.json`, {encoding: "utf-8"}));

    let items = Object.keys(emotes).map((key) => {
        return [key, dict[key]]
    });

    items.sort((f, s) => {
        return s[1] - f[1]
    });

    let top_emotes = items.slice(0, 5);

    client.ping().then((ms) => {
        client.say(target, `@${user.username}, Pong! Session uptime: ${timeformat(process.uptime())}! ${process.env.tv_options.joinasanonymous.split(',').length} channels are tracked in anonymous mode, landed in ${process.env.tv_options_joinasclient.split(',').length} channels. The most used 7TV emote: ${top_emotes[0][0]} (${top_emotes[0][1]})! Latency to TMI: ${ms}ms forsenLevel`);
    });
};