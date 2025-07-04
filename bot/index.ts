import mineflayer from 'mineflayer';

const bot = mineflayer.createBot({
    host: 'localhost',
    username: 'An1by',
    auth: 'offline'
})

bot.on('kicked', console.log)
bot.on('error', console.log)
