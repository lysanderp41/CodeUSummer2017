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

package codeu.chat.common;

public final class NetworkCode {

  public static final int
      NO_MESSAGE = 0,
      GET_USERS_REQUEST = 1,
      GET_USERS_RESPONSE = 2,
      GET_ALL_CONVERSATIONS_REQUEST = 3,
      GET_ALL_CONVERSATIONS_RESPONSE = 4,
      GET_CONVERSATIONS_BY_ID_RESPONSE = 5,
      GET_CONVERSATIONS_BY_ID_REQUEST = 6,
      GET_MESSAGES_BY_ID_REQUEST = 7,
      GET_MESSAGES_BY_ID_RESPONSE = 8,
      NEW_MESSAGE_REQUEST = 9,
      NEW_MESSAGE_RESPONSE = 10,
      NEW_USER_REQUEST = 11,
      NEW_USER_RESPONSE = 12,
      NEW_CONVERSATION_REQUEST = 13,
      NEW_CONVERSATION_RESPONSE = 14,
      NEW_INTERESTS_REQUEST = 15,
      NEW_INTERESTS_RESPONSE = 16,
      GET_INTERESTS_REQUEST = 17,
      GET_INTERESTS_RESPONSE = 18,
      GET_INTERESTS_BY_USERID_REQUEST = 19,
      GET_INTERESTS_BY_USERID_RESPONSE = 20,
      STATUS_UPDATE_REQUEST = 21,
      STATUS_UPDATE_RESPONSE = 22,
      RELAY_READ_REQUEST = 27,
      RELAY_READ_RESPONSE = 28,
      RELAY_WRITE_REQUEST = 29,
      RELAY_WRITE_RESPONSE = 30,
      SERVER_UPTIME_REQUEST = 31,
      SERVER_UPTIME_RESPONSE = 32,
      SERVER_VERSION_REQUEST = 33,
      SERVER_VERSION_RESPONSE = 34,
      REMOVE_INTERESTS_REQUEST = 35,
      REMOVE_INTERESTS_RESPONSE = 36,
      NEW_ACCESS_LEVEL_REQUEST = 37,
      NEW_ACCESS_LEVEL_RESPONSE = 38,
      GET_ALL_ACCESS_LEVELS_REQUEST = 39,
      GET_ALL_ACCESS_LEVELS_RESPONSE = 40,
      GET_ACCESS_LEVEL_REQUEST = 41,
      GET_ACCESS_LEVEL_RESPONSE = 42,
      SET_DEFAULT_ACCESS_LEVEL_REQUEST = 43,
      SET_DEFAULT_ACCESS_LEVEL_RESPONSE = 44,
      GET_DEFAULT_ACCESS_LEVEL_REQUEST = 45,
      GET_DEFAULT_ACCESS_LEVEL_RESPONSE = 46;
}
