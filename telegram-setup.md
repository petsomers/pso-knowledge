# Telegram Bot Setup Guide

  ## Step 1: Create a Bot via BotFather

  1. Open Telegram on your phone
  2. Search for `@BotFather` in the search bar
  3. Start a conversation and send `/newbot`
  4. Choose a display name for your bot (e.g., "PSO Knowledge Bot")
  5. Choose a username ending in `bot` (e.g., `pso_knowledge_bot`)
  6. BotFather will reply with your **bot token** - save it securely

  ## Step 2: Get Your Telegram User ID

  1. In Telegram, search for `@userinfobot`
  2. Start a conversation and send any message
  3. It will reply with your **user ID** (a number like `123456789`)

  ## Step 3: Configure the Application

  Edit `src/main/resources/application.yml` and fill in your values:

  ```yaml
  telegram:
    token: "YOUR_BOT_TOKEN_FROM_BOTFATHER"
    username: "your_bot_username"
    allowed-user-ids:
      - 123456789  # Replace with your actual user ID

  - token: The token BotFather gave you in Step 1
  - username: The bot username you chose (without the @)
  - allowed-user-ids: List of Telegram user IDs allowed to interact with the bot

  Step 4: Start the Application

  mvn spring-boot:run

  The bot will automatically start long-polling Telegram for messages.

  Step 5: Test the Bot

  1. Open Telegram and search for your bot by its username
  2. Start a conversation with /start or any message
  3. Store mode: Send any text - it gets saved as a markdown file in the Inbox
  4. Search mode: Send a message starting with ? (e.g., ?Who is Bert Reyniers?) - the bot queries your knowledge base and replies with an answer

  Adding More Users

  To allow additional users, add their user IDs to the allowed-user-ids list in application.yml and restart the application.