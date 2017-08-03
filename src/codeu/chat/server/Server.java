// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


package codeu.chat.server;

import codeu.chat.common.*;
import codeu.chat.util.*;
import codeu.chat.util.connections.Connection;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;

public final class Server {

    private interface Command {
        void onMessage(InputStream in, OutputStream out) throws IOException;
    }

    private static final Logger.Log LOG = Logger.newLog(Server.class);

    private static final int RELAY_REFRESH_MS = 5000;  // 5 seconds

    private final Timeline timeline = new Timeline();

    private final Map<Integer, Command> commands = new HashMap<>();

    private final Uuid id;
    private final Secret secret;

    private final Model model = new Model();
    private final View view = new View(model);
    private final Controller controller;

    private final Relay relay;
    private Uuid lastSeen = Uuid.NULL;

    private LogQueue logQueue;

    //creates instance of server's information
    private static final ServerInfo info = new ServerInfo();

    public Server(final Uuid id, final Secret secret, final Relay relay) {

        this.id = id;
        this.secret = secret;
        this.controller = new Controller(id, model);
        this.relay = relay;
        this.logQueue = new LogQueue();

        // New Message - A client wants to add a new message to the back end.
        this.commands.put(NetworkCode.NEW_MESSAGE_REQUEST, new Command() {
            @Override
            public void onMessage(InputStream in, OutputStream out) throws IOException {

                final Uuid author = Uuid.SERIALIZER.read(in);
                final Uuid conversation = Uuid.SERIALIZER.read(in);
                final String content = Serializers.STRING.read(in);

                final Message message = controller.newMessage(author, conversation, content);

                Serializers.INTEGER.write(out, NetworkCode.NEW_MESSAGE_RESPONSE);
                Serializers.nullable(Message.SERIALIZER).write(out, message);
                logQueue.getTransactions().add("ADD-MESSAGE " + message.id.toString() + " " + author.toString() + " " + conversation.toString()
                        + " " + "\"" + content + "\"" + " " + message.creation.inMs());
                timeline.scheduleNow(createSendToRelayEvent(
                        author,
                        conversation,
                        message.id));
            }
        });

        // New User - A client wants to add a new user to the back end.
        this.commands.put(NetworkCode.NEW_USER_REQUEST, new Command() {
            @Override
            public void onMessage(InputStream in, OutputStream out) throws IOException {

                final String name = Serializers.STRING.read(in);
                final User user = controller.newUser(name);


        Serializers.INTEGER.write(out, NetworkCode.NEW_USER_RESPONSE);
        Serializers.nullable(User.SERIALIZER).write(out, user);
        logQueue.getTransactions().add("ADD-USER " + user.id.toString() + " " + "\""+ user.name + "\"" + " "+ user.creation.inMs());
      }
    });

        // New Conversation - A client wants to add a new conversation to the back end.
        this.commands.put(NetworkCode.NEW_CONVERSATION_REQUEST, new Command() {
            @Override
            public void onMessage(InputStream in, OutputStream out) throws IOException {

                final String title = Serializers.STRING.read(in);
                final Uuid owner = Uuid.SERIALIZER.read(in);
                final AccessLevel defaultAccessLevel = AccessLevel.valueOf(Serializers.STRING.read(in));
                final ConversationHeader conversation = controller.newConversation(title, owner, defaultAccessLevel);

                Serializers.INTEGER.write(out, NetworkCode.NEW_CONVERSATION_RESPONSE);
                Serializers.nullable(ConversationHeader.SERIALIZER).write(out, conversation);

                logQueue.getTransactions().add("ADD-CONVERSATION " + conversation.id.toString() + " " + owner.toString() + " " + "\""+
                        title + "\""+ " " + conversation.creation.inMs() + " " + conversation.defaultAccessLevel);

            }
        });

        // New Interest - A client wants to add a new interest to the back end.
    this.commands.put(NetworkCode.NEW_INTERESTS_REQUEST,  new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Uuid userid = (Uuid.SERIALIZER).read(in);
        final Uuid interest = (Uuid.SERIALIZER).read(in);
        final Interests interests = controller.newInterest(userid, interest);

        Serializers.INTEGER.write(out, NetworkCode.NEW_INTERESTS_RESPONSE);
        Serializers.nullable(Interests.SERIALIZER).write(out, interests);

        logQueue.getTransactions().add("ADD-INTEREST " + userid.toString() + " " + interest.toString() + " " + interests.creation.inMs());
      }
    });

    // Get Users - A client wants to get all the users from the back end.
        this.commands.put(NetworkCode.GET_USERS_REQUEST, new Command() {
            @Override
            public void onMessage(InputStream in, OutputStream out) throws IOException {

                final Collection<User> users = view.getUsers();

                Serializers.INTEGER.write(out, NetworkCode.GET_USERS_RESPONSE);
                Serializers.collection(User.SERIALIZER).write(out, users);
            }
        });

        // Get Conversations - A client wants to get all the conversations from the back end.
        this.commands.put(NetworkCode.GET_ALL_CONVERSATIONS_REQUEST, new Command() {
            @Override
            public void onMessage(InputStream in, OutputStream out) throws IOException {

                final Collection<ConversationHeader> conversations = view.getConversations();

                Serializers.INTEGER.write(out, NetworkCode.GET_ALL_CONVERSATIONS_RESPONSE);
                Serializers.collection(ConversationHeader.SERIALIZER).write(out, conversations);
            }
        });

        // Get Interests - A client wants to get all the interests from the back end.
    this.commands.put(NetworkCode.GET_INTERESTS_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {

        final Collection<Interests> interests = view.getInterests();

        Serializers.INTEGER.write(out, NetworkCode.GET_INTERESTS_RESPONSE);
        Serializers.collection(Interests.SERIALIZER).write(out, interests);
      }
    });

    // Get Conversations By Id - A client wants to get a subset of the converations from
        //                           the back end. Normally this will be done after calling
        //                           Get Conversations to get all the headers and now the client
        //                           wants to get a subset of the payloads.
        this.commands.put(NetworkCode.GET_CONVERSATIONS_BY_ID_REQUEST, new Command() {
            @Override
            public void onMessage(InputStream in, OutputStream out) throws IOException {

                final Collection<Uuid> ids = Serializers.collection(Uuid.SERIALIZER).read(in);
                final Collection<ConversationPayload> conversations = view.getConversationPayloads(ids);

                Serializers.INTEGER.write(out, NetworkCode.GET_CONVERSATIONS_BY_ID_RESPONSE);
                Serializers.collection(ConversationPayload.SERIALIZER).write(out, conversations);
            }
        });

        // Get Messages By Id - A client wants to get a subset of the messages from the back end.
        this.commands.put(NetworkCode.GET_MESSAGES_BY_ID_REQUEST, new Command() {
            @Override
            public void onMessage(InputStream in, OutputStream out) throws IOException {

                final Collection<Uuid> ids = Serializers.collection(Uuid.SERIALIZER).read(in);
                final Collection<Message> messages = view.getMessages(ids);

                Serializers.INTEGER.write(out, NetworkCode.GET_MESSAGES_BY_ID_RESPONSE);
                Serializers.collection(Message.SERIALIZER).write(out, messages);
            }
        });

        //Gets the Server information
        this.commands.put(NetworkCode.SERVER_VERSION_REQUEST, new Command() {
            @Override
            public void onMessage(InputStream in, OutputStream out) throws IOException {

                Serializers.INTEGER.write(out, NetworkCode.SERVER_VERSION_RESPONSE);
                Uuid.SERIALIZER.write(out, info.version);
            }
        });

        // Get Server Uptime - A client wants to get the amount of time the server has been running.
        this.commands.put(NetworkCode.SERVER_UPTIME_REQUEST, new Command() {
            @Override
            public void onMessage(InputStream in, OutputStream out) throws IOException {
                Serializers.INTEGER.write(out, NetworkCode.SERVER_UPTIME_RESPONSE);
                Time.SERIALIZER.write(out, info.startTime);
            }
        });

        // Remove Interest - A client wants to remove a interest
        this.commands.put(NetworkCode.REMOVE_INTERESTS_REQUEST,  new Command() {
            @Override
            public void onMessage(InputStream in, OutputStream out) throws IOException {

                final Uuid userid = (Uuid.SERIALIZER).read(in);
                final Uuid interest = (Uuid.SERIALIZER).read(in);
                final Interests interests = controller.removeInterest(userid, interest);

                Serializers.INTEGER.write(out, NetworkCode.REMOVE_INTERESTS_RESPONSE);
                Serializers.nullable(Interests.SERIALIZER).write(out, interests);

                logQueue.getTransactions().add("REMOVE-INTEREST " + userid.toString() + " " + interest.toString());
            }
        });

    // Status Update - A client wants to get an update on all the things they're interested in.
    // writes the following items:
    //   1. The updates about the users being followed - A HashMap of key-value pairs where
    //      the key is the userid and the value is a set of conversations
    //   2. The updates about the conversations being followed - A HashMap of key-value pairs
    //      where the key is the conversation id and the values is a set of messages
    this.commands.put(NetworkCode.STATUS_UPDATE_REQUEST, new Command() {
      @Override
      public void onMessage(InputStream in, OutputStream out) throws IOException {
        final Uuid userid = Uuid.SERIALIZER.read(in);
        final HashMap<Uuid, Collection<ConversationHeader>> interestedUsers = new HashMap<Uuid, Collection<ConversationHeader>>();
        final HashMap<Uuid, Integer> interestedConversations = new HashMap<Uuid, Integer>();

        final Interests interests = view.findInterests(userid);
        final Collection<Uuid> uuids = interests.interests;
        final Time lastUpdate = interests.lastStatusUpdate;

        final Collection<ConversationHeader> conversations = new HashSet<ConversationHeader>(view.getConversations());

        for (ConversationHeader convo : conversations) {
          Uuid owner = convo.owner;
          if (convo.creation.compareTo(lastUpdate) >= 0 && uuids.contains(owner)) {
            Collection<ConversationHeader> interestedConvo = interestedUsers.get(owner);
            interestedConvo = interestedConvo == null ? new HashSet<ConversationHeader>() : interestedConvo;
            interestedConvo.add(convo);
            interestedUsers.put(owner, interestedConvo);
          }
          for (Message message = view.findMessage(view.getConversationPayload(convo.id).firstMessage);
            message != null;
            message = view.findMessage(message.next)) {
            if (message.creation.compareTo(lastUpdate) >= 0) {
              if (uuids.contains(message.author)) {
                Collection<ConversationHeader> interestedConvo = interestedUsers.get(owner);
                interestedConvo = interestedConvo == null ? new HashSet<ConversationHeader>() : interestedConvo;
                interestedConvo.add(convo);
                interestedUsers.put(message.author, interestedConvo);
              }
              if (uuids.contains(convo.id)) {
                Integer interestedMess = interestedConversations.get(convo.id);
                interestedMess = interestedMess == null ? 1 : interestedMess + 1;
                interestedConversations.put(convo.id, interestedMess);
              }
            }
          }
        }

        interests.lastStatusUpdate = Time.now();
        Serializers.INTEGER.write(out, NetworkCode.STATUS_UPDATE_RESPONSE);
        Serializers.collection(Uuid.SERIALIZER).write(out, interestedUsers.keySet());
        Serializers.collection(Serializers.collection(ConversationHeader.SERIALIZER)).write(out, interestedUsers.values());
        Serializers.collection(Uuid.SERIALIZER).write(out, interestedConversations.keySet());
        Serializers.collection(Serializers.INTEGER).write(out, interestedConversations.values());
        logQueue.getTransactions().add("STATUS-UPDATE " + userid.toString() + " " + interests.lastStatusUpdate.inMs());
      }
    });this.timeline.scheduleNow(new Runnable() {
      @Override
      public void run() {
        try {

                    LOG.info("Reading update from relay...");

                    for (final Relay.Bundle bundle : relay.read(id, secret, lastSeen, 32)) {
                        onBundle(bundle);
                        lastSeen = bundle.id();
                    }

                } catch (Exception ex) {

                    LOG.error(ex, "Failed to read update from relay.");

                }

                timeline.scheduleIn(RELAY_REFRESH_MS, this);
            }
        });
    }

    public void handleConnection(final Connection connection) {
        timeline.scheduleNow(new Runnable() {
            @Override
            public void run() {
                try {

                    LOG.info("Handling connection...");

                    final int type = Serializers.INTEGER.read(connection.in());
                    final Command command = commands.get(type);

                    if (command == null) {
                        // The message type cannot be handled so return a dummy message.
                        Serializers.INTEGER.write(connection.out(), NetworkCode.NO_MESSAGE);
                        LOG.info("Connection rejected");
                    } else {
                        command.onMessage(connection.in(), connection.out());
                        LOG.info("Connection accepted");
                    }

                } catch (Exception ex) {

                    LOG.error(ex, "Exception while handling connection.");

                }

                try {
                    connection.close();
                } catch (Exception ex) {
                    LOG.error(ex, "Exception while closing connection.");
                }
            }
        });
    }

    private void onBundle(Relay.Bundle bundle) {

        final Relay.Bundle.Component relayUser = bundle.user();
        final Relay.Bundle.Component relayConversation = bundle.conversation();
        final Relay.Bundle.Component relayMessage = bundle.user();
      //  final Relay.Bundle.Component relayAccessLevel = bundle.defaultAccessLevel();

        User user = model.userById().first(relayUser.id());

        if (user == null) {
            user = controller.newUser(relayUser.id(), relayUser.text(), relayUser.time());
        }

        ConversationHeader conversation = model.conversationById().first(relayConversation.id());

        if (conversation == null) {

            // As the relay does not tell us who made the conversation - the first person who
            // has a message in the conversation will get ownership over this server's copy
            // of the conversation.
            conversation = controller.newConversation(relayConversation.id(),
                    relayConversation.text(),
                    user.id,
                    relayConversation.time(),
                    relayConversation.defaultAccessLevel());
        }

        Message message = model.messageById().first(relayMessage.id());

        if (message == null) {
            message = controller.newMessage(relayMessage.id(),
                    user.id,
                    conversation.id,
                    relayMessage.text(),
                    relayMessage.time());
        }
    }

    private Runnable createSendToRelayEvent(final Uuid userId,
                                            final Uuid conversationId,
                                            final Uuid messageId) {
        return new Runnable() {
            @Override
            public void run() {
                final User user = view.findUser(userId);
                final ConversationHeader conversation = view.findConversation(conversationId);
                final Message message = view.findMessage(messageId);
                relay.write(id,
                        secret,
                        relay.pack(user.id, user.name, user.creation),
                        relay.pack(conversation.id, conversation.title, conversation.creation),
                        relay.pack(message.id, message.content, message.creation));
            }
        };
    }

    public void readTransactionLog() {

        try {
            File transactionLog = new File("transaction_log.txt");
            if (!transactionLog.exists()) {
                transactionLog.createNewFile();
            }
            Scanner scan = new Scanner(transactionLog);   //read transaction log if it already exists
            while (scan.hasNextLine()) {
                String item = scan.nextLine().trim(); //item in transaction log
                Tokenizer tokenizer = new Tokenizer(item);
                String action = tokenizer.next();

                if (action.equals("ADD-CONVERSATION")) {
                    Uuid uuid = Uuid.parse(tokenizer.next());
                    Uuid ownerUuid = Uuid.parse(tokenizer.next());
                    String title = tokenizer.next();
                    long timeInMs = Long.parseLong(tokenizer.next());
                    Time timeCreated = Time.fromMs(timeInMs);
                    AccessLevel defaultAccess = AccessLevel.valueOf(tokenizer.next());
                    controller.newConversation(uuid, title, ownerUuid, timeCreated, defaultAccess);
                } else if (action.equals("ADD-USER")) {
                    Uuid uuid = Uuid.parse(tokenizer.next());
                    String userName = tokenizer.next();
                    long timeInMs = Long.parseLong(tokenizer.next());
                    Time timeCreated = Time.fromMs(timeInMs);
                    controller.newUser(uuid, userName, timeCreated);
                } else if (action.equals("ADD-MESSAGE")) {
                    Uuid uuid = Uuid.parse(tokenizer.next());
                    Uuid authorUuid = Uuid.parse(tokenizer.next());
                    Uuid conversationUuid = Uuid.parse(tokenizer.next());
                    String content = tokenizer.next();
                    long timeInMs = Long.parseLong(tokenizer.next());
                    Time timeCreated = Time.fromMs(timeInMs);
                    controller.newMessage(uuid, authorUuid, conversationUuid, content, timeCreated);
                } else if (action.equals("ADD-INTEREST")) {
                    Uuid user = Uuid.parse(tokenizer.next());
                    Uuid interest = Uuid.parse(tokenizer.next());
                    long timeInMs = Long.parseLong(tokenizer.next());
                    Time timeCreated = Time.fromMs(timeInMs);
                    controller.newInterest(user, interest, timeCreated);
                } else if (action.equals("STATUS-UPDATE")) {
                    Uuid userId = Uuid.parse(tokenizer.next());
                    long timeInMs = Long.parseLong(tokenizer.next());
                    Time timeCreated = Time.fromMs(timeInMs);
                    view.findInterests(userId).lastStatusUpdate = timeCreated;
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
