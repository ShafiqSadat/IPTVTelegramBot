# Changelog

All notable changes to the IPTV Telegram Bot project.

## [2.0.0] - 2025-11-01

### 🎉 Major Improvements

#### Enhanced User Experience
- ✨ Added visual feedback with typing and uploading indicators
- 📝 Implemented user-friendly status messages throughout the bot flow
- 🎨 Added emoji-rich messages for better readability
- 🔄 Improved button layouts and navigation flow
- 📸 Enhanced welcome screen with better formatting

#### Code Quality & Architecture
- 🏗️ Complete code refactoring with better organization
- 🔧 Implemented Redis connection pooling for better performance
- 📦 Added proper dependency injection patterns
- 🎯 Separated concerns with dedicated utility classes
- 🧹 Cleaned up code duplication and improved maintainability

#### New Features
- 🔄 Asynchronous file downloads with `CompletableFuture`
- 💾 Redis connection pooling with `RedisManager`
- 📋 Reusable message templates with `MessageTemplates` class
- 🛠️ Text utility functions in `TextUtils` class
- 📝 Comprehensive logging with SLF4J
- ⚡ Better error handling and recovery

#### Bug Fixes
- 🐛 Fixed Redis connection issues (port specification)
- 🔧 Fixed deprecated URL constructor usage
- 📊 Fixed country search parser (adapted to markdown format)
- 🌍 Fixed region parser (adapted to markdown format)
- ✅ Fixed MarkdownV2 special character escaping
- 🔍 Improved error logging and debugging

#### Parser Updates
- 📝 Rewrote country parser to handle markdown list format
- 🌐 Rewrote region parser to handle markdown list format
- ✨ Added debugging output with emoji indicators
- 🔍 Better error messages for troubleshooting

#### Documentation
- 📚 Created comprehensive UX improvements guide
- 🧪 Added detailed testing guide
- 🚀 Created quick start guide
- 📋 Added project review documentation
- 🎨 Created visual UX guide
- 📄 Added this changelog

### 🔧 Technical Changes

#### Dependencies
- Updated Jedis to 5.0.0
- Updated Jsoup to 1.17.2
- Moved SLF4J from test scope to compile scope
- Added proper logging configuration

#### Code Structure
```
New files:
+ src/main/java/com/github/shafiqsadat/IPTV/utils/RedisManager.java
+ src/main/java/com/github/shafiqsadat/IPTV/utils/MessageTemplates.java
+ src/main/java/com/github/shafiqsadat/IPTV/utils/TextUtils.java
+ src/main/resources/simplelogger.properties

Updated files:
~ src/main/java/com/github/shafiqsadat/IPTV/IPTVBot.java (completely refactored)
~ src/main/java/com/github/shafiqsadat/IPTV/Main.java (added graceful shutdown)
~ src/main/java/com/github/shafiqsadat/IPTV/utils/IPTVParser.java (fixed parsers)
~ src/main/java/com/github/shafiqsadat/IPTV/utils/FileDownloader.java (fixed deprecations)
~ pom.xml (updated dependencies)
```

### 🎯 Performance Improvements
- ⚡ Implemented connection pooling for Redis
- 🚀 Asynchronous file downloads
- 💾 Better resource management with try-with-resources
- 🔄 Reduced redundant code execution

### 📊 Testing & Validation
- ✅ All features tested and working
- 🧪 No compilation errors
- 🔍 No runtime errors
- ✨ Better debugging output

### 🌍 IPTV Data Source Updates
The IPTV GitHub repository (iptv-org) changed their data format:
- **Before**: Used HTML tables for all sections
- **After**: Changed countries and regions to markdown lists
- **Our Fix**: Updated parsers to handle both formats seamlessly

### 🚀 What's Next?
- Consider adding database support for caching
- Implement user analytics
- Add more interactive features
- Consider rate limiting for API calls

---

## [1.0.0] - Original Release

### Initial Features
- Basic IPTV channel browsing
- Category-based filtering
- Language-based filtering
- Country-based search
- Region-based filtering
- M3U file downloads
- Basic Telegram bot functionality
