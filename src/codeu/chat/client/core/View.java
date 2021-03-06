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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import codeu.chat.common.*;
import codeu.chat.util.Logger;
import codeu.chat.util.Serializers;
import codeu.chat.util.Time;
import codeu.chat.util.Uuid;
import codeu.chat.util.connections.Connection;
import codeu.chat.util.connections.ConnectionSource;

// VIEW
//
// This is the view component of the Model-View-Controller pattern used by the
// the client to reterive readonly data from the server. All methods are blocking
// calls.
final class View implements BasicView {

  private final static Logger.Log LOG = Logger.newLog(View.class);

  private final ConnectionSource source;

  public View(ConnectionSource source) {
    this.source = source;
  }

  @Override
  public Collection<User> getUsers() {

    final Collection<User> users = new ArrayList<>();

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.GET_USERS_REQUEST);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.GET_USERS_RESPONSE) {
        users.addAll(Serializers.collection(User.SERIALIZER).read(connection.in()));
      } else {
        LOG.error("Response from server failed.");
      }

    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return users;
  }

  @Override
  public Collection<ConversationHeader> getConversations() {

    final Collection<ConversationHeader> summaries = new ArrayList<>();

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.GET_ALL_CONVERSATIONS_REQUEST);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.GET_ALL_CONVERSATIONS_RESPONSE) {
        summaries.addAll(Serializers.collection(ConversationHeader.SERIALIZER).read(connection.in()));
      } else {
        LOG.error("Response from server failed.");
      }

    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return summaries;
  }

  @Override
  public Collection<ConversationPayload> getConversationPayloads(Collection<Uuid> ids) {

    final Collection<ConversationPayload> conversations = new ArrayList<>();

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.GET_CONVERSATIONS_BY_ID_REQUEST);
      Serializers.collection(Uuid.SERIALIZER).write(connection.out(), ids);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.GET_CONVERSATIONS_BY_ID_RESPONSE) {
        conversations.addAll(Serializers.collection(ConversationPayload.SERIALIZER).read(connection.in()));
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return conversations;
  }

  @Override
  public Collection<Interests> getInterests() {

    final Collection<Interests> interests = new ArrayList<>();

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.GET_INTERESTS_REQUEST);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.GET_INTERESTS_RESPONSE) {
        interests.addAll(Serializers.collection(Interests.SERIALIZER).read(connection.in()));
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return interests;
  }

  @Override
  public Collection<UserAccessLevel> getAccessLevels(Uuid conversation) {

    final Collection<UserAccessLevel> accessLevels = new ArrayList<>();

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.GET_ALL_ACCESS_LEVELS_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), conversation);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.GET_ALL_ACCESS_LEVELS_RESPONSE) {
        accessLevels.addAll(Serializers.collection(UserAccessLevel.SERIALIZER).read(connection.in()));
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return accessLevels;
  }

  @Override
  public AccessLevel getDefaultAccessLevel(Uuid conversationId) {
    AccessLevel response = null;

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.GET_DEFAULT_ACCESS_LEVEL_REQUEST);
      Uuid.SERIALIZER.write(connection.out(), conversationId);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.GET_DEFAULT_ACCESS_LEVEL_RESPONSE) {
        response = AccessLevel.valueOf(Serializers.STRING.read(connection.in()));
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return response;
  }

  @Override
  public Collection<Message> getMessages(Collection<Uuid> ids) {

    final Collection<Message> messages = new ArrayList<>();

    try (final Connection connection = source.connect()) {

      Serializers.INTEGER.write(connection.out(), NetworkCode.GET_MESSAGES_BY_ID_REQUEST);
      Serializers.collection(Uuid.SERIALIZER).write(connection.out(), ids);

      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.GET_MESSAGES_BY_ID_RESPONSE) {
        messages.addAll(Serializers.collection(Message.SERIALIZER).read(connection.in()));
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }

    return messages;
  }

  public void getStatusUpdate(Uuid userid, HashMap<Uuid, Collection<ConversationHeader>> interestedUsers,
   HashMap<Uuid, Integer> interestedConversations) {

    try (final Connection connection = source.connect()) {
      Serializers.INTEGER.write(connection.out(), NetworkCode.STATUS_UPDATE_REQUEST);
      System.out.println(userid.toString());
      Uuid.SERIALIZER.write(connection.out(), userid);
      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.STATUS_UPDATE_RESPONSE) {
        Collection<Uuid> keys = Serializers.collection(Uuid.SERIALIZER).read(connection.in());
        Collection<Collection<ConversationHeader>> values = Serializers.collection(Serializers.collection(ConversationHeader.SERIALIZER)).read(connection.in());
        Iterator<Uuid> i1 = keys.iterator();
        Iterator<Collection<ConversationHeader>> i2 = values.iterator();
        while (i1.hasNext() && i2.hasNext()) {
          interestedUsers.put(i1.next(), i2.next());
        }

        keys = Serializers.collection(Uuid.SERIALIZER).read(connection.in());
        Collection<Integer> values2 = Serializers.collection(Serializers.INTEGER).read(connection.in());
        i1 = keys.iterator();
        Iterator<Integer> i3 = values2.iterator();
        while (i1.hasNext() && i3.hasNext()) {
          interestedConversations.put(i1.next(), i3.next());
        }
      } else {
        LOG.error("Response from server failed.");
      }
    } catch (Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex, "Exception during call on server.");
    }
  }

  //get the info object from the server 
  public ServerInfo getVersion() {

    try (final Connection connection = this.source.connect()) {

      Serializers.INTEGER.write(connection.out(),NetworkCode.SERVER_VERSION_REQUEST);
      
      if (Serializers.INTEGER.read(connection.in())== NetworkCode.SERVER_VERSION_RESPONSE) {
        final Uuid version = Uuid.SERIALIZER.read(connection.in());
        return new ServerInfo(version);
      } else {
        LOG.error("ERROR, server did not respond with proper response");
        System.out.println("ERROR, server did not respons with proper response");
      }
    } catch (Exception ex) {
      LOG.error(ex,"ERROR, something went wrong with the connection");
      System.out.println("ERROR, something went wrong with the connection");
    }

    return null;
  } 

  public ServerInfo getServerUptime() {
    
    try (final Connection connection = source.connect()) {
      
      Serializers.INTEGER.write(connection.out(),NetworkCode.SERVER_UPTIME_REQUEST);
      
      if (Serializers.INTEGER.read(connection.in()) == NetworkCode.SERVER_UPTIME_RESPONSE) {
        final Time startTime = Time.SERIALIZER.read(connection.in());
        return new ServerInfo(startTime);
      } else {
        LOG.error("Response from server failed.");
      }
    } catch(Exception ex) {
      System.out.println("ERROR: Exception during call on server. Check log for details.");
      LOG.error(ex,"Exception during call on server");
    }
    return null;
  }

}
