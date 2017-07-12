package codeu.chat.client.core;

import java.util.Collection;

import codeu.chat.common.BasicController;
import codeu.chat.common.BasicView;
import codeu.chat.common.ConversationHeader;
import codeu.chat.common.Interests;
import codeu.chat.util.Uuid;


public final class InterestsContext {
    public final Interests interests;
    private final BasicView view;
    private final BasicController controller;

    public InterestsContext(Interests interests, BasicView view, BasicController controller) {
        this.interests = interests;
        this.view = view;
        this.controller = controller;
    }
    public InterestsContext (Interests interests){
        this.interests = interests;
        this.view = null;
        this.controller = null;
    }
}