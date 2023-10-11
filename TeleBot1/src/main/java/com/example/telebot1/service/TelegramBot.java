package com.example.telebot1.service;

import com.example.telebot1.config.BotConfig;
import com.example.telebot1.model.User;
import com.example.telebot1.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScope;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.xml.stream.events.Comment;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    final BotConfig config;
    static final String HELP_TEXT = "This bot was created to demonstrate telegram abilities. " +
            "You can choose command from main menu." +
            "Type /start to see a welcome message.\n" +
            "Type /help to see this message again.\n";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "get your data"));
        listOfCommands.add(new BotCommand("/deletedata", "delete my data"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }



    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            switch (messageText) {
                case "/start":
                    registerUser(update.getMessage());
                    try {
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case "/help":
                    sendMessage(chatId, HELP_TEXT);
                    break;
                default: sendMessage(chatId, "Sorry, command was not recognized.");

            }
        }



    }

    private void registerUser(Message message) {
        if(userRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();

            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("user saved: " + user);
        }

    }

    private void startCommandReceived(long chatId, String name) throws TelegramApiException {

        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!" + "\uD83D\uDE0A ");
//        String answer = "Hi, " + name + ", nice to meet you!";
        log.info("Replied to user " + name);
        sendMessage(chatId, answer);

    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("weather");
        row.add("get randeom joke");

        keyboardRows.add(row);

        row.add("register");
        row.add("check my data");
        row.add("delete my data");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);

        try  {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occured: " + e.getMessage());
        }
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }
}
