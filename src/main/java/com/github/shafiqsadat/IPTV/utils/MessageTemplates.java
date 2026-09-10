package com.github.shafiqsadat.IPTV.utils;

public class MessageTemplates {
    
    public static String getWelcomeMessage(String firstName, String lastName) {
        String fullName = (firstName + " " + (lastName != null ? lastName : "")).trim();
        return String.format("""
                👋 Hi %s, Welcome to our IPTV Bot!
                
                🎉 We are thrilled to have you here. With our bot, you can access over 60,000+ IPTV channels from around the world!
                
                🌟 Features:
                • Browse channels by Category, Language, Country, or Region
                • Download M3U playlists instantly
                • Copy streaming links directly
                • Watch channels online using our web player
                
                📱 To get started, tap on "Get IPTV" below or use the menu button.
                
                ❓ Need help? Tap "How it works?" to learn more.
                
                Thank you for choosing our IPTV Bot. Happy streaming! 🎬""", fullName);
    }
    
    public static String getHowItWorksMessage() {
        return """
                📖 How Our IPTV Bot Works:
                
                1️⃣ Click the "📺 Get IPTV" button
                2️⃣ Choose your preferred filter:
                   • 📂 By Category (Sports, News, Movies, etc.)
                   • 🌐 By Language
                   • 🏳️ By Country
                   • ®️ By Region
                3️⃣ Select your desired option from the list
                4️⃣ Receive the M3U file with streaming links
                5️⃣ Use the menu button to open our web player
                
                💡 Tip: You can download the file or copy the streaming link to use in your favorite IPTV player!
                
                Enjoy your IPTV experience! 🎊""";
    }
    
    public static String getWhatIsIPTVMessage() {
        return """
                📺 What is IPTV?
                
                IPTV stands for Internet Protocol Television. It's a service that delivers television programming and video content through an internet connection.
                
                ✨ Why Choose IPTV?
                • 💰 More affordable than traditional cable/satellite TV
                • 🕐 Watch what you want, when you want
                • 🌍 Access content from around the world
                • 📱 Watch on any device with internet
                • 🎯 Customize your viewing experience
                
                🚀 Getting Started is Easy!
                Our bot makes it simple to access thousands of free IPTV channels. Just select your preferences and start streaming!
                
                🔒 Note: This bot provides access to publicly available IPTV streams.""";
    }
    
    public static String getSelectCategoryMessage() {
        return "📂 Please select a category from the list below:";
    }
    
    public static String getSelectLanguageMessage() {
        return "🌐 Please select a language from the list below:";
    }
    
    public static String getSelectRegionMessage() {
        return "®️ Please select a region from the list below:";
    }
    
    public static String getCountrySearchMessage() {
        return """
                🏳️ Country Search
                
                Please enter your country name or use the country flag emoji.
                
                Examples:
                • "United States" or 🇺🇸
                • "Japan" or 🇯🇵
                • "Germany" or 🇩🇪
                
                💡 Tip: Just type the country name for best results!""";
    }
    
    public static String getDownloadingMessage() {
        return "⏳ Preparing your IPTV playlist... Please wait.";
    }
    
    public static String getNoChannelsFoundMessage() {
        return "❌ Sorry, we couldn't find any channels matching your request. Please try a different search term.";
    }
    
    public static String getErrorMessage() {
        return "⚠️ Oops! Something went wrong. Please try again later or contact support if the problem persists.";
    }
    
    public static String getBackToMainMenuMessage() {
        return "⬅️ Returning to main menu...";
    }
    
    public static String getChannelInfoCaption(String type, String name, String count, String streamLink) {
        String channels = count.isBlank() ? "" : "\n📊 Channels: " + escapeMarkdownV2(count);
        return String.format("""
                ✅ %s: %s%s
                🔗 Stream Link: `%s`

                💡 Download the file or copy the link to use in your IPTV player\\!""",
                escapeMarkdownV2(type), escapeMarkdownV2(name), channels, escapeMarkdownV2Code(streamLink));
    }

    private static String escapeMarkdownV2(String text) {
        return text.replaceAll("([_*\\[\\]()~`>#+=|{}.!\\-\\\\])", "\\\\$1");
    }

    // Inside a code span only backslash and backtick are special
    private static String escapeMarkdownV2Code(String text) {
        return text.replaceAll("([`\\\\])", "\\\\$1");
    }
}
