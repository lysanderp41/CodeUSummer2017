package codeu.chat.common;

import codeu.chat.util.Uuid;


/**
 * Created by Lysander on 7/25/17.
 */
public class UserAccessLevel {
    private Uuid user;
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
