# IPTV Telegram Bot

IPTV Telegram Bot is a bot that lets you watch IPTV streams right in Telegram App. IPTV stands for Internet Protocol Television, which is a way of delivering live TV channels over the internet. With this bot, you can send the name of the channel you want to watch, and the bot will respond with available streams to watch. There are over 60000+ online streams from all over the world, covering various genres and languages.

![Screenshot of IPTV Telegram Bot](https://i.imgur.com/XVsp1Nd.png)

![Screenshot of IPTV Telegram Bot](https://raw.githubusercontent.com/ShafiqSadat/IPTVTelegramBot/master/screenshots/1.gif)
## How to use

- Clone this repository or download the zip file.
- Install the requirements using `mvn install`.
- Create a bot using [@BotFather](https://t.me/BotFather) and get the bot token.
- In BotFather, send the "/setmenubutton" command, select your bot, and send the following link: ```https://iptvnator.vercel.app/```. Then, provide a name for the button, such as "Open Player."
- Rename example_local.properties into local.properties under /src/main/resources/example_local.properties
- Edit the local.properties file and enter your bot token and username.
- Run the Main.java file using `java Main`.
- Start your bot and enjoy watching IPTV streams.

## Credits

- IPTV API: [iptv-org/iptv](https://github.com/iptv-org/iptv)
- Telegram API: [rubenlagus/TelegramBots](https://github.com/rubenlagus/TelegramBots)
- IPTV Player: [4gray/iptvnator](https://github.com/4gray/iptvnator)

## License

This project is licensed under the MIT License - see the [LICENSE] file for details.

## Contributing

If you want to contribute to this project, you are welcome to do so. Please follow these steps:

- Fork this repository and create a new branch for your feature or bug fix.
- Write your code and test it locally.
- Commit and push your changes to your forked repository.
- Create a pull request with a clear description of your changes and a link to the issue (if any) that you are addressing.
- Wait for the maintainer to review and merge your pull request.

## Contact

If you have any questions, suggestions, or feedback, you can contact me via:

- Email: ShafiqSadat2012@gmail.com
- Telegram: [@Shafiq](https://t.me/Shafiq)

## License
IPTVTelegramBot is licensed under the MIT License. The terms are as follows:

```
The MIT License (MIT)

Copyright (c) 2024 Shafiq Sadat

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
