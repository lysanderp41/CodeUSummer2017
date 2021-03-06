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

package codeu.chat.client.commandline;

import codeu.chat.client.core.*;
import codeu.chat.common.AccessLevel;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.ServerInfo;
import codeu.chat.common.UserAccessLevel;
import codeu.chat.util.Time;
import codeu.chat.util.Tokenizer;
import codeu.chat.util.Uuid;

import java.io.IOException;
import java.util.*;


public final class Chat {

  // PANELS
  //
  // We are going to use a stack of panels to track where in the application
  // we are. The command will always be routed to the panel at the top of the
  // stack. When a command wants to go to another panel, it will add a new
  // panel to the top of the stack. When a command wants to go to the previous
  // panel all it needs to do is pop the top panel.
  private final Stack<Panel> panels = new Stack<>();

  public Chat(Context context) {
    this.panels.push(createRootPanel(context));
  }

  // HANDLE COMMAND
  //
  // Take a single line of input and parse a command from it. If the system
  // is willing to take another command, the function will return true. If
  // the system wants to exit, the function will return false.
  //
  public boolean handleCommand(String line) {
    final List<String> args = new ArrayList<>();
    final Tokenizer tokenizer = new Tokenizer(line);
    try {
      for (String token = tokenizer.next(); token != null; token = tokenizer.next()) {
        args.add(token);
      }
    } catch (IOException e) {
      System.out.println("Error: " + e);
    }
    final String command = args.get(0);
    args.remove(0);


    // Because "exit" and "back" are applicable to every panel, handle
    // those commands here to avoid having to implement them for each
    // panel.

    if ("exit".equals(command)) {
      // The user does not want to process any more commands
      return false;
    }

    // Do not allow the root panel to be removed.
    if ("back".equals(command) && panels.size() > 1) {
      panels.pop();
      return true;
    }

    if (panels.peek().handleCommand(command, args)) {
      // the command was handled
      return true;
    }

    // If we get to here it means that the command was not correctly handled
    // so we should let the user know. Still return true as we want to continue
    // processing future commands.
    System.out.println("ERROR: Unsupported command");
    return true;
  }

