package lv.all_sins;

import com.sun.jdi.InternalException;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.RestClient;
import reactor.core.publisher.Mono;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        System.out.println("iroiro start!");

        // Attempt to read token from file. In case of errors, exit the program and print debug info.
        String tokenFileName = "iroiro.token";
        String token;
        try {
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(tokenFileName));
            String currentLine;
            while ((currentLine = bufferedReader.readLine()) != null) {
                // Read a full line of the file while there is one. Result gets appended to StringBuilder.
                stringBuilder.append(currentLine);
            }
            token = stringBuilder.toString();
            bufferedReader.close();
        } catch (FileNotFoundException e) {
            throw new InternalException("Token file not found in root of application!");
        } catch (IOException e) {
            throw new InternalException("Token file could not be read.");
        }

        final DiscordClient client = DiscordClient.create(token);
        final GatewayDiscordClient gateway = client.login().block();
        final ArrayList<String> adminUsers = new ArrayList<>(List.of("Tsu#5168"));

        // Check if gateway was created successfully and fetch applicationID.
        // Prevent potential null pointer exception later on.
        if (gateway == null) {
            throw new InternalError("Couldn't log in to create a gateway!");
        }
        final long applicationId;
        try {
            // applicationId = client.getApplicationId().block();
            applicationId = RestClient.create(token).getApplicationId().block();
        } catch (NullPointerException e) {
            throw new InternalException("Couldn't fetch application ID.");
        }

        // Build our command's definition
        ApplicationCommandRequest greetCmdRequest = ApplicationCommandRequest.builder()
                .name("color")
                .description("Set color for your nickname.")
                .addOption(ApplicationCommandOptionData.builder()
                        .name("name")
                        .description("Description?")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .required(true)
                        .build()
                ).build();

        // Create the command with Discord
        // Apparently Global commands are updated with rate limitation - around 1 hour.
        // It is therefore suggested to use guild commands for testing and then change it to
        // gloabl command when ready to deploy.
        // TODO: I'm not sure yet if global commands are even relevant.
        // Changing to guild command for the time being.
        client.getApplicationService()
                // .createGlobalApplicationCommand(applicationId, greetCmdRequest)
                // TODO: Change guildID from long literal to programmatically fetched from client.
                .createGuildApplicationCommand(applicationId, 1073336951173820558L, greetCmdRequest)
                .subscribe();

        /*
        // Subscribe to MessageCreateEvent and execute the following code when it's fired.
        gateway.on(MessageCreateEvent.class).subscribe(event -> {
            final Message message = event.getMessage();
            final MessageChannel channel = message.getChannel().block();
            final Optional<User> author = message.getAuthor();
            final String username;
            if (author.isEmpty()) {
                username = "USERNAME_GET_ERROR";
                throw new InternalError("Message author has no username!");
            } else {
                username = author.get().getUsername();
            }
            final String messageText = message.getContent();

            if (channel == null) {
                throw new InternalError("CHANNEL_IS_NULL");
            }

            if (messageText.startsWith("$color")) {

                long startTimer = System.currentTimeMillis();
                message.addReaction(ReactionEmoji.unicode("\u2705")).block();
                System.out.println("Processing request from " + username);
                float timeTaken = (System.currentTimeMillis() - startTimer) / 1000f;
                // if (timeTaken > 5000L) {
                //    titleMessage = "Request took a worrying amount of time! " + username + ", please contact @Tsu!";
                //} else {
                //    titleMessage = "Request took " + timeTaken + " seconds.";
                //}
                channel.createMessage("Hello there "+username+"! I executed this command in "+timeTaken+" seconds.");
            }
        }); */

        // TODO: wtf this official documentation way just exits the application. Probably some Reactor BS.

        Mono<Void> login = client.withGateway((GatewayDiscordClient gateway2) ->
                gateway2.on(MessageCreateEvent.class, event -> {
                    Message message = event.getMessage();

                    if (message.getContent().equalsIgnoreCase("!ping")) {
                        return message.getChannel()
                                .flatMap(channel -> channel.createMessage("pong!"));
                    }

                    return Mono.empty();
                }));


        gateway.onDisconnect().retry().block();
    }
}