package codeu.chat.common;

import codeu.chat.util.*;

import java.io.*;


/**
 * Created by Lysander on 7/25/17.
 */
public class UserAccessLevel {

    public static final Serializer<UserAccessLevel> SERIALIZER = new Serializer<UserAccessLevel>() {
        @Override
        public void write(OutputStream out, UserAccessLevel value) throws IOException {
            Uuid.SERIALIZER.write(out, value.user);
            Serializers.STRING.write(out, value.accessLevel.toString());
        }

        @Override
        public UserAccessLevel read(InputStream in) throws IOException {
            return new UserAccessLevel(
                    Uuid.SERIALIZER.read(in),
                    AccessLevel.valueOf(Serializers.STRING.read(in))
            );
        }
    };

    private final Uuid user;
    private AccessLevel accessLevel;

    public UserAccessLevel(Uuid user, AccessLevel accessLevel) {
        this.user = user;
        this.accessLevel = accessLevel;
    }

    public Uuid getUser() {
        return user;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }
}