  // CREATE ROOT PANEL
  //
  // Create a panel for the root of the application. Root in this context means
  // the first panel and the only panel that should always be at the bottom of
  // the panels stack.
  //
  // The root panel is for commands that require no specific contextual information.
  // This is before a user has signed in. Most commands handled by the root panel
  // will be user selection focused.
  //
  private Panel createRootPanel(final Context context) {

    final Panel panel = new Panel();

    // HELP
    //
    // Add a command to print a list of all commands and their description when
    // the user for "help" while on the root panel.
    //
    panel.register("help", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("ROOT MODE");
        System.out.println("  u-list");
        System.out.println("    List all users.");
        System.out.println("  u-add <name>");
        System.out.println("    Add a new user with the given name.");
        System.out.println("  u-sign-in <name>");
        System.out.println("    Sign in as the user with the given name.");
        System.out.println("  version");
        System.out.println("    Display the version of the server.");
        System.out.println("  uptime");
        System.out.println("    Display the amount of time the server has been running.");
        System.out.println("  exit");
        System.out.println("    Exit the program.");
      }
    });

    //UPTIME
    //
    // Add a command to get the server uptime when the user enters "uptime"
    // while on the root panel.
    //
    panel.register("uptime", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final ServerInfo info = context.getServerUptime();
        if (info == null) {
          System.out.println("ERROR: unable to retrieve server uptime");
        } else {
          long uptimeInMs = Time.now().inMs() - info.startTime.inMs();
          long second = (uptimeInMs / 1000) % 60;
          long minute = (uptimeInMs / (1000 * 60)) % 60;
          long hour = (uptimeInMs / (1000 * 60 * 60)) % 24;
          String formattedTime = String.format("%02d:%02d:%02d", hour, minute, second);
          System.out.println(formattedTime);
        }
      }
    });

    // U-LIST (user list)
    //
    // Add a command to print all users registered on the server when the user
    // enters "u-list" while on the root panel.
    //
    panel.register("u-list", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for (final UserContext user : context.allUsers()) {
          System.out.format(
              "USER %s (UUID:%s)\n",
              user.user.name,
              user.user.id);
        }
      }
    });

    // U-ADD (add user)
    //
    // Add a command to add and sign-in as a new user when the user enters
    // "u-add" while on the root panel.
    //
    panel.register("u-add", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.size() > 0 ? args.get(0) : "";
        if (name.length() > 0) {
          if (context.create(name) == null) {
            System.out.println("ERROR: Failed to create new user");
          }
        } else {
          System.out.println("ERROR: Missing <username>");
        }
      }
    });

    // U-SIGN-IN (sign in user)
    //
    // Add a command to sign-in as a user when the user enters "u-sign-in"
    // while on the root panel.
    //
    panel.register("u-sign-in", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        if (args.size() > 0) {
          final UserContext user = findUser(args.get(0));
          if (user == null) {
            System.out.format("ERROR: Failed to sign in as '%s'\n", args.get(0));
          } else {
            panels.push(createUserPanel(user));
          }
        } else {
          System.out.println("ERROR: Missing <username>");
        }
      }

      // Find the first user with the given name and return a user context
      // for that user. If no user is found, the function will return null.
      private UserContext findUser(String name) {
        for (final UserContext user : context.allUsers()) {
          if (user.user.name.equals(name)) {
            return user;
          }
        }
        return null;
      }
    });

    // Version Check
    //
    // adds a new command "version" and will display the version it is in
    //
    panel.register("version", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final ServerInfo info = context.getVersion();
        if (info == null) {
          System.out.println("ERROR, server did not send valid info");
        } else {
          System.out.println(info.version);
        }
      }
    });

    // Now that the panel has all its commands registered, return the panel
    // so that it can be used.
    return panel;
  }

  private Panel createUserPanel(final UserContext user) {

    final Panel panel = new Panel();

    // HELP
    //
    // Add a command that will print a list of all commands and their
    // descriptions when the user enters "help" while on the user panel.
    //
    panel.register("help", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("USER MODE");
        System.out.println("  c-list");
        System.out.println("    List all conversations that the current user can interact with.");
        System.out.println("  c-add <title>");
        System.out.println("    Add a new conversation with the given title and join it as the current user.");
        System.out.println("  i-add <id>");
        System.out.println("    Add a new interest with the given id ");
        System.out.println("  c-join <title>");
        System.out.println("    Join the conversation as the current user.");
        System.out.println("  info");
        System.out.println("    Display all info for the current user");
        System.out.println("  back");
        System.out.println("    Go back to ROOT MODE.");
        System.out.println("  exit");
        System.out.println("    Exit the program.");
      }
    });

    // C-LIST (list conversations)
    //
    // Add a command that will print all conversations when the user enters
    // "c-list" while on the user panel.
    //
    panel.register("c-list", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        for (final ConversationContext conversation : user.conversations()) {
          System.out.format(
              "CONVERSATION %s (UUID:%s)\n",
              conversation.conversation.title,
              conversation.conversation.id);
        }
      }
    });

    // C-ADD (add conversation)
    //
    // Add a command that will create and join a new conversation when the user
    // enters "c-add" while on the user panel.
    //
    panel.register("c-add", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.size() > 0 ? args.get(0) : "";
        if (name.length() > 0) {
          final ConversationContext conversation = user.start(name);
          if (conversation == null) {
            System.out.println("ERROR: Failed to create new conversation");
          } else {
            panels.push(createConversationPanel(conversation));
            user.createConversation(conversation.conversation.id);
            conversation.setDefaultAccessLevel(AccessLevel.NONE);
          }
        } else {
          System.out.println("ERROR: Missing <title>");
        }
      }
    });

    // I-ADD (add interests)
    //
    // Add a command that will add a new interest when the user
    // enters "i-add" while on the user panel.
    //
    panel.register("i-add", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        try {
          final Uuid id = args.size() > 0 ? Uuid.parse(args.get(0)) : Uuid.NULL;
          if (args.size() > 0) {
            if (id == null) {
              System.out.println("ERROR: Failed to add new interest");
            } else {
              user.addInterest(id);
            }
          } else {
            System.out.println("ERROR: Missing <title>");
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    // I-REMOVE (remove interests)
    //
    // Add a command that will add a new interest when the user
    // enters "i-add" while on the user panel.
    //
    panel.register("i-remove", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        try {
          final Uuid id = args.size() > 0 ? Uuid.parse(args.get(0)) : Uuid.NULL;
          if (args.size() > 0) {
            if (id == null) {
              System.out.println("ERROR: Failed to remove new interest");
            } else {
              user.removeInterest(id);
            }
          } else {
            System.out.println("ERROR: Missing <title>");
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
    // C-JOIN (join conversation)
    //
    // Add a command that will joing a conversation when the user enters
    // "c-join" while on the user panel.
    //
    panel.register("c-join", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String name = args.size() > 0 ? args.get(0) : "";
        if (name.length() > 0) {
          final ConversationContext conversation = find(name);
          if (conversation == null) {
            System.out.format("ERROR: No conversation with name '%s'\n", name);
          } else {
            panels.push(createConversationPanel(conversation));
            user.joinConversation(conversation.conversation.id);
          }
        } else {
          System.out.println("ERROR: Missing <title>");
        }
      }

      // Find the first conversation with the given name and return its context.
      // If no conversation has the given name, this will return null.
      private ConversationContext find(String title) {
        for (final ConversationContext conversation : user.conversations()) {
          if (title.equals(conversation.conversation.title)) {
            return conversation;
          }
        }
        return null;
      }
    });

    // STATUS UPDATE (status update)
    //
    // Add a command that will print all of the updated things that the
    // user is interested in.
    //
    panel.register("status-update", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        HashMap<Uuid, Collection<ConversationHeader>> usersOfInterest = new HashMap<Uuid, Collection<ConversationHeader>>();
        HashMap<Uuid, Integer> conversationsOfInterest = new HashMap<Uuid, Integer>();
        user.getStatusUpdate(usersOfInterest, conversationsOfInterest);
        for (final Map.Entry<Uuid, Collection<ConversationHeader>> entry : usersOfInterest.entrySet()) {
          Uuid userid = entry.getKey();
          Collection<ConversationHeader> conversations = entry.getValue();
          System.out.format(
              "User of interest's uuid:%s\n\tNew/Updated Conversations:\n",
              userid);
          for (ConversationHeader conversation : conversations) {
            System.out.format(
              "\t\tCONVERSATION %s (UUID:%s)\n",
              conversation.title,
              conversation.id);
          }
        }
        for (final Map.Entry<Uuid, Integer> entry : conversationsOfInterest.entrySet()) {
          Uuid conversationid = entry.getKey();
          Integer newMessages = entry.getValue();
          System.out.format(
              "Conversation of interest's uuid:%s\n\tNumber of new messages:%s\n",
              conversationid,
              newMessages);
        }
      }
    });

    // INFO
    //
    // Add a command that will print info about the current context when the
    // user enters "info" while on the user panel.
    //
    panel.register("info", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("User Info:");
        System.out.format("  Name : %s\n", user.user.name);
        System.out.format("  Id   : UUID:%s\n", user.user.id);
      }
    });

    // Now that the panel has all its commands registered, return the panel
    // so that it can be used.
    return panel;
  }

  private Panel createConversationPanel(final ConversationContext conversation) {

    final Panel panel = new Panel();

    // HELP
    //
    // Add a command that will print all the commands and their descriptions
    // when the user enters "help" while on the conversation panel.
    //
    panel.register("help", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("USER MODE");
        System.out.println("  m-list");
        System.out.println("    List all messages in the current conversation.");
        System.out.println("  m-add <message>");
        System.out.println("    Add a new message to the current conversation as the current user.");
        System.out.println("  set-default <access level>");
        System.out.println("    Set a new default access level for new users (creator only)");
        System.out.println("  n-make <userid>");
        System.out.println("    Make the user have no access level in the conversation.");
        System.out.println("  mem-make <userid>");
        System.out.println("    Make the user a member.");
        System.out.println("  o-make <userid>");
        System.out.println("    Make the user an owner.");
        System.out.println("  info");
        System.out.println("    Display all info about the current conversation.");
        System.out.println("  back");
        System.out.println("    Go back to USER MODE.");
        System.out.println("  exit");
        System.out.println("    Exit the program.");
      }
    });

    // M-LIST (list messages)
    //
    // Add a command to print all messages in the current conversation when the
    // user enters "m-list" while on the conversation panel.
    //
    panel.register("m-list", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final AccessLevel accesslevel = conversation.getUserAccessLevel().getAccessLevel();
        if (accesslevel == AccessLevel.NONE ) {
          System.out.println("ERROR: You do not have the valid access level to view Messages");
        } else {
          System.out.println("--- start of conversation ---");
          for (MessageContext message = conversation.firstMessage();
               message != null;
               message = message.next()) {
            System.out.println();
            System.out.format("USER : %s\n", message.message.author);
            System.out.format("SENT : %s\n", message.message.creation);
            System.out.println();
            System.out.println(message.message.content);
            System.out.println();
          }
          System.out.println("---  end of conversation  ---");
        }
      }
    });

    // M-ADD (add message)
    //
    // Add a command to add a new message to the current conversation when the
    // user enters "m-add" while on the conversation panel.
    //
    panel.register("m-add", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final AccessLevel accesslevel = conversation.getUserAccessLevel().getAccessLevel();
        if (accesslevel == AccessLevel.NONE ) {
          System.out.println("ERROR: You do not have the valid access level to add Messages");
        } else {
          final String message = args.size() > 0 ? args.get(0) : "";
          if (message.length() > 0) {
            conversation.add(message);
          } else {
            System.out.println("ERROR: Messages must contain text");
          }
        }
      }
    });

    panel.register("set-default", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        final String accessLevel = args.size() > 0 ? args.get(0) : "";
        if (accessLevel.length() > 0 && conversation.user.id.equals(conversation.conversation.owner)) {
          AccessLevel defaultAccessLevel = AccessLevel.valueOf(accessLevel);
          conversation.setDefaultAccessLevel(defaultAccessLevel);
        } else {
          System.out.println("ERROR: Must set a valid access level, or you do not have the necessary permissions");
        }
      }
    });

    // N-MAKE (make user none)
    //
    // Add a command to make a user have the access level of none
    //
    panel.register("n-make", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        try {
          final Uuid id = args.size() > 0 ? Uuid.parse(args.get(0)) : Uuid.NULL;
          final AccessLevel accesslevel = conversation.getUserAccessLevel().getAccessLevel();
          if (args.size() > 0 && (accesslevel == AccessLevel.OWNER || accesslevel == AccessLevel.CREATOR)) {
            if (id == null) {
              System.out.println("ERROR: Failed to make user have no access level");
            } else {
              conversation.addUserAccessLevel(id, AccessLevel.NONE);
            }
          } else {
            System.out.println("ERROR: Missing <userid> or you don't have the access level required");
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    // MEM-MAKE (make user member)
    //
    // Add a command to make a user have the access level of member
    //
    panel.register("mem-make", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        try {
          final Uuid id = args.size() > 0 ? Uuid.parse(args.get(0)) : Uuid.NULL;
          final AccessLevel accesslevel = conversation.getUserAccessLevel().getAccessLevel();
          if (args.size() > 0 && (accesslevel == AccessLevel.OWNER || accesslevel == AccessLevel.CREATOR)) {
            if (id == null) {
              System.out.println("ERROR: Failed to make user a member");
            } else {
              conversation.addUserAccessLevel(id, AccessLevel.MEMBER);
            }
          } else {
            System.out.println("ERROR: Missing <userid> or missing necessary permissions");
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    // O-MAKE (make user owner)
    //
    // Add a command to make a user have the access level of owner
    //
    panel.register("o-make", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        try {
          final Uuid id = args.size() > 0 ? Uuid.parse(args.get(0)) : Uuid.NULL;
          final AccessLevel accesslevel = conversation.getUserAccessLevel().getAccessLevel();
          if (args.size() > 0 && (accesslevel == AccessLevel.OWNER || accesslevel == AccessLevel.CREATOR)) {
            if (id == null) {
              System.out.println("ERROR: Failed to make user an owner");
            } else {
              conversation.addUserAccessLevel(id, AccessLevel.OWNER);
            }
          } else {
            System.out.println("ERROR: Missing <userid>");
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    // INFO
    //
    // Add a command to print info about the current conversation when the user
    // enters "info" while on the conversation panel.
    //
    panel.register("info", new Panel.Command() {
      @Override
      public void invoke(List<String> args) {
        System.out.println("Conversation Info:");
        System.out.format("  Title : %s\n", conversation.conversation.title);
        System.out.format("  Id    : UUID:%s\n", conversation.conversation.id);
        System.out.format("  Creator  : %s\n", conversation.conversation.owner);
        System.out.format("  Access Level: %s\n", conversation.getUserAccessLevel().getAccessLevel());
      }
    });

    // Now that the panel has all its commands registered, return the panel
    // so that it can be used.
    return panel;
  }
}
