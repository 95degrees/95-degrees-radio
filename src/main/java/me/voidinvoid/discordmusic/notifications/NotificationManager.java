package me.voidinvoid.discordmusic.notifications;

import com.mongodb.client.model.Filters;
import me.voidinvoid.discordmusic.DatabaseManager;
import me.voidinvoid.discordmusic.RadioService;
import me.voidinvoid.discordmusic.utils.Service;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

public class NotificationManager implements RadioService {

    public Map<Notification, Boolean> getAll(User user) {
        var notifs = getNotificationsDocument(user);
        var map = new HashMap<Notification, Boolean>();

        if (notifs == null || notifs.isEmpty()) return map;

        for (var entry : notifs.entrySet()) {
            map.put(Notification.valueOf(entry.getKey()), (boolean) entry.getValue());
        }

        return map;
    }

    public Map<Notification, Boolean> getAll(Member member) {
        return getAll(member.getUser());
    }

    public boolean isEnabled(User user, Notification notification) {
        return getAll(user).getOrDefault(notification, notification.getDefaultValue());
    }

    public boolean isEnabled(Member member, Notification notification) {
        return isEnabled(member.getUser(), notification);
    }

    public void setEnabled(Member member, Notification notification, boolean enabled) {
        Service.of(DatabaseManager.class).getClient().getDatabase("95degrees").getCollection("users").updateOne(Filters.eq(member.getUser().getId()), new Document("$set", new Document("notifications." + notification.name(), enabled)));
    }

    private Document getNotificationsDocument(User user) {
        var doc = Service.of(DatabaseManager.class).getClient().getDatabase("95degrees").getCollection("users").find(Filters.eq(user.getId())).first();
        if (doc == null || !doc.containsKey("notifications")) return null;

        return doc.get("notifications", Document.class);
    }
}
