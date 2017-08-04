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

package codeu.chat.client.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread;

import codeu.chat.common.AccessLevel;
import codeu.chat.common.BasicController;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.Interests;
import codeu.chat.common.Message;
import codeu.chat.common.NetworkCode;
import codeu.chat.common.User;
import codeu.chat.common.UserAccessLevel;
import codeu.chat.util.*;
import codeu.chat.util.connections.Connection;
import codeu.chat.util.connections.ConnectionSource;

final class Controller implements BasicController {

  private final static Logger.Log LOG = Logger.newLog(Controller.class);

  private final ConnectionSource source;

  public Controller(ConnectionSource source) {
    this.source = source;
  }

  @Override
  public Message newMessage(Uuid author, Uuid conversation, String body) {

    Message response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_MESSAGE_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), author);
      Uuid.SERIALIZER.write(connection.out(), conversation);
      Serializers.STRING.write(connection.out(), body);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_MESSAGE_RESPONSE) {
        response = Serializers.nullable(Message.SERIALIZER).read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  @Override
  public User newUser(String name) {

    User response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_USER_REQUEST);
      Serializers.STRING.write(connection.out(), name);
      LOG.info("newUser: Request completed.");

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_USER_RESPONSE) {
        response = Serializers.nullable(User.SERIALIZER).read(connection.in());
        LOG.info("newUser: Response completed.");
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  @Override
  public ConversationHeader newConversation(String title, Uuid owner, AccessLevel defaultAccessLevel)  {

    ConversationHeader response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_CONVERSATION_REQUEST);
      Serializers.STRING.write(connection.out(), title);
      Uuid.SERIALIZER.write(connection.out(), owner);
      Serializers.STRING.write(connection.out(), defaultAccessLevel.toString());

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_CONVERSATION_RESPONSE) {
        response = Serializers.nullable(ConversationHeader.SERIALIZER).read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  public ConversationHeader newConversation(String title, Uuid owner)  {

    ConversationHeader response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_CONVERSATION_REQUEST);
      Serializers.STRING.write(connection.out(), title);
      Uuid.SERIALIZER.write(connection.out(), owner);
      Serializers.STRING.write(connection.out(), AccessLevel.NONE.toString());

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_CONVERSATION_RESPONSE) {
        response = Serializers.nullable(ConversationHeader.SERIALIZER).read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  @Override
  public UserAccessLevel newUserAccessLevel(Uuid conversationId, Uuid userId, AccessLevel accessLevel) {

      UserAccessLevel response = null;

      try (final Connection connection = source.connect()) {
        Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_ACCESS_LEVEL_REQUEST);
        Uuid.SERIALIZER.write(connection.out(), conversationId);
        Uuid.SERIALIZER.write(connection.out(), userId);
        Serializers.STRING.write(connection.out(), accessLevel.toString());

        if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_ACCESS_LEVEL_RESPONSE) {
          response = Serializers.nullable(UserAccessLevel.SERIALIZER).read(connection.in());
        } else {
          LOG.error("Response from server failed.");
        }
      } catch (Exception ex) {
        System.out.println("ERROR: Exception during call on server. Check log for details.");
        LOG.error(ex, "Exception during call on server.");
      }
      return response;
  }

  @Override
  public UserAccessLevel getUserAccessLevel(Uuid conversationId, Uuid userId) {

      UserAccessLevel response = null;

      try (final Connection connection = source.connect()) {
        Serializers.INTEGER.write(connection.out(), NetworkCode.GET_ACCESS_LEVEL_REQUEST);
        Uuid.SERIALIZER.write(connection.out(), conversationId);
        Uuid.SERIALIZER.write(connection.out(), userId);

        if (Serializers.INTEGER.read(connection.in()) == NetworkCode.GET_ACCESS_LEVEL_RESPONSE) {
          response = Serializers.nullable(UserAccessLevel.SERIALIZER).read(connection.in());
        } else {
          LOG.error("Response from server failed.");
        }
      } catch (Exception ex) {
        System.out.println("ERROR: Exception during call on server. Check log for details.");
        LOG.error(ex, "Exception during call on server.");
      }
      return response;
  }

  @Override
  public Interests newInterest(Uuid userid, Uuid interests) {

    Interests response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.NEW_INTERESTS_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), userid);
      Uuid.SERIALIZER.write(connection.out(), interests);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.NEW_INTERESTS_RESPONSE) {
        response = Serializers.nullable(Interests.SERIALIZER).read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  public Interests removeInterest(Uuid userid, Uuid interests) {

    Interests response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.REMOVE_INTERESTS_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), userid);
      Uuid.SERIALIZER.write(connection.out(), interests);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.REMOVE_INTERESTS_RESPONSE) {
        response = Serializers.nullable(Interests.SERIALIZER).read(connection.in());
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }
}
